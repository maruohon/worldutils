package fi.dy.masa.worldutils.data;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class TileTickData implements Comparable<TileTickData>
{
    public final ChunkPos chunkPos;
    public final BlockPos blockPos;
    public final String blockId;
    public final ResourceLocation resource;
    public final int delay;
    public final int priority;

    public TileTickData(ChunkPos chunkPos, BlockPos pos, ResourceLocation blockResource, String blockId, int delay, int priority)
    {
        this.chunkPos = chunkPos;
        this.blockPos = pos;
        this.resource = blockResource;
        this.blockId = blockId;
        this.delay = delay;
        this.priority = priority;
    }

    @Override
    public int compareTo(TileTickData other)
    {
        int tx = this.blockPos.getX() >> 4;
        int tz = this.blockPos.getZ() >> 4;
        int ox = other.blockPos.getX() >> 4;
        int oz = other.blockPos.getZ() >> 4;

        if (tx > ox || tz > oz)
        {
            return 1;
        }
        else if (tx < ox || tz < oz)
        {
            return -1;
        }
        else
        {
            if (this.blockPos.getX() > other.blockPos.getX() || this.blockPos.getZ() > other.blockPos.getZ())
            {
                return 1;
            }
            else if (this.blockPos.getX() < other.blockPos.getX() || this.blockPos.getZ() < other.blockPos.getZ())
            {
                return -1;
            }
            else
            {
                if      (this.blockPos.getY() > other.blockPos.getY()) { return 1; }
                else if (this.blockPos.getY() < other.blockPos.getY()) { return -1; }
                else { return 0; }
            }
        }
    }
}
