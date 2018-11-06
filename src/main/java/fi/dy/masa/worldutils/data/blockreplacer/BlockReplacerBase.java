package fi.dy.masa.worldutils.data.blockreplacer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
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
import fi.dy.masa.worldutils.data.BlockTools.LoadedType;
import fi.dy.masa.worldutils.data.IWorldDataHandler;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public abstract class BlockReplacerBase implements IWorldDataHandler
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
    public void init(int dimension)
    {
        this.regionCount = 0;
        this.chunkCountUnloaded = 0;
        this.chunkCountLoaded = 0;
        this.replaceCountUnloaded = 0;
        this.replaceCountLoaded = 0;
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
        if (this.validState == false)
        {
            return 0;
        }

        NBTTagCompound chunkNBT = null;
        int count = 0;

        try
        {
            DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

            if (data == null)
            {
                WorldUtils.logger.warn("BlockReplacerBase#processChunk(): Failed to get chunk data input stream for chunk [{}, {}] from file '{}'",
                        chunkX, chunkZ, region.getAbsolutePath());
                return 0;
            }

            chunkNBT = CompressedStreamTools.read(data);
            data.close();
        }
        catch (Exception e)
        {
            WorldUtils.logger.warn("BlockReplacerBase#processChunk(): Failed to read chunk NBT data for chunk [{}, {}] from file '{}'",
                    chunkX, chunkZ, region.getAbsolutePath(), e);
            return 0;
        }

        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");
            ChunkPos chunkPos = new ChunkPos(level.getInteger("xPos"), level.getInteger("zPos"));

            // This needs to use absolute Chunk coordinates
            if (this.provider != null && this.provider.chunkExists(chunkPos.x, chunkPos.z))
            {
                if (this.loadedChunks == LoadedType.ALL || this.loadedChunks == LoadedType.LOADED)
                {
                    count = this.processLoadedChunk(chunkPos.x, chunkPos.z);
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
                else
                {
                    // Make sure to remove a possible previously existing Add array, or it would shuffle the final block IDs
                    sectionTag.removeTag("Add");
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

            try
            {
                DataOutputStream data = region.getRegionFile().getChunkDataOutputStream(chunkX, chunkZ);

                if (data == null)
                {
                    WorldUtils.logger.warn("BlockReplacerBase#processChunk(): Failed to get chunk data output stream for chunk [{}, {}] in region file '{}'",
                            chunkX, chunkZ, region.getAbsolutePath());
                    return count;
                }

                CompressedStreamTools.write(chunkNBT, data);
                data.close();
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("BlockReplacerBase#processChunk(): Failed to write chunk data for chunk [{}, {}] in region file '{}' ({})",
                        chunkX, chunkZ, region.getAbsolutePath(), e.getMessage());
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
