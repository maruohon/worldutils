package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.command.CommandWorldUtils;
import fi.dy.masa.worldutils.util.BlockData;
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class BlockSetter
{
    public static boolean setBlock(int dimension, BlockPos pos, BlockData blockData) throws CommandException
    {
        File worldDir = FileUtils.getWorldSaveLocation(dimension);

        if (worldDir == null)
        {
            CommandWorldUtils.throwCommand("worldutils.commands.error.invaliddimension", Integer.valueOf(dimension));
        }

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        RegionFile regionFile = RegionFileCache.getRegionFileIfExists(worldDir, chunkX, chunkZ);

        if (regionFile == null)
        {
            CommandWorldUtils.throwCommand("worldutils.commands.error.setblock", Integer.valueOf(dimension));
        }

        Region region = Region.fromRegionCoords(worldDir, chunkX >> 5, chunkZ >> 5);
        NBTTagCompound chunkNBT = getChunkNBT(region, chunkX, chunkZ);

        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            if (setBlock(chunkNBT, pos, blockData))
            {
                saveChunkNBT(region, chunkX, chunkZ, chunkNBT);

                return true;
            }
        }

        return false;
    }

    @Nullable
    private static NBTTagCompound getChunkNBT(Region region, int chunkX, int chunkZ)
    {
        NBTTagCompound chunkNBT;
        DataInputStream dataIn = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

        if (dataIn == null)
        {
            WorldUtils.logger.warn("BlockSetter#getChunkNBT(): Failed to get chunk data input stream for chunk ({}, {}) from file '{}'",
                    chunkX, chunkZ, region.getFileName());
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
            WorldUtils.logger.warn("BlockSetter#getChunkNBT(): Failed to read chunk NBT data for chunk ({}, {}) from file '{}'",
                    chunkX, chunkZ, region.getFileName(), e);
        }

        return null;
    }

    private static void saveChunkNBT(Region region, int chunkX, int chunkZ, NBTTagCompound chunkNBT)
    {
        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            DataOutputStream dataOut = region.getRegionFile().getChunkDataOutputStream(chunkX, chunkZ);

            if (dataOut == null)
            {
                WorldUtils.logger.warn("BlockSetter#saveChunkNBT(): Failed to get chunk data output stream for chunk ({}, {}) in file '{}'",
                        chunkX, chunkZ, region.getFileName());
                return;
            }

            try
            {
                CompressedStreamTools.write(chunkNBT, dataOut);
                dataOut.close();
            }
            catch (IOException e)
            {
                WorldUtils.logger.warn("BlockSetter#saveChunkNBT(): Failed to write chunk data for chunk ({}, {}) in file '{}'",
                        chunkX, chunkZ, region.getFileName(), e);
            }
        }
    }

    private static boolean setBlock(NBTTagCompound chunkNBT, BlockPos pos, BlockData blockData)
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

                return true;
            }
        }

        return false;
    }
}
