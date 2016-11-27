package fi.dy.masa.worldtools.data;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldtools.WorldTools;
import fi.dy.masa.worldtools.util.FileUtils;
import fi.dy.masa.worldtools.util.FileUtils.Region;

public class EntityReader
{
    private static final EntityReader INSTANCE = new EntityReader();
    private final EntityDataReader entityDataReader = new EntityDataReader();

    private class EntityDataReader implements IWorldDataHandler
    {
        private ChunkProviderServer provider;
        private int regionCount;
        private int chunkCount;
        private int entityCount;
        private List<EntityData> entities = new ArrayList<EntityData>();

        public EntityDataReader()
        {
        }

        public List<EntityData> getEntities()
        {
            return this.entities;
        }

        @Override
        public void init()
        {
            this.entities.clear();

            this.regionCount = 0;
            this.chunkCount = 0;
            this.entityCount = 0;
        }

        @Override
        public void setChunkProvider(@Nullable ChunkProviderServer provider)
        {
            this.provider = provider;
        }

        @Override
        public int processRegion(Region region)
        {
            this.regionCount++;

            return 0;
        }

        @Override
        public int processChunk(Region region, int chunkX, int chunkZ)
        {
            int count = 0;
            DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

            if (data == null)
            {
                WorldTools.logger.warn("Failed to read chunk data for chunk ({}, {}) from file '{}'", chunkX, chunkZ, region.getName());
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
                    if (provider != null && provider.chunkExists(chunkPos.chunkXPos, chunkPos.chunkZPos))
                    {
                        return 0;
                    }

                    NBTTagList list = level.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

                    for (int i = 0; i < list.tagCount(); i++)
                    {
                        NBTTagCompound entity = list.getCompoundTagAt(i);
                        NBTTagList posList = entity.getTagList("Pos", Constants.NBT.TAG_DOUBLE);
                        int dim = entity.getInteger("Dimension");
                        Vec3d pos = new Vec3d(posList.getDoubleAt(0), posList.getDoubleAt(1), posList.getDoubleAt(2));
                        UUID uuid = new UUID(entity.getLong("UUIDMost"), entity.getLong("UUIDLeast"));

                        this.entities.add(new EntityData(dim, entity.getString("id"), pos, chunkPos, uuid));
                        count++;
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            this.chunkCount++;
            this.entityCount += count;

            return count;
        }

        @Override
        public void finish(ICommandSender sender)
        {
            if (this.entityCount > 0)
            {
                String chatOutput = String.format("Read a total of %d entities from %d chunks in %d region files",
                        this.entityCount, this.chunkCount, this.regionCount);

                sender.sendMessage(new TextComponentString(chatOutput));
                WorldTools.logger.info(chatOutput);
            }

            if (this.provider != null)
            {
                String chatOutput = String.format("There were %d chunks currently loaded, the entity list does not include entities in those chunks!!",
                        this.provider.getLoadedChunkCount());

                sender.sendMessage(new TextComponentString(chatOutput));
                WorldTools.logger.warn(chatOutput);
            }
        }
        
    }

    private class EntityDuplicateRemover implements IChunkDataHandler
    {
        private final Region region;
        private final List<EntityData> toRemove;

        public EntityDuplicateRemover(Region region, List<EntityData> toRemove)
        {
            this.region = region;
            this.toRemove = toRemove;
        }

        @Override
        public int processData(ChunkPos chunkPos, NBTTagCompound chunkNBT, boolean simulate)
        {
            int entityCount = 0;
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");

            if (level.hasKey("Entities", Constants.NBT.TAG_LIST))
            {
                NBTTagList list = level.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

                for (EntityData entry : this.toRemove)
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
            }

            WorldTools.logger.info("In region {}, chunk {}, {} - removed {} duplicate entities",
                    this.region.getName(), chunkPos.chunkXPos, chunkPos.chunkZPos, entityCount);

            return entityCount;
        }
    }

    private EntityReader()
    {
    }

    public static EntityReader instance()
    {
        return INSTANCE;
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

    public void readEntities(int dimension, ICommandSender sender)
    {
        this.entityDataReader.init();
        FileUtils.worldDataProcessor(dimension, this.entityDataReader, sender);
    }

    public String removeAllDuplicateEntities(int dimension, boolean simulate, ICommandSender sender)
    {
        File worldSaveLocation = FileUtils.getWorldSaveLocation(dimension);
        File regionDir = new File(worldSaveLocation, "region");
        int removedTotal = 0;
        Region region = null;

        try
        {
            if (regionDir.exists() && regionDir.isDirectory())
            {
                this.entityDataReader.init();
                FileUtils.worldDataProcessor(dimension, this.entityDataReader, sender);

                List<EntityData> dupes = getDuplicateEntriesExcludingFirst(this.entityDataReader.getEntities(), true);
                Map<ChunkPos, Map<ChunkPos, List<EntityData>>> entitiesByRegion = this.sortEntitiesByRegionAndChunk(dupes);

                for (Map.Entry<ChunkPos, Map<ChunkPos, List<EntityData>>> regionEntry : entitiesByRegion.entrySet())
                {
                    ChunkPos regionPos = regionEntry.getKey();
                    region = new Region(regionDir, regionPos);

                    for (Map.Entry<ChunkPos, List<EntityData>> chunkEntry : regionEntry.getValue().entrySet())
                    {
                        List<EntityData> toRemove = chunkEntry.getValue();
                        EntityDuplicateRemover entityDuplicateRemover = new EntityDuplicateRemover(region, toRemove);

                        removedTotal += FileUtils.handleChunkInRegion(region, chunkEntry.getKey(), entityDuplicateRemover, simulate);
                    }

                    region.getRegionFile().close();
                }
            }
        }
        catch (IOException e)
        {
            WorldTools.logger.warn("Exception while removing duplicate entities from region '{}'", region != null ? region.getName() : "");
            e.printStackTrace();
        }

        return "Removed a total of " + removedTotal + " entities";
    }

    private List<EntityData> getDuplicateEntriesIncludingFirst(List<EntityData> dataIn, boolean sortFirst)
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

    private List<EntityData> getDuplicateEntriesExcludingFirst(List<EntityData> dataIn, boolean sortFirst)
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

    public List<String> getDuplicateEntitiesOutput(boolean includeFirst, boolean sortFirst)
    {
        List<EntityData> dupes;

        if (includeFirst)
        {
            dupes = this.getDuplicateEntriesIncludingFirst(this.entityDataReader.getEntities(), sortFirst);
        }
        else
        {
            dupes = this.getDuplicateEntriesExcludingFirst(this.entityDataReader.getEntities(), sortFirst);
        }

        return this.getFormattedOutputLines(dupes, sortFirst);
    }

    public List<String> getAllEntitiesOutput(boolean sortFirst)
    {
        return this.getFormattedOutputLines(this.entityDataReader.getEntities(), sortFirst);
    }

    private List<String> getFormattedOutputLines(List<EntityData> dataIn, boolean sortFirst)
    {
        List<String> lines = new ArrayList<String>();

        if (sortFirst)
        {
            Collections.sort(dataIn);
        }

        int longestId = 0;

        for (EntityData entry : dataIn)
        {
            int len = entry.id.length();

            if (len > longestId)
            {
                longestId = len;
            }
        }

        String format = "%s %" + longestId + "s @ {DIM: %3d pos: x = %8.2f, y = %8.2f, z = %8.2f chunk: (%5d, %5d) region: r.%d.%d.mca}";

        for (EntityData entry : dataIn)
        {
            String str = this.getFormattedOutput(entry, format);

            if (entry.uuid.getLeastSignificantBits() == 0 && entry.uuid.getMostSignificantBits() == 0)
            {
                WorldTools.logger.warn("Entity: {} UUID: most = 0, least = 0 => {}", entry.id, entry.uuid.toString());
            }

            lines.add(str);
        }

        return lines;
    }

    private String getFormattedOutput(EntityData data, String format)
    {
        return String.format(format, data.uuid.toString(), data.id, data.dimension, data.pos.xCoord, data.pos.yCoord, data.pos.zCoord,
                data.chunkPos.chunkXPos, data.chunkPos.chunkZPos, data.chunkPos.chunkXPos >> 5, data.chunkPos.chunkZPos >> 5);
    }
}
