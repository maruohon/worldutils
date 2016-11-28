package fi.dy.masa.worldutils.data;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

public interface IChunkDataHandler
{
    /**
     * Handle processing of the raw Chunk NBT data.
     * @param chunkPos The current Chunk position this method is called on
     * @param chunkNBT The raw Chunk NBT data to process
     * @param simulate If true, only simulate what would happen
     * @return the number of operations that was done. The meaning is implementor-specific, <b>BUT</b>
     * a value > 0 means that the Chunk NBT data will get written back to the RegionFile, if simulate is false.
     */
    public int processChunkData(ChunkPos chunkPos, NBTTagCompound chunkNBT, boolean simulate);
}
