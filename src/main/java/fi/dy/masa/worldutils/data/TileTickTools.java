package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class TileTickTools
{
    private static final TileTickTools INSTANCE = new TileTickTools();
    private final TileTickReader tileTickReader = new TileTickReader();
    private final Set<String> toRemoveMods = new HashSet<String>();
    private final Set<String> toRemoveNames = new HashSet<String>();

    public static enum Operation
    {
        READ,
        FIND_INVALID,
        REMOVE_INVALID,
        REMOVE_BY_MOD,
        REMOVE_BY_NAME,
        REMOVE_ALL;
    }

    private TileTickTools()
    {
    }

    public static TileTickTools instance()
    {
        return INSTANCE;
    }

    private class TileTickReader implements IWorldDataHandler
    {
        protected ChunkProviderServer provider;
        protected List<TileTickData> tileTicks = new ArrayList<TileTickData>();
        protected List<TileTickData> invalidTicks = new ArrayList<TileTickData>();
        protected Set<String> foundIds = new HashSet<String>();
        protected Operation operation = Operation.READ;
        protected int regionCount;
        protected int chunkCount;
        protected int processedCount;
        protected boolean running;

        public List<TileTickData> getAllTileTicks()
        {
            if (this.running)
            {
                return Collections.emptyList();
            }

            return this.tileTicks;
        }

        public List<TileTickData> getInvalidTicksList()
        {
            if (this.running)
            {
                return Collections.emptyList();
            }

            return this.invalidTicks;
        }

        private void findAllInvalid()
        {
            this.invalidTicks.clear();

            for (TileTickData data : this.tileTicks)
            {
                // The ResourceLocation should only be null if the data was saved with an integer ID
                // and the block wasn't found.
                if (data.resource == null || Block.REGISTRY.getObject(data.resource) == Blocks.AIR)
                {
                    this.invalidTicks.add(data);
                }
            }
        }

        public void setOperation(Operation operation)
        {
            this.operation = operation;
        }

        @Override
        public void init(int dimension)
        {
            this.tileTicks.clear();

            this.regionCount = 0;
            this.chunkCount = 0;
            this.processedCount = 0;
            this.operation = Operation.READ;
            this.running = true;
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
            DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);
            int count = 0;

            if (data == null)
            {
                WorldUtils.logger.warn("TileTickReader#processChunk(): Failed to read chunk data for chunk ({}, {}) from file '{}'",
                        chunkX, chunkZ, region.getName());
                return 0;
            }

            try
            {
                NBTTagCompound nbt = CompressedStreamTools.read(data);
                data.close();
                NBTTagCompound level = nbt.getCompoundTag("Level");

                if (level.hasKey("TileTicks", Constants.NBT.TAG_LIST))
                {
                    ChunkPos chunkPos = new ChunkPos(level.getInteger("xPos"), level.getInteger("zPos"));

                    // This needs to use absolute Chunk coordinates
                    if (this.provider != null && this.provider.chunkExists(chunkPos.chunkXPos, chunkPos.chunkZPos))
                    {
                        return 0;
                    }

                    NBTTagList list = level.getTagList("TileTicks", Constants.NBT.TAG_COMPOUND);

                    for (int i = 0; i < list.tagCount(); i++)
                    {
                        NBTTagCompound tag = list.getCompoundTagAt(i);
                        int x = tag.getInteger("x");
                        int y = tag.getInteger("y");
                        int z = tag.getInteger("z");
                        int delay = tag.getInteger("t");
                        int priority = tag.getInteger("p");
                        Pair<ResourceLocation, String> pair = getBlockIdentifiers(tag, "i");

                        this.tileTicks.add(new TileTickData(chunkPos, new BlockPos(x, y, z), pair.getLeft(), pair.getRight(), delay, priority));
                        this.foundIds.add(pair.getRight());
                        count++;
                    }

                    //WorldUtils.logger.info("Read {} tile ticks in chunk [{}, {}] in region '{}'", count, chunkX, chunkZ, region.getName());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            this.chunkCount++;
            this.processedCount += count;

            return count;
        }

        @Override
        public void finish(ICommandSender sender, boolean simulate)
        {
            this.running = false;

            WorldUtils.logger.info("Read a total of {} tile ticks from {} chunks in {} region files",
                    this.processedCount, this.chunkCount, this.regionCount);
            sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.reader.info",
                    Integer.valueOf(this.processedCount), Integer.valueOf(this.chunkCount), Integer.valueOf(this.regionCount)));

            if (this.provider != null && this.provider.getLoadedChunkCount() > 0)
            {
                int loaded = this.provider.getLoadedChunkCount();
                WorldUtils.logger.info("There were {} chunks currently loaded, the tile tick list does not include data from those chunks!", loaded);
                sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.reader.loaded", Integer.valueOf(loaded)));
            }

            if (this.operation == Operation.FIND_INVALID)
            {
                this.findAllInvalid();
                WorldUtils.logger.info("Found {} invalid scheduled tile ticks in the world", this.invalidTicks.size());
                sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.readingandfindinginvalid.complete", this.invalidTicks.size()));
            }
        }
    }

    private class TileTickRemoverAll implements IWorldDataHandler
    {
        protected ChunkProviderServer provider;
        protected int regionCount;
        protected int chunkCount;
        protected int processedCount;

        @Override
        public void init(int dimension)
        {
        }

        @Override
        public void setChunkProvider(ChunkProviderServer provider)
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
            int count = 0;
            DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

            if (data == null)
            {
                WorldUtils.logger.warn("TileTickRemoverAll#processChunk(): Failed to read chunk data for chunk ({}, {}) from file '{}'",
                        chunkX, chunkZ, region.getName());
                return 0;
            }

            try
            {
                NBTTagCompound chunkNBT = CompressedStreamTools.read(data);
                data.close();
                NBTTagCompound level = chunkNBT.getCompoundTag("Level");

                if (level.hasKey("TileTicks", Constants.NBT.TAG_LIST))
                {
                    // This needs to use absolute Chunk coordinates
                    if (this.provider != null && this.provider.chunkExists(level.getInteger("xPos"), level.getInteger("zPos")))
                    {
                        return 0;
                    }

                    count = level.getTagList("TileTicks", Constants.NBT.TAG_COMPOUND).tagCount();

                    if (count > 0 && simulate == false)
                    {
                        level.setTag("TileTicks", new NBTTagList());

                        DataOutputStream dataOut = region.getRegionFile().getChunkDataOutputStream(chunkX, chunkZ);
                        CompressedStreamTools.write(chunkNBT, dataOut);
                        dataOut.close();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            this.chunkCount++;
            this.processedCount += count;

            return count;
        }

        @Override
        public void finish(ICommandSender sender, boolean simulate)
        {
            if (this.processedCount > 0)
            {
                WorldUtils.logger.info("Removed a total of {} tile ticks from {} chunks in {} region files",
                        this.processedCount, this.chunkCount, this.regionCount);
                sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.remover.info",
                        Integer.valueOf(this.processedCount), Integer.valueOf(this.chunkCount), Integer.valueOf(this.regionCount)));
            }

            if (this.provider != null && this.provider.getLoadedChunkCount() > 0)
            {
                int loaded = this.provider.getLoadedChunkCount();
                WorldUtils.logger.info("There were {} chunks currently loaded, the tile ticks were NOT removed from those chunks", loaded);
                sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.remover.loaded", Integer.valueOf(loaded)));
            }
        }
    }

    private class TileTickRemoverByModOrName extends TileTickRemoverAll
    {
        private final Operation operation;
        private final Set<String> toRemove;

        public TileTickRemoverByModOrName(Operation operation, Set<String> toRemove)
        {
            this.operation = operation;
            this.toRemove = toRemove;
        }

        @Override
        public int processChunk(Region region, int chunkX, int chunkZ, boolean simulate)
        {
            DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);
            int count = 0;

            if (data == null)
            {
                WorldUtils.logger.warn("TileTickRemoverByModOrName#processChunk(): Failed to read chunk data for chunk ({}, {}) from file '{}'",
                        chunkX, chunkZ, region.getName());
                return 0;
            }

            try
            {
                NBTTagCompound chunkNBT = CompressedStreamTools.read(data);
                data.close();
                NBTTagCompound level = chunkNBT.getCompoundTag("Level");

                if (level.hasKey("TileTicks", Constants.NBT.TAG_LIST))
                {
                    // This needs to use absolute Chunk coordinates
                    if (this.provider != null && this.provider.chunkExists(level.getInteger("xPos"), level.getInteger("zPos")))
                    {
                        return 0;
                    }

                    NBTTagList list = level.getTagList("TileTicks", Constants.NBT.TAG_COMPOUND);
                    int size = list.tagCount();

                    if (this.operation == Operation.REMOVE_BY_MOD)
                    {
                        for (int i = 0; i < size; i++)
                        {
                            NBTTagCompound tag = list.getCompoundTagAt(i);
                            Pair<ResourceLocation, String> pair = getBlockIdentifiers(tag, "i");
                            ResourceLocation rl = pair.getLeft();

                            if (rl != null && this.toRemove.contains(rl.getResourceDomain()))
                            {
                                if (simulate == false)
                                {
                                    list.removeTag(i);
                                    i--;
                                    size--;
                                }

                                count++;
                            }
                        }
                    }
                    else if (this.operation == Operation.REMOVE_BY_NAME)
                    {
                        for (int i = 0; i < size; i++)
                        {
                            NBTTagCompound tag = list.getCompoundTagAt(i);
                            String name = tag.hasKey("i", Constants.NBT.TAG_STRING) ? tag.getString("i") : String.valueOf(tag.getInteger("i"));

                            if (this.toRemove.contains(name))
                            {
                                if (simulate == false)
                                {
                                    list.removeTag(i);
                                    i--;
                                    size--;
                                }

                                count++;
                            }
                        }
                    }
                    else if (this.operation == Operation.REMOVE_INVALID)
                    {
                        for (int i = 0; i < size; i++)
                        {
                            NBTTagCompound tag = list.getCompoundTagAt(i);
                            String name = tag.hasKey("i", Constants.NBT.TAG_STRING) ? tag.getString("i") : String.valueOf(tag.getInteger("i"));

                            if (Block.REGISTRY.getObject(new ResourceLocation(name)) == Blocks.AIR)
                            {
                                if (simulate == false)
                                {
                                    list.removeTag(i);
                                    i--;
                                    size--;
                                }

                                count++;
                            }
                        }
                    }

                    if (simulate == false && count > 0)
                    {
                        DataOutputStream dataOut = region.getRegionFile().getChunkDataOutputStream(chunkX, chunkZ);
                        CompressedStreamTools.write(chunkNBT, dataOut);
                        dataOut.close();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            this.chunkCount++;
            this.processedCount += count;

            return count;
        }
    }

    private static Pair<ResourceLocation, String> getBlockIdentifiers(NBTTagCompound tag, String name)
    {
        if (tag.hasKey("i", Constants.NBT.TAG_STRING))
        {
            return getBlockIdentifiers(tag.getString("i"));
        }
        else
        {
            return getBlockIdentifiers(tag.getInteger("i"));
        }
    }

    private static Pair<ResourceLocation, String> getBlockIdentifiers(int id)
    {
        Block block = Block.getBlockById(id);

        if (block != null)
        {
            ResourceLocation rl = block.getRegistryName();
            return Pair.of(rl, rl.toString());
        }
        else
        {
            return Pair.of(null, String.valueOf(id));
        }
    }

    private static Pair<ResourceLocation, String> getBlockIdentifiers(String name)
    {
        return Pair.of(new ResourceLocation(name), name);
    }

    private Set<String> getRemoveSet(Operation operation)
    {
        if (operation == Operation.REMOVE_BY_MOD)
        {
            return this.toRemoveMods;
        }
        else if (operation == Operation.REMOVE_BY_NAME)
        {
            return this.toRemoveNames;
        }

        return Collections.emptySet();
    }

    public void resetFilters(Operation operation)
    {
        this.getRemoveSet(operation).clear();
    }

    public void addFilter(String name, Operation operation)
    {
        this.getRemoveSet(operation).add(name);
    }

    public void removeFilter(String name, Operation operation)
    {
        this.getRemoveSet(operation).remove(name);
    }

    public Set<String> getFilters(Operation operation)
    {
        return this.getRemoveSet(operation);
    }

    public void startTask(int dimension, Operation operation, boolean forceRescan, ICommandSender sender)
    {
        if (operation == Operation.READ || operation == Operation.FIND_INVALID)
        {
            if (operation == Operation.READ || forceRescan || this.tileTickReader.getAllTileTicks().size() == 0)
            {
                this.tileTickReader.init(dimension);
                this.tileTickReader.setOperation(operation);
                TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, this.tileTickReader, sender), 1);
            }
        }
        else if (operation == Operation.REMOVE_ALL)
        {
            TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, new TileTickRemoverAll(), sender), 1);
        }
        else if (operation == Operation.REMOVE_BY_MOD)
        {
            Set<String> toRemove = TileTickTools.this.getRemoveSet(operation);
            TileTickRemoverByModOrName remover = new TileTickRemoverByModOrName(Operation.REMOVE_BY_MOD, toRemove);
            TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, remover, sender), 1);
        }
        else if (operation == Operation.REMOVE_BY_NAME)
        {
            Set<String> toRemove = TileTickTools.this.getRemoveSet(operation);
            TileTickRemoverByModOrName remover = new TileTickRemoverByModOrName(Operation.REMOVE_BY_NAME, toRemove);
            TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, remover, sender), 1);
        }
        else if (operation == Operation.REMOVE_INVALID)
        {
            TileTickRemoverByModOrName remover = new TileTickRemoverByModOrName(Operation.REMOVE_INVALID, Collections.emptySet());
            TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, remover, sender), 1);
        }
    }

    public List<String> getAllTileTicksOutput(boolean sortFirst)
    {
        return this.getFormattedOutputLines(this.tileTickReader.getAllTileTicks(), sortFirst);
    }

    public List<String> getInvalidTileTicksOutput(boolean sortFirst)
    {
        return this.getFormattedOutputLines(this.tileTickReader.getInvalidTicksList(), sortFirst);
    }

    private List<String> getFormattedOutputLines(List<TileTickData> dataIn, boolean sortFirst)
    {
        List<String> lines = new ArrayList<String>();

        if (sortFirst)
        {
            Collections.sort(dataIn);
        }

        int longestId = 0;

        for (TileTickData entry : dataIn)
        {
            int len = entry.blockId.length();

            if (len > longestId)
            {
                longestId = len;
            }
        }

        String format = "%-" + longestId + "s - delay: %4d, priority: %2d @ {pos: x = %6d, y = %3d, z = %6d chunk: (%5d, %5d) region: r.%d.%d.mca}";

        for (TileTickData entry : dataIn)
        {
            lines.add(this.getFormattedOutput(entry, format));
        }

        return lines;
    }

    private String getFormattedOutput(TileTickData data, String format)
    {
        return String.format(format, data.blockId, data.delay, data.priority, data.blockPos.getX(), data.blockPos.getY(), data.blockPos.getZ(),
                data.chunkPos.chunkXPos, data.chunkPos.chunkZPos, data.chunkPos.chunkXPos >> 5, data.chunkPos.chunkZPos >> 5);
    }
}
