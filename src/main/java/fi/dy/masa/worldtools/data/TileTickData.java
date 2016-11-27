package fi.dy.masa.worldtools.data;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class TileTickData implements Comparable<TileTickData>
{
    public final ChunkPos chunk;
    public final BlockPos pos;
    public final String blockId;
    public final ResourceLocation resource;
    public final int delay;
    public final int priority;

    public TileTickData(ChunkPos chunkPos, BlockPos pos, ResourceLocation blockResource, String blockId, int delay, int priority)
    {
        this.chunk = chunkPos;
        this.pos = pos;
        this.resource = blockResource;
        this.blockId = blockId;
        this.delay = delay;
        this.priority = priority;
    }

    @Override
    public int compareTo(TileTickData other)
    {
        int tx = this.pos.getX() >> 4;
        int tz = this.pos.getZ() >> 4;
        int ox = other.pos.getX() >> 4;
        int oz = other.pos.getZ() >> 4;

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
            if (this.pos.getX() > other.pos.getX() || this.pos.getZ() > other.pos.getZ())
            {
                return 1;
            }
            else if (this.pos.getX() < other.pos.getX() || this.pos.getZ() < other.pos.getZ())
            {
                return -1;
            }
            else
            {
                if      (this.pos.getY() > other.pos.getY()) { return 1; }
                else if (this.pos.getY() < other.pos.getY()) { return -1; }
                else { return 0; }
            }
        }
    }
}
