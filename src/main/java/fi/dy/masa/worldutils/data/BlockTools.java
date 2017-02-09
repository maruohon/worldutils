package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;
import fi.dy.masa.worldutils.util.BlockData;
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class BlockTools
{
    private static final BlockTools INSTANCE = new BlockTools();

    private BlockTools()
    {
    }

    public static BlockTools instance()
    {
        return INSTANCE;
    }

    public void replaceBlocks(int dimension, String replacement, List<String> blockNames, List<IBlockState> blockStates,
            boolean keepListedBlocks, LoadedType loaded, ICommandSender sender)
    {
        File regionDir = FileUtils.getRegionDirectory(dimension);

        if (regionDir.exists() && regionDir.isDirectory())
        {
            BlockReplacerSet replacer = new BlockReplacerSet(replacement, keepListedBlocks, loaded);
            replacer.addBlocksFromBlockStates(blockStates);
            replacer.addBlocksFromStrings(blockNames);

            if (keepListedBlocks)
            {
                replacer.addBlocksFromBlockStates(Lists.newArrayList(Blocks.AIR.getDefaultState()));
            }

            TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, replacer, sender), 1);
        }
    }

    public void replaceBlocksInPairs(int dimension, List<Pair<String, String>> blockPairs, LoadedType loaded, ICommandSender sender)
    {
        File regionDir = FileUtils.getRegionDirectory(dimension);

        if (regionDir.exists() && regionDir.isDirectory())
        {
            BlockReplacerPairs replacer = new BlockReplacerPairs(loaded);
            replacer.addBlockPairs(blockPairs);
            TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, replacer, sender), 1);
        }
    }

    private class BlockReplacerSet extends BlockReplacerBase
    {
        protected final boolean keepListedBlocks;
        protected final BlockData replacementBlockData;

        private BlockReplacerSet(String replacement, boolean keepListedBlocks, LoadedType loaded)
        {
            super(loaded);

            Arrays.fill(this.blocksToReplaceLookup, keepListedBlocks);
            this.keepListedBlocks = keepListedBlocks;
            this.replacementBlockData = BlockData.parseBlockTypeFromString(replacement);

            if (this.replacementBlockData != null && this.replacementBlockData.isValid())
            {
                int id = this.replacementBlockData.getBlockStateId();
                IBlockState state = Block.getStateById(id);
                Arrays.fill(this.replacementBlockStateIds, id);
                Arrays.fill(this.replacementBlockStates, state);
            }
            else
            {
                WorldUtils.logger.warn("Failed to parse block from string '{}'", replacement);

                IBlockState state = Blocks.AIR.getDefaultState();
                int id = Block.getStateId(state);
                Arrays.fill(this.replacementBlockStateIds, id);
                Arrays.fill(this.replacementBlockStates, state);
            }
        }

        public void addBlocksFromBlockStates(List<IBlockState> blockStates)
        {
            boolean replace = this.keepListedBlocks == false;

            for (IBlockState state : blockStates)
            {
                this.blocksToReplaceLookup[Block.getStateId(state)] = replace;
            }

            this.validState |= this.replacementBlockData != null &&
                               this.replacementBlockData.isValid() &&
                               blockStates.isEmpty() == false;
        }

        public void addBlocksFromStrings(List<String> blockEntries)
        {
            boolean replace = this.keepListedBlocks == false;
            boolean hasData = false;

            for (String str : blockEntries)
            {
                BlockData data = BlockData.parseBlockTypeFromString(str);

                if (data != null && data.isValid())
                {
                    hasData = true;

                    for (int id : data.getBlockStateIds())
                    {
                        this.blocksToReplaceLookup[id] = replace;
                    }
                }
                else
                {
                    WorldUtils.logger.warn("Failed to parse block from string '{}'", str);
                }
            }

            this.validState |= this.replacementBlockData != null &&
                               this.replacementBlockData.isValid() && hasData;
        }
    }

    private class BlockReplacerPairs extends BlockReplacerBase
    {
        protected BlockReplacerPairs(LoadedType loaded)
        {
            super(loaded);

            Arrays.fill(this.blocksToReplaceLookup, false);
        }

        public void addBlockPairs(List<Pair<String, String>> blockPairs)
        {
            boolean addedData = false;

            for (Pair<String, String> pair : blockPairs)
            {
                BlockData dataFrom = BlockData.parseBlockTypeFromString(pair.getLeft());
                BlockData dataTo   = BlockData.parseBlockTypeFromString(pair.getRight());

                if (dataFrom != null && dataFrom.isValid() && dataTo != null && dataTo.isValid())
                {
                    int idFrom = dataFrom.getBlockStateId();
                    int idTo = dataTo.getBlockStateId();
                    this.blocksToReplaceLookup[idFrom] = true;
                    this.replacementBlockStateIds[idFrom] = idTo;
                    this.replacementBlockStates[idFrom] = Block.getStateById(idTo);
                    addedData = true;
                }
                else
                {
                    WorldUtils.logger.warn("Failed to parse block from string '{}' or '{}'", pair.getLeft(), pair.getRight());
                }
            }

            this.validState |= addedData;
        }
    }

    private abstract class BlockReplacerBase implements IWorldDataHandler
    {
        protected ChunkProviderServer provider;
        protected final LoadedType loadedChunks;
        protected int regionCount;
        protected int chunkCountUnloaded;
        protected int chunkCountLoaded;
        protected int replaceCountUnloaded;
        protected int replaceCountLoaded;
        protected final boolean[] blocksToReplaceLookup = new boolean[1 << 16];
        protected final IBlockState[] replacementBlockStates = new IBlockState[1 << 16];
        protected final int[] replacementBlockStateIds = new int[1 << 16];
        protected boolean validState;

        protected BlockReplacerBase(LoadedType loaded)
        {
            this.loadedChunks = loaded;
        }

        @Override
        public void init()
        {
            this.regionCount = 0;
            this.chunkCountUnloaded = 0;
            this.chunkCountLoaded = 0;
            this.replaceCountUnloaded = 0;
            this.replaceCountLoaded = 0;
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
            if (this.validState == false)
            {
                return 0;
            }

            NBTTagCompound chunkNBT;
            DataInputStream dataIn = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);
            int count = 0;

            if (dataIn == null)
            {
                WorldUtils.logger.warn("BlockReplacer#processChunk(): Failed to read chunk data for chunk ({}, {}) from file '{}'",
                        chunkX, chunkZ, region.getName());
                return 0;
            }

            try
            {
                chunkNBT = CompressedStreamTools.read(dataIn);
                dataIn.close();
            }
            catch (IOException e)
            {
                WorldUtils.logger.warn("BlockReplacer#processChunk(): Failed to read chunk data for chunk ({}, {}) from file '{}'",
                        chunkX, chunkZ, region.getName(), e);

                return 0;
            }

            if (chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
            {
                NBTTagCompound level = chunkNBT.getCompoundTag("Level");
                ChunkPos chunkPos = new ChunkPos(level.getInteger("xPos"), level.getInteger("zPos"));

                // This needs to use absolute Chunk coordinates
                if (this.provider != null && this.provider.chunkExists(chunkPos.chunkXPos, chunkPos.chunkZPos))
                {
                    if (this.loadedChunks == LoadedType.ALL || this.loadedChunks == LoadedType.LOADED)
                    {
                        count = this.processLoadedChunk(chunkPos.chunkXPos, chunkPos.chunkZPos);
                        this.replaceCountLoaded += count;
                        this.chunkCountLoaded++;
                    }
                }
                else if (this.loadedChunks == LoadedType.ALL || this.loadedChunks == LoadedType.UNLOADED)
                {
                    count = this.processUnloadedChunk(region, chunkNBT, chunkX, chunkZ);
                    this.replaceCountUnloaded += count;
                    this.chunkCountUnloaded++;
                }
            }

            return count;
        }

        private int processUnloadedChunk(Region region, NBTTagCompound chunkNBT, int chunkX, int chunkZ)
        {
            boolean chunkDirty = false;
            int count = 0;
            boolean[] replacedPositions = new boolean[65536];
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");
            NBTTagList sectionsList = level.getTagList("Sections", Constants.NBT.TAG_COMPOUND);

            for (int sec = 0; sec < sectionsList.tagCount(); sec++)
            {
                int countLast = count;
                NBTTagCompound sectionTag = sectionsList.getCompoundTagAt(sec);
                int sectionStart = sectionTag.getInteger("Y") * 4096;
                byte[] blockArray = sectionTag.getByteArray("Blocks");
                int[] blockStateIds = new int[4096];
                NibbleArray metaNibble = new NibbleArray(sectionTag.getByteArray("Data"));

                if (sectionTag.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY))
                {
                    NibbleArray addNibble = new NibbleArray(sectionTag.getByteArray("Add"));

                    for (int i = 0; i < 4096; i++)
                    {
                        int id = (((metaNibble.getFromIndex(i) & 0xF) << 12) | ((addNibble.getFromIndex(i) & 0xF) << 8) | blockArray[i] & 0xFF) & 0xFFFF;

                        if (this.blocksToReplaceLookup[id])
                        {
                            blockStateIds[i] = this.replacementBlockStateIds[id];
                            replacedPositions[sectionStart + i] = true;
                            count++;
                        }
                        else
                        {
                            blockStateIds[i] = id;
                        }
                    }
                }
                // These are completely separate cases so that they are possibly a little bit faster
                // because they don't do the Add array checking on each iteration
                else
                {
                    for (int i = 0; i < 4096; i++)
                    {
                        int id = (((metaNibble.getFromIndex(i) & 0xF) << 12) | blockArray[i] & 0xFF) & 0xFFFF;

                        if (this.blocksToReplaceLookup[id])
                        {
                            blockStateIds[i] = this.replacementBlockStateIds[id];
                            replacedPositions[sectionStart + i] = true;
                            count++;
                        }
                        else
                        {
                            blockStateIds[i] = id;
                        }
                    }
                }

                // Write the block data back to the chunk NBT
                if (count != countLast) // sectionDirty
                {
                    boolean needsAdd = false;
                    byte[] metaArray = new byte[2048];
                    byte[] addArray = new byte[2048];

                    for (int i = 0, bi = 0; i < 2048; i++, bi += 2)
                    {
                        blockArray[bi    ] = (byte) (blockStateIds[bi    ] & 0xFF);
                        blockArray[bi + 1] = (byte) (blockStateIds[bi + 1] & 0xFF);
                        metaArray[i]       = (byte) (((blockStateIds[bi] >> 12) & 0x0F) | ((blockStateIds[bi + 1] >> 8) & 0xF0));
                        addArray[i]        = (byte) (((blockStateIds[bi] >>  8) & 0x0F) | ((blockStateIds[bi + 1] >> 4) & 0xF0));
                        needsAdd |= addArray[i] != 0;
                    }

                    sectionTag.setByteArray("Blocks", blockArray);
                    sectionTag.setByteArray("Data", metaArray);

                    if (needsAdd)
                    {
                        sectionTag.setByteArray("Add", addArray);
                    }

                    chunkDirty = true;
                    countLast = count;
                }
            }

            // Replaced something, remove the corresponding TileEntities and TileTicks
            if (chunkDirty)
            {
                this.removeTileEntitiesAndTileTicks(level, replacedPositions);
                // Re-check the lighting if blocks were replaced. This still doesn't actually force a proper
                // re-light calculation though... There doesn't seem to be any way to do that via the chunk NBT
                // data, without actually re-calculating all the lighting here and updating the light arrays...
                //level.removeTag("LightPopulated");

                DataOutputStream dataOut = region.getRegionFile().getChunkDataOutputStream(chunkX, chunkZ);

                if (dataOut == null)
                {
                    WorldUtils.logger.warn("BlockReplacer#processChunk(): Failed to write chunk data for chunk ({}, {}) from file '{}'",
                            chunkX, chunkZ, region.getName());
                    return 0;
                }

                try
                {
                    CompressedStreamTools.write(chunkNBT, dataOut);
                    dataOut.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            return count;
        }

        private void removeTileEntitiesAndTileTicks(NBTTagCompound level, boolean[] replacedPositions)
        {
            NBTTagList list = level.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);

            if (list != null)
            {
                this.removeTileEntry(list, replacedPositions);
            }

            list = level.getTagList("TileTicks", Constants.NBT.TAG_COMPOUND);

            if (list != null)
            {
                this.removeTileEntry(list, replacedPositions);
            }
        }

        private void removeTileEntry(NBTTagList list, boolean[] replacedPositions)
        {
            int size = list.tagCount();

            for (int i = 0; i < size; i++)
            {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                int x = tag.getInteger("x");
                int y = tag.getInteger("y");
                int z = tag.getInteger("z");
                int pos = ((y & 0xFF) << 8) | ((z & 0xF) << 4) | (x & 0xF);

                if (replacedPositions[pos])
                {
                    list.removeTag(i);
                    i--;
                    size--;
                }
            }
        }

        protected int processLoadedChunk(int chunkX, int chunkZ)
        {
            Chunk chunk = this.provider.getLoadedChunk(chunkX, chunkZ);
            World world = chunk.getWorld();
            MutableBlockPos pos = new MutableBlockPos(0, 0, 0);
            int maxY = chunk.getTopFilledSegment() + 15;
            int minX = chunkX << 4;
            int minZ = chunkZ << 4;
            int maxX = minX + 15;
            int maxZ = minZ + 15;
            int count = 0;

            for (int y = maxY; y >= 0; y--)
            {
                for (int z = minZ; z <= maxZ; z++)
                {
                    for (int x = minX; x <= maxX; x++)
                    {
                        pos.setPos(x, y, z);
                        int id = Block.getStateId(world.getBlockState(pos));

                        if (this.blocksToReplaceLookup[id])
                        {
                            world.setBlockState(pos, this.replacementBlockStates[id], 2);
                            count++;
                        }
                    }
                }
            }

            return count;
        }

        @Override
        public void finish(ICommandSender sender, boolean simulate)
        {
            WorldUtils.logger.info("Replaced a total of {} blocks in {} unloaded chunks and {} blocks in {} loaded chunks, touching {} region files",
                    this.replaceCountUnloaded, this.chunkCountUnloaded, this.replaceCountLoaded, this.chunkCountLoaded, this.regionCount);

            sender.sendMessage(new TextComponentTranslation("worldutils.commands.blockreplace.execute.finished",
                    Integer.valueOf(this.replaceCountUnloaded), Integer.valueOf(this.chunkCountUnloaded),
                    Integer.valueOf(this.replaceCountLoaded), Integer.valueOf(this.chunkCountLoaded),
                    Integer.valueOf(this.regionCount)));
        }
    }

    public enum LoadedType
    {
        ALL,
        UNLOADED,
        LOADED;
    }
}
