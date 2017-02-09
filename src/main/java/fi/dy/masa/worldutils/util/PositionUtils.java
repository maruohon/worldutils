package fi.dy.masa.worldutils.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static <T, U extends Collection<T>> Map<ChunkPos, U> getMapForDataPerChunk(Map<ChunkPos, Map<ChunkPos, U>> mapRegions, ChunkPos regionPos)
    {
        Map<ChunkPos, U> mapChunks = mapRegions.get(regionPos);

        if (mapChunks == null)
        {
            mapChunks = new HashMap<ChunkPos, U>();
            mapRegions.put(regionPos, mapChunks);
        }

        return mapChunks;
    }

    private static <T> List<T> getListForDataInChunk(Map<ChunkPos, List<T>> mapChunks, ChunkPos chunkPos)
    {
        List<T> data = mapChunks.get(chunkPos);

        if (data == null)
        {
            data = new ArrayList<T>();
            mapChunks.put(chunkPos, data);
        }

        return data;
    }

    private static <T> Set<T> getSetForDataInChunk(Map<ChunkPos, Set<T>> mapChunks, ChunkPos chunkPos)
    {
        Set<T> data = mapChunks.get(chunkPos);

        if (data == null)
        {
            data = new HashSet<T>();
            mapChunks.put(chunkPos, data);
        }

        return data;
    }

    public static <T> void addDataForChunkInLists(Map<ChunkPos, Map<ChunkPos, List<T>>> dataByRegion, ChunkPos chunkPos, T data)
    {
        ChunkPos regionPos = new ChunkPos(chunkPos.chunkXPos >> 5, chunkPos.chunkZPos >> 5);
        getListForDataInChunk(getMapForDataPerChunk(dataByRegion, regionPos), chunkPos).add(data);
    }

    public static <T> void addDataForChunkInSets(Map<ChunkPos, Map<ChunkPos, Set<T>>> dataByRegion, ChunkPos chunkPos, T data)
    {
        ChunkPos regionPos = new ChunkPos(chunkPos.chunkXPos >> 5, chunkPos.chunkZPos >> 5);
        getSetForDataInChunk(getMapForDataPerChunk(dataByRegion, regionPos), chunkPos).add(data);
    }
}
