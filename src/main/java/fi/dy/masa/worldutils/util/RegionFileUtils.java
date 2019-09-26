package fi.dy.masa.worldutils.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldutils.WorldUtils;

public class RegionFileUtils
{
    public static List<String> findPossibleChunksAligned(File fileIn)
    {
        List<String> output = new ArrayList<>();

        if (fileIn.exists() && fileIn.isFile() && fileIn.canRead())
        {
            try
            {
                RandomAccessFile dataFile = new RandomAccessFile(fileIn, "r");
                final long size = dataFile.length();

                if (size < 4096L)
                {
                    WorldUtils.logger.warn("Error: Region file '{}' is too small to contain any chunk data", fileIn.getName());
                    dataFile.close();
                    return output;
                }

                byte[] arr = new byte[4];
                int count = 0;

                for (long pos = 0; pos < size - 8; pos += 4096L)
                {
                    dataFile.seek(pos);
                    int byteCount = dataFile.readInt();

                    if (dataFile.read(arr) < arr.length)
                    {
                        WorldUtils.logger.warn(String.format("Failed to read data from region file '%s' at position 0x%08X (%6d)", fileIn.getName(), pos, pos));
                        break;
                    }

                    // Index 4 is the compression type, 0x02 = zlib.
                    // Index 5 and 6 are the zlib stream header, it seems to be
                    // 0x789C for the deflate streams written by Java/Minecraft.
                    if (arr[0] == 0x02 && arr[1] == (byte) 0x78 && arr[2] == (byte) 0x9C)
                    {
                        byteCount -= 1; // subtract the compression type byte (arr[0] above)

                        if (byteCount <= 1044480) // 255 * 4096
                        {
                            output.add(String.format("Possible chunk header at 0x%08X (%6d), stream length: %6d bytes", pos, pos, byteCount));
                            ++count;
                        }
                        else
                        {
                            output.add(String.format("INVALID possible chunk header at 0x%08X (%6d), stream length: %6d bytes", pos, pos, byteCount));
                        }
                    }
                }

                output.add(String.format("Found %d potential chunk data streams", count));

                dataFile.close();
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("Error: Exception trying to read region file '{}'", fileIn.getName());
            }
        }
        else
        {
            WorldUtils.logger.warn("Error: File '{}' can't be read", fileIn.getName());
        }

        //return new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(abyte))));

        return output;
    }

    public static int tryRestoreChunksFromRegion(File fileIn, File fileOut)
    {
        int count = 0;

        if (fileIn.exists() && fileIn.isFile() && fileIn.canRead())
        {
            if (fileOut.exists())
            {
                WorldUtils.logger.warn("Error: Output file '{}' already exists, aborting...", fileOut.getName());
                return 0;
            }

            try
            {
                RandomAccessFile dataFile = new RandomAccessFile(fileIn, "r");
                final long size = dataFile.length();

                if (size < 4096L)
                {
                    WorldUtils.logger.warn("Error: Region file '{}' is too small to contain any chunk data", fileIn.getName());
                    dataFile.close();
                    return 0;
                }

                byte[] arr = new byte[4];
                RegionFile regionFile = new RegionFile(fileOut);

                for (long pos = 0; pos < size - 8; pos += 4096L)
                {
                    dataFile.seek(pos);
                    int byteCount = dataFile.readInt();

                    if (dataFile.read(arr) < arr.length)
                    {
                        WorldUtils.logger.warn(String.format("Failed to read data from region file '%s' at position 0x%08X (%6d)", fileIn.getName(), pos, pos));
                        break;
                    }

                    // Index 4 is the compression type, 0x02 = zlib.
                    // Index 5 and 6 are the zlib stream header, it seems to be
                    // 0x789C for the deflate streams written by Java/Minecraft.
                    if (arr[0] == 0x02 && arr[1] == (byte) 0x78 && arr[2] == (byte) 0x9C)
                    {
                        byteCount -= 1; // subtract the compression type byte (arr[0] above)

                        if (byteCount <= 1044480) // 255 * 4096
                        {
                            WorldUtils.logger.info(String.format("Trying to restore possible chunk at 0x%08X (%6d), stream length: %6d bytes", pos, pos, byteCount));

                            byte[] chunkDataRaw = new byte[byteCount];
                            dataFile.seek(pos + 5);
                            dataFile.read(chunkDataRaw);

                            try
                            {
                                DataInputStream is = new DataInputStream(new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(chunkDataRaw))));
                                NBTTagCompound nbt = CompressedStreamTools.read(is);
                                is.close();

                                if (nbt != null && nbt.hasKey("Level", Constants.NBT.TAG_COMPOUND))
                                {
                                    NBTTagCompound levelTag = nbt.getCompoundTag("Level");

                                    if (levelTag.hasKey("xPos", Constants.NBT.TAG_INT) && levelTag.hasKey("zPos", Constants.NBT.TAG_INT))
                                    {
                                        int x = levelTag.getInteger("xPos");
                                        int z = levelTag.getInteger("zPos");

                                        WorldUtils.logger.info(String.format("Successfully read a Chunk NBT tag for chunk [%5d, %5d] at 0x%08X (%6d), stream length: %6d bytes, writing it to a new region file '%s'", x, z, pos, pos, byteCount, fileOut.getName()));

                                        try
                                        {
                                            DataOutputStream os = regionFile.getChunkDataOutputStream(x & 0x1F, z & 0x1F);
                                            CompressedStreamTools.write(nbt, os);
                                            os.close();
                                            ++count;
                                        }
                                        catch (Exception e)
                                        {
                                            WorldUtils.logger.warn(String.format("Exception while trying to write chunk [%5d, %5d] at 0x%08X (%6d) into the new region file", x, z, pos, pos));
                                        }
                                    }
                                    else
                                    {
                                        WorldUtils.logger.warn(String.format("Chunk NBT tag does not contain the x and z position tags at 0x%08X (%6d), discarding it", pos, pos));
                                    }
                                }
                                else
                                {
                                    WorldUtils.logger.warn(String.format("Failed to read Chunk NBT for chunk at 0x%08X (%6d), discarding it", pos, pos));
                                }
                            }
                            catch (Exception e)
                            {
                                WorldUtils.logger.warn(String.format("Exception while trying to read Chunk from pos 0x%08X (%6d)", pos, pos), e);
                            }
                        }
                        else
                        {
                            WorldUtils.logger.warn(String.format("INVALID possible chunk header at 0x%08X (%6d), stream length: %6d bytes", pos, pos, byteCount));
                        }
                    }
                }

                WorldUtils.logger.info("Restored {} chunks from region '{}' into a newly constructed region '{}'", count, fileIn.getName(), fileOut.getName());

                regionFile.close();
                dataFile.close();
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("Error: Exception trying to read region file '{}'", fileIn.getName());
            }
        }

        return count;
    }
}
