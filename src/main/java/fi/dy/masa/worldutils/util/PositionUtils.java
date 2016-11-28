package fi.dy.masa.worldutils.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

public class PositionUtils
{
    public static ChunkPos getChunkPosFromBlockPos(BlockPos pos)
    {
        return new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
    }

    public static ChunkPos getLookedAtChunk(World world, EntityPlayer player, int maxDistance)
    {
        RayTraceResult trace = EntityUtils.getRayTraceFromPlayer(world, player, true, maxDistance);

        if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK)
        {
            return getChunkPosFromBlockPos(trace.getBlockPos());
        }

        return null;
    }
}
