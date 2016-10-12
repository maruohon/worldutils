package fi.dy.masa.worldtools.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldtools.WorldTools;

public class EntityReader
{
    private static final EntityReader INSTANCE = new EntityReader();
    private List<EntityData> entities = new ArrayList<EntityData>();
    private final FilenameFilter anvilRegionFileFilter = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.startsWith("r.") && name.endsWith(".mca");
        }
    };

    private EntityReader()
    {
    }

    public static EntityReader instance()
    {
        return INSTANCE;
    }

    public List<EntityData> getEntities()
    {
        return this.entities;
    }

    private static File getWorldSaveLocation(int dimension)
    {
        World world = DimensionManager.getWorld(dimension);
        File dir = DimensionManager.getCurrentSaveRootDirectory();

        if (world != null && world.provider.getSaveFolder() != null)
        {
            dir = new File(dir, world.provider.getSaveFolder());
        }

        return dir;
    }

    public String readEntities(int dimension)
    {
        this.entities.clear();
        String chatOutput = "";

        File worldSaveLocation = getWorldSaveLocation(dimension);
        File regionDir = new File(worldSaveLocation, "region");
        int regionCount = 0;
        int totalEntityCount = 0;

        if (regionDir.exists() && regionDir.isDirectory())
        {
            for (File regionFile : regionDir.listFiles(this.anvilRegionFileFilter))
            {
                totalEntityCount += this.readEntitiesFromRegion(regionFile);
                regionCount++;
            }
        }

        if (totalEntityCount > 0)
        {
            chatOutput = String.format("Read a total of %d entities from %d region files", totalEntityCount, regionCount);
            WorldTools.logger.info(chatOutput);
        }

        World world = DimensionManager.getWorld(dimension);
        if (world != null && world.getChunkProvider() instanceof ChunkProviderServer)
        {
            WorldTools.logger.warn("There were {} chunks currently loaded, the entity list will be inaccurate for those chunks!",
                    ((ChunkProviderServer) world.getChunkProvider()).getLoadedChunkCount());
        }

        return chatOutput;
    }

    private int readEntitiesFromRegion(File regionFile)
    {
        RegionFile region = new RegionFile(regionFile);
        int entityCount = 0;

        for (int cz = 0; cz < 32; cz++)
        {
            for (int cx = 0; cx < 32; cx++)
            {
                if (region.isChunkSaved(cx, cz))
                {
                    entityCount += this.readEntitiesFromChunk(region, cx, cz, regionFile.getName());
                }
            }
        }

        return entityCount;
    }

    private int readEntitiesFromChunk(RegionFile region, int chunkX, int chunkZ, String regionName)
    {
        int entityCount = 0;
        DataInputStream data = region.getChunkDataInputStream(chunkX, chunkZ);

        if (data == null)
        {
            WorldTools.logger.warn("Failed to read chunk data for chunk ({}, {}) from file '{}'", chunkX, chunkZ, regionName);
            return 0;
        }

        try
        {
            NBTTagCompound nbt = CompressedStreamTools.read(data);
            data.close();
            NBTTagCompound level = nbt.getCompoundTag("Level");

            if (level.hasKey("Entities", Constants.NBT.TAG_LIST))
            {
                ChunkPos chunkPos = new ChunkPos(level.getInteger("xPos"), level.getInteger("zPos"));
                NBTTagList list = level.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

                for (int i = 0; i < list.tagCount(); i++)
                {
                    NBTTagCompound entity = list.getCompoundTagAt(i);
                    NBTTagList posList = entity.getTagList("Pos", Constants.NBT.TAG_DOUBLE);
                    int dim = entity.getInteger("Dimension");
                    Vec3d pos = new Vec3d(posList.getDoubleAt(0), posList.getDoubleAt(1), posList.getDoubleAt(2));
                    UUID uuid = new UUID(entity.getLong("UUIDMost"), entity.getLong("UUIDLeast"));

                    this.entities.add(new EntityData(dim, entity.getString("id"), pos, chunkPos, uuid));
                    entityCount++;
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return entityCount;
    }

    private Map<ChunkPos, List<EntityData>> getMapForChunks(Map<ChunkPos, Map<ChunkPos, List<EntityData>>> mapRegions, ChunkPos regionPos)
    {
        Map<ChunkPos, List<EntityData>> mapChunks = mapRegions.get(regionPos);

        if (mapChunks == null)
        {
            mapChunks = new HashMap<ChunkPos, List<EntityData>>();
            mapRegions.put(regionPos, mapChunks);
        }

        return mapChunks;
    }

    private List<EntityData> getListForEntitiesInChunk(Map<ChunkPos, List<EntityData>> mapChunks, ChunkPos chunkPos)
    {
        List<EntityData> list = mapChunks.get(chunkPos);

        if (list == null)
        {
            list = new ArrayList<EntityData>();
            mapChunks.put(chunkPos, list);
        }

        return list;
    }

    private Map<ChunkPos, Map<ChunkPos, List<EntityData>>> sortEntitiesByRegionAndChunk(List<EntityData> listIn)
    {
        Map<ChunkPos, Map<ChunkPos, List<EntityData>>> entitiesByRegion = new HashMap <ChunkPos, Map<ChunkPos, List<EntityData>>>();

        for (EntityData entry : listIn)
        {
            ChunkPos regionPos = new ChunkPos(entry.chunkPos.chunkXPos >> 5, entry.chunkPos.chunkZPos >> 5);
            this.getListForEntitiesInChunk(this.getMapForChunks(entitiesByRegion, regionPos), entry.chunkPos).add(entry);
        }

        return entitiesByRegion;
    }

    private int removeEntitiesFromChunkInRegion(RegionFile region, ChunkPos chunkPos, List<EntityData> toRemove, String regionName, boolean simulate)
    {
        int entityCount = 0;

        if (region.isChunkSaved(chunkPos.chunkXPos, chunkPos.chunkZPos))
        {
            DataInputStream data = region.getChunkDataInputStream(chunkPos.chunkXPos, chunkPos.chunkZPos);

            if (data == null)
            {
                WorldTools.logger.warn("Failed to read chunk data for chunk ({}, {}) from region '{}'", chunkPos.chunkXPos, chunkPos.chunkZPos, regionName);
                return 0;
            }

            try
            {
                NBTTagCompound nbt = CompressedStreamTools.read(data);
                data.close();
                NBTTagCompound level = nbt.getCompoundTag("Level");

                if (level.hasKey("Entities", Constants.NBT.TAG_LIST))
                {
                    NBTTagList list = level.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

                    for (EntityData entry : toRemove)
                    {
                        for (int i = 0; i < list.tagCount(); i++)
                        {
                            NBTTagCompound entity = list.getCompoundTagAt(i);

                            if (entity.getLong("UUIDMost") == entry.uuid.getMostSignificantBits() &&
                                entity.getLong("UUIDLeast") == entry.uuid.getLeastSignificantBits() &&
                                entity.getString("id").equals(entry.id))
                            {
                                if (simulate == false)
                                {
                                    list.removeTag(i);
                                }

                                entityCount++;
                                break;
                            }
                        }
                    }

                    if (simulate == false)
                    {
                        DataOutputStream dataOut = region.getChunkDataOutputStream(chunkPos.chunkXPos, chunkPos.chunkZPos);
                        CompressedStreamTools.write(level, dataOut);
                        dataOut.close();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return entityCount;
    }

    public String removeAllDuplicateEntities(int dimension, boolean simulate)
    {
        File worldSaveLocation = getWorldSaveLocation(dimension);
        File regionDir = new File(worldSaveLocation, "region");
        int removedTotal = 0;

        if (regionDir.exists() && regionDir.isDirectory())
        {
            List<EntityData> dupes = getDuplicateEntriesExcludingFirst(this.entities, true);
            Map<ChunkPos, Map<ChunkPos, List<EntityData>>> entitiesByRegion = this.sortEntitiesByRegionAndChunk(dupes);

            for (Map.Entry<ChunkPos, Map<ChunkPos, List<EntityData>>> regionEntry : entitiesByRegion.entrySet())
            {
                ChunkPos rPos = regionEntry.getKey();
                String regionName = "r." + rPos.chunkXPos + "." + rPos.chunkZPos;
                File regionFile = new File(regionDir, regionName + ".mca");
                RegionFile region = new RegionFile(regionFile);

                for (Map.Entry<ChunkPos, List<EntityData>> chunkEntry : regionEntry.getValue().entrySet())
                {
                    ChunkPos cPos = chunkEntry.getKey();
                    List<EntityData> toRemove = chunkEntry.getValue();
                    int removed = this.removeEntitiesFromChunkInRegion(region, cPos, toRemove, regionName, simulate);
                    removedTotal += removed;

                    WorldTools.logger.info("In region r.{}.{}, chunk {}, {} - removed {} duplicate entities",
                            rPos.chunkXPos, rPos.chunkZPos, cPos.chunkXPos, cPos.chunkZPos, removed);
                }
            }
        }

        return "Removed a total of " + removedTotal + " entities";
    }

    public static List<EntityData> getDuplicateEntriesIncludingFirst(List<EntityData> dataIn, boolean sortFirst)
    {
        List<EntityData> list = new ArrayList<EntityData>();

        if (sortFirst)
        {
            Collections.sort(dataIn);
        }

        int size = dataIn.size();
        if (size == 0)
        {
            return list;
        }

        EntityData current = dataIn.get(0);
        boolean dupe = false;

        for (int i = 1; i < size; i++)
        {
            EntityData next = dataIn.get(i);

            if (next.uuid.equals(current.uuid))
            {
                if (dupe == false)
                {
                    list.add(current);
                }

                list.add(next);
                dupe = true;
            }
            else
            {
                dupe = false;
            }

            current = next;
        }

        return list;
    }

    public static List<EntityData> getDuplicateEntriesExcludingFirst(List<EntityData> dataIn, boolean sortFirst)
    {
        List<EntityData> list = new ArrayList<EntityData>();

        if (sortFirst)
        {
            Collections.sort(dataIn);
        }

        int size = dataIn.size();
        if (size == 0)
        {
            return list;
        }

        EntityData current = dataIn.get(0);

        for (int i = 1; i < size; i++)
        {
            EntityData next = dataIn.get(i);

            if (next.uuid.equals(current.uuid))
            {
                list.add(next);
            }

            current = next;
        }

        return list;
    }

    public static List<String> getFormattedOutputLines(List<EntityData> dataIn, boolean sortFirst)
    {
        List<String> lines = new ArrayList<String>();

        if (sortFirst)
        {
            Collections.sort(dataIn);
        }

        for (EntityData entry : dataIn)
        {
            String str = getFormattedOutput(entry);

            if (entry.uuid.getLeastSignificantBits() == 0 && entry.uuid.getMostSignificantBits() == 0)
            {
                WorldTools.logger.warn("Entity: {} UUID: most = 0, least = 0 => {}", entry.id, entry.uuid.toString());
            }

            lines.add(str);
        }

        return lines;
    }

    public static String getFormattedOutput(EntityData data)
    {
        return String.format("%s %32s @ {DIM: %3d pos: x = %8.2f, y = %8.2f, z = %8.2f chunk: (%4d, %4d)}",
                data.uuid.toString(), data.id, data.dimension,
                data.pos.xCoord, data.pos.yCoord, data.pos.zCoord, data.chunkPos.chunkXPos, data.chunkPos.chunkZPos);
    }
}
