package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.event.TickHandler;
import fi.dy.masa.worldutils.event.tasks.ITask;
import fi.dy.masa.worldutils.event.tasks.TaskRegionDirectoryProcessor;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.FileUtils.Region;
import fi.dy.masa.worldutils.util.PositionUtils;

public class EntityTools
{
    private static final EntityTools INSTANCE = new EntityTools();
    private final EntityDataReader entityDataReader = new EntityDataReader();

    private class EntityDataReader implements IWorldDataHandler
    {
        @Nullable
        private final File regionDir;
        private final boolean removeDuplicates;
        private final int dimension;
        private int regionCount;
        private int chunkCount;
        private int entityCount;
        private ChunkProviderServer provider;
        private List<EntityData> entities = new ArrayList<EntityData>();

        public EntityDataReader()
        {
            this.regionDir = null;
            this.dimension = 0;
            this.removeDuplicates = false;
        }

        public EntityDataReader(int dimension, boolean removeDuplicates)
        {
            this.regionDir = null;
            this.dimension = dimension;
            this.removeDuplicates = removeDuplicates;
        }

        public EntityDataReader(File regionDir, boolean removeDuplicates)
        {
            this.regionDir = regionDir;
            this.dimension = 0;
            this.removeDuplicates = removeDuplicates;
        }

        public List<EntityData> getEntities()
        {
            return this.entities;
        }

        @Override
        public void init(int dimension)
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
        public int processRegion(Region region, boolean simulate)
        {
            this.regionCount++;

            return 0;
        }

        @Override
        public int processChunk(Region region, int chunkX, int chunkZ, boolean simulate)
        {
            NBTTagCompound chunkNBT = null;
            int count = 0;

            try
            {
                DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

                if (data == null)
                {
                    WorldUtils.logger.warn("EntityDataReader#processChunk(): Failed to get chunk data input stream for chunk [{}, {}] from region file '{}'",
                            chunkX, chunkZ, region.getAbsolutePath());
                    return 0;
                }

                chunkNBT = CompressedStreamTools.read(data);
                data.close();
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("EntityDataReader#processChunk(): Failed to read Chunk NBT data for Chunk [{}, {}] in region file '{}' ({})",
                        chunkX, chunkZ, region.getAbsolutePath(), e.getMessage());
                return 0;
            }

            if (chunkNBT != null)
            {
                NBTTagCompound level = chunkNBT.getCompoundTag("Level");

                if (level.hasKey("Entities", Constants.NBT.TAG_LIST))
                {
                    ChunkPos chunkPos = new ChunkPos(level.getInteger("xPos"), level.getInteger("zPos"));

                    if (this.provider != null && this.provider.chunkExists(chunkPos.x, chunkPos.z))
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

                this.chunkCount++;
                this.entityCount += count;
            }

            return count;
        }

        @Override
        public void finish(ICommandSender sender, boolean simulate)
        {
            if (this.entityCount > 0)
            {
                String chatOutput = String.format("Read a total of %d entities from %d chunks in %d region files",
                        this.entityCount, this.chunkCount, this.regionCount);

                sender.sendMessage(new TextComponentString(chatOutput));
                WorldUtils.logger.info(chatOutput);
            }

            if (this.provider != null)
            {
                String chatOutput = String.format("There were %d chunks currently loaded, the entity list does not include entities in those chunks!!",
                        this.provider.getLoadedChunkCount());

                sender.sendMessage(new TextComponentString(chatOutput));
                WorldUtils.logger.info(chatOutput);
            }

            if (this.removeDuplicates)
            {
                List<EntityData> dupes = getDuplicateEntitiesExcludingFirst(this.getEntities(), true);
                EntityDuplicateRemover remover;

                if (this.regionDir != null)
                {
                    remover = new EntityDuplicateRemover(this.regionDir, dupes);
                }
                else
                {
                    remover = new EntityDuplicateRemover(this.dimension, dupes);
                }

                TaskScheduler.getInstance().scheduleTask(remover, 1);
            }
        }
    }

