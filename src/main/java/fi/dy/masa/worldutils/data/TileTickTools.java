package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
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
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class TileTickTools
{
    private static final TileTickTools INSTANCE = new TileTickTools();
    private final TileTickReader tileTickReader = new TileTickReader();
    private final Set<String> namesToRemove = new HashSet<String>();

    public static enum RemoveType
    {
        ALL,
        BY_MOD,
        BY_NAME;
    }

    private class TileTickReader implements IWorldDataHandler
    {
        protected ChunkProviderServer provider;
        protected int regionCount;
        protected int chunkCount;
        protected int processedCount;
        protected List<TileTickData> tileTicks = new ArrayList<TileTickData>();
        protected List<TileTickData> invalidTicks = new ArrayList<TileTickData>();
        protected Set<String> missingIds = new HashSet<String>();

        public List<TileTickData> getAllTileTicks()
        {
            return this.tileTicks;
        }

        public Set<String> getMissingIdsSet()
        {
            return this.missingIds;
        }

        public List<TileTickData> getInvalidTicksList()
        {
            return this.invalidTicks;
        }

        public void findInvalid()
        {
            Set<ResourceLocation> existingBlocks = Block.REGISTRY.getKeys();
            this.missingIds.clear();
            this.invalidTicks.clear();

            for (TileTickData data : this.tileTicks)
            {
                // The ResourceLocation should only be null if the data was saved with an integer ID
                // and the block wasn't found.
                if (data.resource == null || existingBlocks.contains(data.resource) == false)
                {
                    this.invalidTicks.add(data);
                    this.missingIds.add(data.blockId);
                }
            }
        }

        @Override
        public void init()
        {
            this.tileTicks.clear();

            this.regionCount = 0;
            this.chunkCount = 0;
            this.processedCount = 0;
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
            WorldUtils.logger.info("Read a total of {} tile ticks from {} chunks in {} region files",
                    this.processedCount, this.chunkCount, this.regionCount);
            sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.reader.info",
                    Integer.valueOf(this.processedCount), Integer.valueOf(this.chunkCount), Integer.valueOf(this.regionCount)));

            if (this.provider != null && this.provider.getLoadedChunkCount() > 0)
            {
                int loaded = this.provider.getLoadedChunkCount();
                WorldUtils.logger.warn("There were {} chunks currently loaded, the tile tick list does not include data from those chunks!", loaded);
                sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.reader.loaded", Integer.valueOf(loaded)));
            }
        }
        
    }

    private class TileTickRemover extends TileTickReader implements IChunkDataHandler
    {
        private final Region region;
        private final RemoveType type;
        private final Set<String> toRemove;

        public TileTickRemover()
        {
            this.region = null;
            this.type = RemoveType.ALL;
            this.toRemove = new HashSet<String>();
        }

        public TileTickRemover(Region region, RemoveType type, Set<String> toRemove)
        {
            this.region = region;
            this.type = type;
            this.toRemove = toRemove;
        }

        @Override
        public int processChunkData(ChunkPos chunkPos, NBTTagCompound chunkNBT, boolean simulate)
        {
            int count = 0;
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");

            if (level.hasKey("TileTicks", Constants.NBT.TAG_LIST))
            {
                NBTTagList list = level.getTagList("TileTicks", Constants.NBT.TAG_COMPOUND);

                if (this.type == RemoveType.ALL)
                {
                    count += list.tagCount();

                    if (simulate == false)
                    {
                        level.setTag("TileTicks", new NBTTagList());
                    }
                }
                else
                {
                    for (int i = 0; i < list.tagCount(); i++)
                    {
                        NBTTagCompound tag = list.getCompoundTagAt(i);

                        if (this.type == RemoveType.BY_MOD)
                        {
                            Pair<ResourceLocation, String> pair = getBlockIdentifiers(tag, "i");
                            ResourceLocation rl = pair.getLeft();

                            if (rl != null && this.toRemove.contains(rl.getResourceDomain()))
                            {
                                if (simulate == false)
                                {
                                    list.removeTag(i);
                                    i--;
                                }

                                count++;
                            }
                        }
                        else if (this.type == RemoveType.BY_NAME)
                        {
                            String name = tag.hasKey("i", Constants.NBT.TAG_STRING) ? tag.getString("i") : String.valueOf(tag.getInteger("i"));

                            if (this.toRemove.contains(name))
                            {
                                if (simulate == false)
                                {
                                    list.removeTag(i);
                                    i--;
                                }

                                count++;
                            }
                        }
                    }
                }
            }

            WorldUtils.logger.info("In region {}, chunk {}, {} - removed {} tile ticks",
                    this.region.getName() != null ? this.region.getName() : "null", chunkPos.chunkXPos, chunkPos.chunkZPos, count);

            return count;
        }

        @Override
        public int processChunk(Region region, int chunkX, int chunkZ, boolean simulate)
        {
            if (this.type != RemoveType.ALL)
            {
                return 0;
            }

            int count = 0;
            DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

            if (data == null)
            {
                WorldUtils.logger.warn("TileTickRemover#processChunk(): Failed to read chunk data for chunk ({}, {}) from file '{}'",
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
                WorldUtils.logger.warn("There were {} chunks currently loaded, the tile ticks were NOT removed from those chunks", loaded);
                sender.sendMessage(new TextComponentTranslation("worldutils.commands.tileticks.remover.loaded", Integer.valueOf(loaded)));
            }
        }
    }

    private TileTickTools()
    {
    }

    public static TileTickTools instance()
    {
        return INSTANCE;
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

    private List<TileTickData> getTileTicksToRemove(List<TileTickData> dataIn, RemoveType type, Set<String> toRemove)
    {
        List<TileTickData> list = new ArrayList<TileTickData>();

        if (type == RemoveType.ALL)
        {
            return dataIn;
        }

        for (TileTickData entry : dataIn)
        {
            if (type == RemoveType.BY_NAME && toRemove.contains(entry.blockId))
            {
                list.add(entry);
            }
            else if (type == RemoveType.BY_MOD && entry.resource != null && toRemove.contains(entry.resource.getResourceDomain()))
            {
                list.add(entry);
            }
        }

        return list;
    }

    private Set<ChunkPos> getSetForChunks(Map<ChunkPos, Set<ChunkPos>> mapRegions, ChunkPos regionPos)
    {
        Set<ChunkPos> setChunks = mapRegions.get(regionPos);

        if (setChunks == null)
        {
            setChunks = new HashSet<ChunkPos>();
            mapRegions.put(regionPos, setChunks);
        }

        return setChunks;
    }

    private Map<ChunkPos, Set<ChunkPos>> sortTileTicksByRegionAndChunk(List<TileTickData> listIn)
    {
        Map<ChunkPos, Set<ChunkPos>> tileTicksByRegion = new HashMap<ChunkPos, Set<ChunkPos>>();

        for (TileTickData entry : listIn)
        {
            ChunkPos regionPos = new ChunkPos(entry.chunkPos.chunkXPos >> 5, entry.chunkPos.chunkZPos >> 5);
            this.getSetForChunks(tileTicksByRegion, regionPos).add(entry.chunkPos);
        }

        return tileTicksByRegion;
    }

    public void resetFilters()
    {
        this.namesToRemove.clear();
    }

    public void addFilter(String name)
    {
        this.namesToRemove.add(name);
    }

    public void removeFilter(String name)
    {
        this.namesToRemove.remove(name);
    }

    public ImmutableSet<String> getFilters()
    {
        return ImmutableSet.copyOf(this.namesToRemove);
    }

    public int readTileTicks(int dimension, ICommandSender sender)
    {
        this.tileTickReader.init();
        FileUtils.worldDataProcessor(dimension, this.tileTickReader, sender, false);

        return this.tileTickReader.getAllTileTicks().size();
    }

    public int findInvalid(int dimension, boolean forceRescan, ICommandSender sender)
    {
        if (forceRescan || this.tileTickReader.getAllTileTicks().size() == 0)
        {
            this.readTileTicks(dimension, sender);
        }

        this.tileTickReader.findInvalid();

        return this.tileTickReader.getInvalidTicksList().size();
    }

    public int removeInvalid(int dimension, boolean forceRescan, ICommandSender sender)
    {
        if (forceRescan || this.tileTickReader.getMissingIdsSet().size() == 0)
        {
            this.findInvalid(dimension, forceRescan, sender);
        }

        Set<String> toRemove = this.tileTickReader.getMissingIdsSet();
        return this.removeTileTicks(dimension, RemoveType.BY_NAME, toRemove, false, sender);
    }

    public int removeTileTicks(int dimension, RemoveType type, boolean simulate, ICommandSender sender)
    {
        if (type == RemoveType.ALL)
        {
            return this.removeTileTicks(dimension, type, this.namesToRemove, simulate, sender);
        }

        if (this.tileTickReader.getAllTileTicks().size() == 0)
        {
            this.readTileTicks(dimension, sender);
        }

        return this.removeTileTicks(dimension, type, this.namesToRemove, simulate, sender);
    }

    private int removeTileTicks(int dimension, RemoveType type, Set<String> namesToRemove, boolean simulate, ICommandSender sender)
    {
        File worldDir = FileUtils.getWorldSaveLocation(dimension);
        File regionDir = new File(worldDir, "region");
        int removedTotal = 0;
        Region region = null;

        if (regionDir.exists() && regionDir.isDirectory())
        {
            if (type == RemoveType.ALL)
            {
                FileUtils.worldDataProcessor(dimension, new TileTickRemover(), sender, false);
            }
            else if (namesToRemove != null)
            {
                List<TileTickData> toRemove = this.getTileTicksToRemove(this.tileTickReader.getAllTileTicks(), type, namesToRemove);
                for (TileTickData data : toRemove)
                {
                    WorldUtils.logger.info("toRemove: {} @ {}", data.resource, data.chunkPos);
                }

                Map<ChunkPos, Set<ChunkPos>> tileTicksByRegion = this.sortTileTicksByRegionAndChunk(toRemove);

                for (Map.Entry<ChunkPos, Set<ChunkPos>> regionEntry : tileTicksByRegion.entrySet())
                {
                    ChunkPos regionPos = regionEntry.getKey();
                    region = Region.fromRegionCoords(worldDir, regionPos);
                    WorldUtils.logger.info("in region: r.{}.{}.mca", regionPos.chunkXPos, regionPos.chunkZPos);

                    for (ChunkPos chunkPos : regionEntry.getValue())
                    {
                        TileTickRemover tileTickRemover = new TileTickRemover(region, type, namesToRemove);
                        removedTotal += FileUtils.handleChunkInRegion(region, chunkPos, tileTickRemover, simulate);
                        WorldUtils.logger.info("in chunk: {},{} removed in total: {}", chunkPos.chunkXPos, chunkPos.chunkZPos, removedTotal);
                    }
                }
            }
        }

        return removedTotal;
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
