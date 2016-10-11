package fi.dy.masa.worldtools.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
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

    public String readEntities(int dimension)
    {
        this.entities.clear();
        String chatOutput = "";

        WorldServer world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(dimension);

        if (world != null)
        {
            ChunkProviderServer provider = world.getChunkProvider();
            File worldSaveLocation = ChunkUtils.getWorldSaveLocation(world);
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
                //WorldTools.logger.info(chatOutput);
            }

            int loaded = provider.getLoadedChunkCount();
            if (loaded > 0)
            {
                WorldTools.logger.warn("There were {} chunks currently loaded, the entity list will be inaccurate for those chunks!", loaded);
            }
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
                    entityCount += this.readEntitiesFomChunk(region, cx, cz, regionFile.getName());
                }
            }
        }

        return entityCount;
    }

    private int readEntitiesFomChunk(RegionFile region, int chunkX, int chunkZ, String regionName)
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
                NBTTagList list = level.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

                for (int i = 0; i < list.tagCount(); i++)
                {
                    NBTTagCompound entity = list.getCompoundTagAt(i);
                    NBTTagList posList = entity.getTagList("Pos", Constants.NBT.TAG_DOUBLE);
                    int dim = entity.getInteger("Dimension");
                    Vec3d pos = new Vec3d(posList.getDoubleAt(0), posList.getDoubleAt(1), posList.getDoubleAt(2));
                    UUID uuid = new UUID(entity.getLong("UUIDMost"), entity.getLong("UUIDLeast"));

                    this.entities.add(new EntityData(dim, entity.getString("id"), pos, uuid));
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

    public List<String> getEntityList()
    {
        List<String> lines = new ArrayList<String>();
        Collections.sort(this.entities);

        for (EntityData data : this.entities)
        {
            String str = String.format("%s %32s @ {DIM: %3d pos: x = %8.2f, y = %8.2f, z = %8.2f chunk: (%4d, %4d)}",
                    data.uuid.toString(), data.id, data.dimension, data.pos.xCoord, data.pos.yCoord, data.pos.zCoord,
                    ((int) Math.floor(data.pos.xCoord)) >> 4, ((int) Math.floor(data.pos.zCoord)) >> 4);

            if (data.uuid.toString().startsWith("00000000"))
            {
                WorldTools.logger.warn("Entity: {} UUID: M = {}, L = {} => {}", data.id, data.uuid.getMostSignificantBits(), data.uuid.getLeastSignificantBits(), data.uuid.toString());
            }

            lines.add(str);
        }

        return lines;
    }
}