    public class EntityDuplicateRemover implements IChunkDataHandler, ITask
    {
        @Nullable
        private final File regionDir;
        private final File worldDir;
        private final Map<ChunkPos, Map<ChunkPos, List<EntityData>>> entitiesByRegion;
        private Iterator<Map.Entry<ChunkPos, Map<ChunkPos, List<EntityData>>>> regionIter;
        private Iterator<Map.Entry<ChunkPos, List<EntityData>>> chunkIter;
        private Map.Entry<ChunkPos, Map<ChunkPos, List<EntityData>>> regionEntry;
        private List<EntityData> toRemoveCurrentChunk;
        private Region region;
        private int dimension;
        private int entityCount;
        private int regionCount;
        private int chunkCount;
        private int tickCount;

        public EntityDuplicateRemover(int dimension, List<EntityData> dupes)
        {
            this.dimension = dimension;
            this.regionDir = null;
            this.worldDir = FileUtils.getWorldSaveLocation(dimension);
            this.entitiesByRegion = this.sortEntitiesByRegionAndChunk(dupes);
            this.regionIter = this.entitiesByRegion.entrySet().iterator();
        }

        public EntityDuplicateRemover(File regionDir, List<EntityData> dupes)
        {
            this.regionDir = regionDir;
            this.worldDir = this.regionDir.getParentFile();
            this.entitiesByRegion = this.sortEntitiesByRegionAndChunk(dupes);
            this.regionIter = this.entitiesByRegion.entrySet().iterator();
        }

        @Override
        public void init()
        {
        }

        @Override
        public boolean canExecute()
        {
            return this.worldDir != null;
        }

        @Override
        public boolean execute()
        {
            this.tickCount++;

            while (true)
            {
                if (this.checkTickTime())
                {
                    return false;
                }

                if (this.chunkIter == null || this.chunkIter.hasNext() == false)
                {
                    if (this.regionIter.hasNext() == false)
                    {
                        return true;
                    }

                    this.regionEntry = this.regionIter.next();
                    this.region = Region.fromRegionCoords(this.worldDir, this.regionEntry.getKey());
                    this.chunkIter = this.regionEntry.getValue().entrySet().iterator();
                    this.regionCount++;
                }

                if (this.chunkIter.hasNext())
                {
                    Map.Entry<ChunkPos, List<EntityData>> chunkEntry = this.chunkIter.next();
                    this.toRemoveCurrentChunk = chunkEntry.getValue();
                    this.entityCount += FileUtils.handleChunkInRegion(this.region, chunkEntry.getKey(), this, false);
                    this.chunkCount++;
                }
            }
        }

        @Override
        public void stop()
        {
            String pre = this.regionDir != null ? "external = " + this.regionDir.getAbsolutePath() : ("DIM " + this.dimension);

            WorldUtils.logger.info("{}: Removed a total of {} duplicate entities from {} chunks in {} regions",
                    pre, this.entityCount, this.chunkCount, this.regionCount);
        }

