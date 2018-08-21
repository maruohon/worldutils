package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.command.CommandWorldUtils;
import fi.dy.masa.worldutils.data.blockreplacer.BlockReplacerPairs;
import fi.dy.masa.worldutils.data.blockreplacer.BlockReplacerSet;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;
import fi.dy.masa.worldutils.util.BlockData;
import fi.dy.masa.worldutils.util.BlockInfo;
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class BlockTools
{
    public enum LoadedType
    {
        ALL,
        UNLOADED,
        LOADED;
    }

    public static void replaceBlocks(int dimension, String replacement, List<String> blockNames, List<IBlockState> blockStates,
            boolean keepListedBlocks, LoadedType loaded, ICommandSender sender) throws CommandException
    {
        BlockReplacerSet replacer = new BlockReplacerSet(replacement, keepListedBlocks, loaded);
        replacer.addBlocksFromBlockStates(blockStates);
        replacer.addBlocksFromStrings(blockNames);

        if (keepListedBlocks)
        {
            replacer.addBlocksFromBlockStates(Lists.newArrayList(Blocks.AIR.getDefaultState()));
        }

        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, replacer, sender, 50), 1);
    }

    public static void replaceBlocksInPairs(int dimension, List<Pair<String, String>> blockPairs,
            LoadedType loaded, ICommandSender sender) throws CommandException
    {
        BlockReplacerPairs replacer = new BlockReplacerPairs(loaded);
        replacer.addBlockPairs(blockPairs);
        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, replacer, sender, 50), 1);
    }

    public static boolean setBlock(int dimension, BlockPos blockPos, BlockData blockData) throws CommandException
    {
        Region region = getRegion(dimension, blockPos);

        if (region.getRegionFile() == null)
        {
            CommandWorldUtils.throwCommand("worldutils.commands.error.setblock.region_not_found");
        }

        final int chunkX = blockPos.getX() >> 4;
        final int chunkZ = blockPos.getZ() >> 4;
        NBTTagCompound chunkNBT = getChunkNBT(region, chunkX, chunkZ);

        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            if (setBlock(chunkNBT, blockPos, blockData))
            {
                saveChunkNBT(region, chunkX, chunkZ, chunkNBT);

                return true;
            }
        }
        else
        {
            CommandWorldUtils.throwCommand("worldutils.commands.error.setblock.chunk_not_found");
        }

        return false;
    }

    public static boolean inspectBlock(int dimension, BlockPos blockPos, boolean dumpToFile, ICommandSender sender) throws CommandException
    {
        Region region = getRegion(dimension, blockPos);

        if (region.getRegionFile() == null)
        {
            CommandWorldUtils.throwCommand("worldutils.commands.error.setblock.region_not_found");
        }

        final int chunkX = blockPos.getX() >> 4;
        final int chunkZ = blockPos.getZ() >> 4;
        NBTTagCompound chunkNBT = getChunkNBT(region, chunkX, chunkZ);

        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            int modified = getChunkModificationTimestamp(region.getAbsolutePath(), chunkX, chunkZ);

            if (inspectBlock(chunkNBT, blockPos, dumpToFile, sender, modified))
            {
                return true;
            }
        }
        else
        {
            CommandWorldUtils.throwCommand("worldutils.commands.error.setblock.chunk_not_found");
        }

        return false;
    }

    private static int getChunkModificationTimestamp(String regionFilePath, int chunkX, int chunkZ)
    {
        try
        {
            RandomAccessFile raf = new RandomAccessFile(regionFilePath, "r");
            raf.seek((long)(4096 + ((chunkX & 0x1F) + (chunkZ & 0x1F) * 32) * 4));
            int timestamp = raf.readInt();
            raf.close();

            return timestamp;
        }
        catch (Exception e)
        {
            WorldUtils.logger.warn("BlockTools#getChunkModificationTimestamp(): Failed to read chunk" +
                        " modification timestamp from region file '{}'", regionFilePath);
        }

        return 0;
    }

    @Nullable
    private static Region getRegion(int dimension, BlockPos blockPos) throws CommandException
    {
        File worldDir = FileUtils.getWorldSaveLocation(dimension);

        if (worldDir == null)
        {
            CommandWorldUtils.throwCommand("worldutils.commands.error.invaliddimension", Integer.valueOf(dimension));
        }

        return Region.fromRegionCoords(worldDir, blockPos.getX() >> 9, blockPos.getZ() >> 9, false);
    }

    @Nullable
    private static NBTTagCompound getChunkNBT(Region region, int chunkX, int chunkZ)
    {
        NBTTagCompound chunkNBT;
        DataInputStream dataIn = region.getRegionFile().getChunkDataInputStream(chunkX & 0x1F, chunkZ & 0x1F);

        if (dataIn == null)
        {
            WorldUtils.logger.warn("BlockTools#getChunkNBT(): Failed to get chunk data input stream for chunk ({}, {}) from file '{}'",
                    chunkX, chunkZ, region.getAbsolutePath());
            return null;
        }

        try
        {
            chunkNBT = CompressedStreamTools.read(dataIn);
            dataIn.close();

            return chunkNBT;
        }
        catch (IOException e)
        {
            WorldUtils.logger.warn("BlockTools#getChunkNBT(): Failed to read chunk NBT data for chunk ({}, {}) from file '{}'",
                    chunkX, chunkZ, region.getAbsolutePath(), e);
        }

        return null;
    }

    private static void saveChunkNBT(Region region, int chunkX, int chunkZ, NBTTagCompound chunkNBT)
    {
        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            try
            {
                DataOutputStream dataOut = region.getRegionFile().getChunkDataOutputStream(chunkX & 0x1F, chunkZ & 0x1F);

                if (dataOut == null)
                {
                    WorldUtils.logger.warn("BlockTools#saveChunkNBT(): Failed to get chunk data output stream for chunk [{}, {}] in region file '{}'",
                            chunkX, chunkZ, region.getAbsolutePath());
                    return;
                }

                CompressedStreamTools.write(chunkNBT, dataOut);
                dataOut.close();
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("BlockTools#saveChunkNBT(): Failed to write chunk data for chunk [{}, {}] in region file '{}' ({})",
                        chunkX, chunkZ, region.getAbsolutePath(), e.getMessage());
            }
        }
    }

    private static boolean setBlock(NBTTagCompound chunkNBT, BlockPos pos, BlockData blockData) throws CommandException
    {
        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");
            NBTTagList sectionsList = level.getTagList("Sections", Constants.NBT.TAG_COMPOUND);
            final int yPos = pos.getY() >> 4;

            if (sectionsList.tagCount() > yPos)
            {
                final int stateId = blockData.getBlockStateId();
                NBTTagCompound sectionTag = sectionsList.getCompoundTagAt(yPos);
                byte[] blockArray = sectionTag.getByteArray("Blocks");
                NibbleArray metaNibble = new NibbleArray(sectionTag.getByteArray("Data"));
                NibbleArray addNibble = null;
                final byte blockId = (byte) (stateId & 0xFF);
                final byte add =  (byte) ((stateId >>>  8) & 0xF);
                final byte meta = (byte) ((stateId >>> 12) & 0xF);
                final int x = pos.getX() & 0xF;
                final int y = pos.getY() & 0xF;
                final int z = pos.getZ() & 0xF;

                blockArray[y << 8 | z << 4 | x] = blockId;
                metaNibble.set(x, y, z, meta);

                if (sectionTag.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY))
                {
                    addNibble = new NibbleArray(sectionTag.getByteArray("Add"));
                    addNibble.set(x, y, z, add);
                }
                // No existing Add array, but one is needed because of the new block
                else if (add != 0)
                {
                    addNibble = new NibbleArray();
                    addNibble.set(x, y, z, add);
                    sectionTag.setByteArray("Add", addNibble.getData());
                }

                removeTileEntityAndTileTick(level, pos);

                return true;
            }
            else
            {
                Integer sec = Integer.valueOf(sectionsList.tagCount());
                Integer maxY = sec * 16 - 1;
                CommandWorldUtils.throwCommand("worldutils.commands.error.chunk_section_doesnt_exist", sec, maxY);
            }
        }

        return false;
    }

    private static boolean inspectBlock(NBTTagCompound chunkNBT, BlockPos pos, boolean dumpToFile,
            ICommandSender sender, int modified) throws CommandException
    {
        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");
            NBTTagList sectionsList = level.getTagList("Sections", Constants.NBT.TAG_COMPOUND);
            final int yPos = pos.getY() >> 4;

            if (sectionsList.tagCount() > yPos)
            {
                NBTTagCompound sectionTag = sectionsList.getCompoundTagAt(yPos);
                byte[] blockArray = sectionTag.getByteArray("Blocks");
                NibbleArray metaNibble = new NibbleArray(sectionTag.getByteArray("Data"));

                final int x = pos.getX() & 0xF;
                final int y = pos.getY() & 0xF;
                final int z = pos.getZ() & 0xF;

                final int meta = metaNibble.get(x, y, z);
                int blockId = ((int) blockArray[y << 8 | z << 4 | x]) & 0xFF;

                if (sectionTag.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY))
                {
                    NibbleArray addNibble = new NibbleArray(sectionTag.getByteArray("Add"));
                    blockId |= addNibble.get(x, y, z) << 8;
                }

                NBTTagCompound tileNBT = getTileNBT(level, pos);
                BlockInfo.outputBlockInfo(blockId, meta, tileNBT, dumpToFile, sender, modified);

                return true;
            }
            else
            {
                Integer sec = Integer.valueOf(sectionsList.tagCount());
                Integer maxY = sec * 16 - 1;
                CommandWorldUtils.throwCommand("worldutils.commands.error.chunk_section_doesnt_exist", sec, maxY);
            }
        }

        return false;
    }

    @Nullable
    private static NBTTagCompound getTileNBT(NBTTagCompound level, BlockPos pos)
    {
        NBTTagList list = level.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);

        if (list != null)
        {
            final int size = list.tagCount();
            final int x = pos.getX();
            final int y = pos.getY();
            final int z = pos.getZ();

            for (int i = 0; i < size; i++)
            {
                NBTTagCompound tag = list.getCompoundTagAt(i);

                if (tag.getInteger("x") == x && tag.getInteger("y") == y && tag.getInteger("z") == z)
                {
                    return list.getCompoundTagAt(i);
                }
            }
        }

        return null;
    }

    private static void removeTileEntityAndTileTick(NBTTagCompound level, BlockPos pos)
    {
        NBTTagList list = level.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);

        if (list != null)
        {
            removeTileEntry(list, pos);
        }

        list = level.getTagList("TileTicks", Constants.NBT.TAG_COMPOUND);

        if (list != null)
        {
            removeTileEntry(list, pos);
        }
    }

    private static void removeTileEntry(NBTTagList list, BlockPos pos)
    {
        final int size = list.tagCount();
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();

        for (int i = 0; i < size; i++)
        {
            NBTTagCompound tag = list.getCompoundTagAt(i);

            if (tag.getInteger("x") == x && tag.getInteger("y") == y && tag.getInteger("z") == z)
            {
                list.removeTag(i);
                break;
            }
        }
    }
}
