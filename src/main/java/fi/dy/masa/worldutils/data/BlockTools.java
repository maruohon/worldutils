package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldutils.WorldUtils;
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

    public void replaceBlocks(int dimension, String replacement, Collection<String> toReplace, boolean loadedChunks, ICommandSender sender)
    {
        File worldDir = FileUtils.getWorldSaveLocation(dimension);
        File regionDir = new File(worldDir, "region");

        if (regionDir.exists() && regionDir.isDirectory())
        {
            BlockReplacer replacer = new BlockReplacer(replacement, toReplace, loadedChunks);
            replacer.init();
            FileUtils.worldDataProcessor(dimension, replacer, sender, false);
        }
    }

    private class BlockReplacer implements IWorldDataHandler
    {
        private ChunkProviderServer provider;
        private boolean loadedChunks;
        private int regionCount;
        private int chunkCountUnloaded;
        private int chunkCountLoaded;
        private int replaceCountUnloaded;
        private int replaceCountLoaded;
        private final Set<BlockData> blocksToReplace = new HashSet<BlockData>();
        private boolean[] blocksToReplaceLookup = new boolean[1 << 16];
        private BlockData replacementBlock;
        private IBlockState replacementBlockState;
        private int replacementBlockStateId;
        private boolean validState;

        private BlockReplacer(String replacement, Collection<String> toReplace)
        {
            this(replacement, toReplace, false);
        }

        private BlockReplacer(String replacement, Collection<String> toReplace, boolean loadedChunks)
        {
            this.loadedChunks = loadedChunks;
            this.setReplacementBlock(replacement);
            this.setBlocksToReplace(toReplace);
            this.validState = this.replacementBlock != null && this.blocksToReplace.isEmpty() == false;
        }

        public void setReplacementBlock(String replacement)
        {
            this.replacementBlock = BlockData.parseBlockTypeFromString(replacement);

            if (this.replacementBlock == null)
            {
                WorldUtils.logger.warn("Failed to parse block from string '{}'", replacement);
            }

            this.replacementBlockStateId = this.replacementBlock.getBlockStateId();
            this.replacementBlockState = Block.getStateById(this.replacementBlockStateId);
        }

        public void setBlocksToReplace(Collection<String> toReplace)
        {
            this.blocksToReplace.clear();

            for (String str : toReplace)
            {
                BlockData data = BlockData.parseBlockTypeFromString(str);

                if (data != null)
                {
                    this.blocksToReplace.add(data);

                    for (int id : data.getBlockStateIds())
                    {
                        this.blocksToReplaceLookup[id] = true;
                    }
                }
                else
                {
                    WorldUtils.logger.warn("Failed to parse block from string '{}'", str);
                }
            }
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
                    if (this.loadedChunks)
                    {
                        count = this.processLoadedChunk(chunkPos.chunkXPos, chunkPos.chunkZPos);
                        this.replaceCountLoaded += count;
                        this.chunkCountLoaded++;
                    }
                }
                else
                {
                    count = this.processUnloadedChunk(region, chunkNBT, chunkX, chunkZ);
                }
            }

            return count;
        }

        private int processUnloadedChunk(Region region, NBTTagCompound chunkNBT, int chunkX, int chunkZ)
        {
            boolean chunkDirty = false;
            int count = 0;
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");
            NBTTagList sectionsList = level.getTagList("Sections", Constants.NBT.TAG_COMPOUND);

            for (int sec = 0; sec < sectionsList.tagCount(); sec++)
            {
                int countLast = count;
                NBTTagCompound sectionTag = sectionsList.getCompoundTagAt(sec);
                byte[] blockArray = sectionTag.getByteArray("Blocks");
                int[] blockStateIds = new int[4096];
                boolean[] replacedPositions = new boolean[4096];
                NibbleArray metaNibble = new NibbleArray(sectionTag.getByteArray("Data"));
                boolean needsAdd = false;

                if (sectionTag.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY))
                {
                    NibbleArray nibbleArray = new NibbleArray(sectionTag.getByteArray("Add"));

                    for (int i = 0; i < 4096; i++)
                    {
                        int id = (((metaNibble.getFromIndex(i) & 0xF) << 12) | ((nibbleArray.getFromIndex(i) & 0xF) << 8) | blockArray[i] & 0xFF) & 0xFFFF;

                        if (this.blocksToReplaceLookup[id])
                        {
                            blockStateIds[i] = this.replacementBlockStateId;
                            replacedPositions[i] = true;
                            count++;
                        }
                        else
                        {
                            blockStateIds[i] = id;
                        }

                        needsAdd |= (blockStateIds[i] & 0xF00) != 0;
                    }
                }
                // These are completely separate cases so that they are possibly a little bit faster
                // because the don't do Add array checking on each iteration
                else
                {
                    for (int i = 0; i < 4096; i++)
                    {
                        int id = (((metaNibble.getFromIndex(i) & 0xF) << 12) | blockArray[i] & 0xFF) & 0xFFFF;

                        if (this.blocksToReplaceLookup[id])
                        {
                            blockStateIds[i] = this.replacementBlockStateId;
                            replacedPositions[i] = true;
                            count++;
                        }
                        else
                        {
                            blockStateIds[i] = id;
                        }

                        needsAdd |= (blockStateIds[i] & 0xF00) != 0;
                    }
                }

                // Replaced something, remove the corresponding TileEntities and TileTicks
                // and then write the data back to the chunk NBT
                if (count != countLast) // sectionDirty
                {
                    this.removeTileEntitiesAndTileTicks(level, sectionTag.getInteger("Y"), replacedPositions);

                    byte[] metaArray = new byte[2048];

                    if (needsAdd)
                    {
                        byte[] addArray = new byte[2048];

                        for (int i = 0, bi = 0; i < 2048; i++, bi += 2)
                        {
                            blockArray[bi    ] = (byte) (blockStateIds[bi    ] & 0xFF);
                            blockArray[bi + 1] = (byte) (blockStateIds[bi + 1] & 0xFF);
                            metaArray[i] = (byte) (((blockStateIds[bi] >> 12) & 0x0F) | ((blockStateIds[bi + 1] >> 8) & 0xF0));
                            addArray[i]  = (byte) (((blockStateIds[bi] >>  8) & 0x0F) | ((blockStateIds[bi + 1] >> 4) & 0xF0));
                        }

                        sectionTag.setByteArray("Blocks", blockArray);
                        sectionTag.setByteArray("Data",   metaArray);
                        sectionTag.setByteArray("Add",    addArray);
                    }
                    else
                    {
                        for (int i = 0, bi = 0; i < 2048; i++, bi += 2)
                        {
                            blockArray[bi    ] = (byte) (blockStateIds[bi    ] & 0xFF);
                            blockArray[bi + 1] = (byte) (blockStateIds[bi + 1] & 0xFF);
                            metaArray[i] = (byte) (((blockStateIds[bi] >> 12) & 0x0F) | ((blockStateIds[bi + 1] >> 8) & 0xF0));
                        }

                        sectionTag.setByteArray("Blocks", blockArray);
                        sectionTag.setByteArray("Data",   metaArray);
                    }

                    chunkDirty = true;
                    countLast = count;
                }
            }

            if (chunkDirty)
            {
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

            this.chunkCountUnloaded++;
            this.replaceCountUnloaded += count;

            return count;
        }

        private void removeTileEntitiesAndTileTicks(NBTTagCompound level, int sectionY, boolean[] replacedPositions)
        {
            NBTTagList listTE = level.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);

            if (listTE != null)
            {
                this.removeTileEntry(listTE, sectionY, replacedPositions);
            }

            NBTTagList listTT = level.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);

            if (listTT != null)
            {
                this.removeTileEntry(listTT, sectionY, replacedPositions);
            }
        }

        private void removeTileEntry(NBTTagList list, int sectionY, boolean[] replacedPositions)
        {
            int size = list.tagCount();

            for (int i = 0; i < size; i++)
            {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                int y = tag.getInteger("y");

                if (y >= (sectionY * 16) && y < (sectionY * 16 + 16))
                {
                    int x = tag.getInteger("x");
                    int z = tag.getInteger("z");
                    int pos = ((y & 0xF) << 8) | ((z & 0xF) << 4) | (x & 0xF);

                    if (replacedPositions[pos])
                    {
                        list.removeTag(i);
                        i--;
                        size--;
                    }
                }
            }
        }

        private int processLoadedChunk(int chunkX, int chunkZ)
        {
            Chunk chunk = this.provider.getLoadedChunk(chunkX, chunkZ);
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
                        if (this.blocksToReplaceLookup[Block.getStateId(chunk.getBlockState(x, y, z))])
                        {
                            chunk.setBlockState(new BlockPos(x, y, z), this.replacementBlockState);
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

            sender.sendMessage(new TextComponentTranslation("worldutils.commands.blockprune.finished",
                    Integer.valueOf(this.replaceCountUnloaded), Integer.valueOf(this.chunkCountUnloaded),
                    Integer.valueOf(this.replaceCountLoaded), Integer.valueOf(this.chunkCountLoaded),
                    Integer.valueOf(this.regionCount)));
        }
    }
}