        @Override
        public int processChunkData(ChunkPos chunkPos, NBTTagCompound chunkNBT, boolean simulate)
        {
            int entityCount = 0;
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");

            if (level.hasKey("Entities", Constants.NBT.TAG_LIST))
            {
                NBTTagList list = level.getTagList("Entities", Constants.NBT.TAG_COMPOUND);

                // This has to happen with nested loops and not a "this.toRemoveCurrentChunk.contains()",
                // otherwise we would/could also remove the one that is supposed to be kept.
                // In other words, the toRemove list is specifically created for each chunk
                // and contains exactly what to remove.
                for (EntityData data : this.toRemoveCurrentChunk)
                {
                    int size = list.tagCount();

                    for (int i = 0; i < size; i++)
                    {
                        NBTTagCompound entity = list.getCompoundTagAt(i);

                        if (entity.getLong("UUIDMost") == data.getUUID().getMostSignificantBits() &&
                            entity.getLong("UUIDLeast") == data.getUUID().getLeastSignificantBits() &&
                            entity.getString("id").equals(data.getId()))
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

            WorldUtils.logger.info("In region {}, chunk [{}, {}] - removed {} duplicate entities",
                    this.region.getRegionFileName(), chunkPos.x, chunkPos.z, entityCount);

            return entityCount;
        }

        private Map<ChunkPos, Map<ChunkPos, List<EntityData>>> sortEntitiesByRegionAndChunk(List<EntityData> listIn)
        {
            Map<ChunkPos, Map<ChunkPos, List<EntityData>>> entitiesByRegion = new HashMap <ChunkPos, Map<ChunkPos, List<EntityData>>>();

            for (EntityData entry : listIn)
            {
                PositionUtils.addDataForChunkInLists(entitiesByRegion, entry.getChunkPosition(), entry);
            }

            return entitiesByRegion;
        }

        private boolean checkTickTime()
        {
            long timeCurrent = System.currentTimeMillis();

            if ((timeCurrent - TickHandler.instance().getTickStartTime()) >= 48L)
            {
                // Status message every 5 seconds
                if ((this.tickCount % 100) == 0)
                {
                    WorldUtils.logger.info("EntityDuplicateRemover progress: Handled {} chunks in {} region files...", this.chunkCount, this.regionCount);
                }

                return true;
            }

            return false;
        }
    }

    public class EntityRemover implements IWorldDataHandler
    {
        private final Set<String> toRemove = new HashSet<String>();
        private ChunkProviderServer provider;
        private int regionCount;
        private int chunkCount;
        private int entityCount;
        private final String tagName;

        public EntityRemover(List<String> toRemove, EntityRenamer.Type type)
        {
            this.toRemove.addAll(toRemove);
            this.tagName = type == EntityRenamer.Type.TILE_ENTITIES ? "TileEntities" : "Entities";
        }

        @Override
        public void init(int dimension)
        {
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
        public int processRegion(Region region, boolean simulate)
        {
            this.regionCount++;
            return 0;
        }

        @Override
        public int processChunk(Region region, int chunkX, int chunkZ, boolean simulate)
        {
            NBTTagCompound chunkNBT = null;
            int count = 0;

            try
            {
                DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

                if (data == null)
                {
                    WorldUtils.logger.warn("EntityRemover#processChunk(): Failed to get chunk data input stream for chunk [{}, {}] from region file '{}'",
                            chunkX, chunkZ, region.getAbsolutePath());
                    return 0;
                }

                chunkNBT = CompressedStreamTools.read(data);
                data.close();
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("EntityRemover#processChunk(): Failed to read chunk data for chunk [{}, {}] from region file '{}' ({})",
                        chunkX, chunkZ, region.getAbsolutePath(), e.getMessage());
            }

            if (chunkNBT != null)
            {
                NBTTagCompound level = chunkNBT.getCompoundTag("Level");

                if (level.hasKey(this.tagName, Constants.NBT.TAG_LIST))
                {
                    int chunkAbsX = level.getInteger("xPos");
                    int chunkAbsZ = level.getInteger("zPos");

                    if (this.provider != null && this.provider.chunkExists(chunkAbsX, chunkAbsZ))
                    {
                        return 0;
                    }

                    NBTTagList list = level.getTagList(this.tagName, Constants.NBT.TAG_COMPOUND);

                    for (int i = 0; i < list.tagCount(); i++)
                    {
                        NBTTagCompound entity = list.getCompoundTagAt(i);

                        if (this.toRemove.contains(entity.getString("id")))
                        {
                            if (simulate == false)
                            {
                                list.removeTag(i);
                                i--;
                            }

                            count++;
                        }
                    }

                    if (simulate == false && count > 0)
                    {
                        try
                        {
                            DataOutputStream dataOut = region.getRegionFile().getChunkDataOutputStream(chunkX, chunkZ);
                            CompressedStreamTools.write(chunkNBT, dataOut);
                            dataOut.close();
                        }
                        catch (Exception e)
                        {
                            WorldUtils.logger.warn("EntityRemover#processChunk(): Failed to write chunk data for chunk [{}, {}] in region file '{}' ({})",
                                    chunkX, chunkZ, region.getAbsolutePath(), e.getMessage());
                        }
                    }
                }

                this.chunkCount++;
                this.entityCount += count;
            }

            return count;
        }

        @Override
        public void finish(ICommandSender sender, boolean simulate)
        {
            String chatOutput = String.format("Removed a total of %d %s from %d chunks in %d region files",
                    this.entityCount, this.tagName, this.chunkCount, this.regionCount);

            sender.sendMessage(new TextComponentString(chatOutput));
            WorldUtils.logger.info(chatOutput);

            if (this.provider != null)
            {
                chatOutput = String.format("There were %d chunks currently loaded, the %s in those chunks were not removed!!",
                        this.provider.getLoadedChunkCount(), this.tagName);

                sender.sendMessage(new TextComponentString(chatOutput));
                WorldUtils.logger.info(chatOutput);
            }
        }
    }

    public static class EntityRenamer implements IWorldDataHandler
    {
        private final Map<String, String> renamePairs = new HashMap<String, String>();
        private ChunkProviderServer provider;
        private int regionCount;
        private int chunkCount;
        private int entityCount;
        private final String tagName;

        public static enum Type
        {
            ENTITIES,
            TILE_ENTITIES;
        }

        public EntityRenamer(List<Pair<String, String>> renamePairs, Type type)
        {
            for (Pair<String, String> pair : renamePairs)
            {
                this.renamePairs.put(pair.getLeft(), pair.getRight());
            }

            this.tagName = type == Type.TILE_ENTITIES ? "TileEntities" : "Entities";
        }

        @Override
        public void init(int dimension)
        {
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
        public int processRegion(Region region, boolean simulate)
        {
            this.regionCount++;
            return 0;
        }

        @Override
        public int processChunk(Region region, int chunkX, int chunkZ, boolean simulate)
        {
            NBTTagCompound chunkNBT = null;
            int count = 0;

            try
            {
                DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

                if (data == null)
                {
                    WorldUtils.logger.warn("EntityRenamer#processChunk(): Failed to get chunk data input stream for chunk [{}, {}] from region file '{}'",
                            chunkX, chunkZ, region.getAbsolutePath());
                    return 0;
                }

                chunkNBT = CompressedStreamTools.read(data);
                data.close();
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("EntityRenamer#processChunk(): Failed to read chunk NBT data for chunk [{}, {}] from region file '{}' ({})",
                        chunkX, chunkZ, region.getAbsolutePath(), e.getMessage());
                return 0;
            }

            if (chunkNBT != null)
            {
                NBTTagCompound level = chunkNBT.getCompoundTag("Level");

                if (level.hasKey(this.tagName, Constants.NBT.TAG_LIST))
                {
                    ChunkPos chunkPos = new ChunkPos(level.getInteger("xPos"), level.getInteger("zPos"));
                    if (this.provider != null && this.provider.chunkExists(chunkPos.x, chunkPos.z))
                    {
                        return 0;
                    }

                    NBTTagList list = level.getTagList(this.tagName, Constants.NBT.TAG_COMPOUND);

                    for (int i = 0; i < list.tagCount(); i++)
                    {
                        NBTTagCompound entity = list.getCompoundTagAt(i);
                        String id = entity.getString("id");

                        if (this.renamePairs.containsKey(id))
                        {
                            if (simulate == false)
                            {
                                entity.setString("id", this.renamePairs.get(id));
                            }

                            count++;
                        }
                    }

                    if (simulate == false && count > 0)
                    {
                        try
                        {
                            DataOutputStream dataOut = region.getRegionFile().getChunkDataOutputStream(chunkX, chunkZ);
                            CompressedStreamTools.write(chunkNBT, dataOut);
                            dataOut.close();
                        }
                        catch (Exception e)
                        {
                            WorldUtils.logger.warn("EntityRenamer#processChunk(): Failed to write chunk data for chunk [{}, {}] in region file '{}' ({})",
                                    chunkX, chunkZ, region.getAbsolutePath(), e.getMessage());
                            return 0;
                        }
                    }
                }

                this.chunkCount++;
                this.entityCount += count;
            }

            return count;
        }

        @Override
        public void finish(ICommandSender sender, boolean simulate)
        {
            String chatOutput = String.format("Renamed a total of %d %s in %d chunks in %d region files",
                    this.entityCount, this.tagName, this.chunkCount, this.regionCount);

            sender.sendMessage(new TextComponentString(chatOutput));
            WorldUtils.logger.info(chatOutput);

            if (this.provider != null)
            {
                chatOutput = String.format("There were %d chunks currently loaded, the %s in those chunks were not renamed!!",
                        this.provider.getLoadedChunkCount(), this.tagName);

                sender.sendMessage(new TextComponentString(chatOutput));
                WorldUtils.logger.info(chatOutput);
            }
        }
    }

    private EntityTools()
    {
    }

    public static EntityTools instance()
    {
        return INSTANCE;
    }

    public void readEntities(int dimension, ICommandSender sender) throws CommandException
    {
        this.entityDataReader.init(dimension);
        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, this.entityDataReader, sender, 50), 1);
    }

    public void readEntities(File regionDir, ICommandSender sender) throws CommandException
    {
        this.entityDataReader.init(0);
        TaskScheduler.getInstance().scheduleTask(new TaskRegionDirectoryProcessor(regionDir, this.entityDataReader, sender, 50), 1);
    }

    public void removeAllDuplicateEntities(int dimension, ICommandSender sender) throws CommandException
    {
        EntityDataReader reader = new EntityDataReader(dimension, true);
        reader.init(dimension);
        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, reader, sender, 50), 1);
    }

    public void removeAllDuplicateEntities(File regionDir, ICommandSender sender) throws CommandException
    {
        EntityDataReader reader = new EntityDataReader(regionDir, true);
        reader.init(0);
        TaskScheduler.getInstance().scheduleTask(new TaskRegionDirectoryProcessor(regionDir, reader, sender, 50), 1);
    }

    public void removeEntities(int dimension, List<String> toRemove, EntityRenamer.Type type, ICommandSender sender) throws CommandException
    {
        EntityRemover remover = new EntityRemover(toRemove, type);
        remover.init(dimension);
        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, remover, sender, 50), 1);
    }

    public void removeEntities(File regionDir, List<String> toRemove, EntityRenamer.Type type, ICommandSender sender) throws CommandException
    {
        EntityRemover remover = new EntityRemover(toRemove, type);
        remover.init(0);
        TaskScheduler.getInstance().scheduleTask(new TaskRegionDirectoryProcessor(regionDir, remover, sender, 50), 1);
    }

    public void renameEntities(int dimension, List<Pair<String, String>> renamePairs, EntityRenamer.Type type, ICommandSender sender) throws CommandException
    {
        EntityRenamer renamer = new EntityRenamer(renamePairs, type);
        renamer.init(dimension);
        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, renamer, sender, 50), 1);
    }

    public void renameEntities(File regionDir, List<Pair<String, String>> renamePairs, EntityRenamer.Type type, ICommandSender sender) throws CommandException
    {
        EntityRenamer renamer = new EntityRenamer(renamePairs, type);
        renamer.init(0);
        TaskScheduler.getInstance().scheduleTask(new TaskRegionDirectoryProcessor(regionDir, renamer, sender, 50), 1);
    }

    private static List<EntityData> getDuplicateEntitiesIncludingFirst(List<EntityData> dataIn, boolean sortFirst)
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

            if (next.getUUID().equals(current.getUUID()))
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

    private static List<EntityData> getDuplicateEntitiesExcludingFirst(List<EntityData> dataIn, boolean sortFirst)
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

            if (next.getUUID().equals(current.getUUID()))
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
            dupes = getDuplicateEntitiesIncludingFirst(this.entityDataReader.getEntities(), sortFirst);
        }
        else
        {
            dupes = getDuplicateEntitiesExcludingFirst(this.entityDataReader.getEntities(), sortFirst);
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
            int len = entry.getId().length();

            if (len > longestId)
            {
                longestId = len;
            }
        }

        String format = "%s %" + longestId + "s @ {DIM: %3d pos: x = %8.2f, y = %8.2f, z = %8.2f chunk: (%5d, %5d) region: r.%d.%d.mca}";

        for (EntityData entry : dataIn)
        {
            String str = this.getFormattedOutput(entry, format);

            if (entry.getUUID().getLeastSignificantBits() == 0 && entry.getUUID().getMostSignificantBits() == 0)
            {
                WorldUtils.logger.warn("Entity: {} UUID: most = 0, least = 0 => {}", entry.getId(), entry.getUUID().toString());
            }

            lines.add(str);
        }

        return lines;
    }

    private String getFormattedOutput(EntityData data, String format)
    {
        return String.format(format, data.getUUID().toString(), data.getId(), data.getDimension(),
                data.getPosition().x, data.getPosition().y, data.getPosition().z,
                data.getChunkPosition().x, data.getChunkPosition().z,
                data.getChunkPosition().x >> 5, data.getChunkPosition().z >> 5);
    }
}
