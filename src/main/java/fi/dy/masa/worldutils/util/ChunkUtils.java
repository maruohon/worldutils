package fi.dy.masa.worldutils.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.google.common.base.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import fi.dy.masa.worldutils.WorldUtils;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ChunkUtils
{
    public static final String TAG_CHANGED_CHUNKS = "chunks_changed";
    public static final String TAG_BIOMES_IMPORTED = "biomes_imported";
    public static final String TAG_BIOMES_SET = "biomes_set";
    private static final ChunkUtils INSTANCE = new ChunkUtils();
    private static MethodHandle methodHandle_ChunkProviderServer_saveChunkData;
    private static MethodHandle methodHandle_ChunkProviderServer_saveChunkExtraData;
    private static Field field_World_tileEntitiesToBeRemoved;
    private static Field field_World_unloadedEntityList;
    private static Field field_PlayerChunkMapEntry_chunk;
    private final Map<File, AnvilChunkLoader> chunkLoaders = new HashMap<File, AnvilChunkLoader>();
    private final TIntObjectHashMap<Map<String, Map<Long, String>>> changedChunks  = new TIntObjectHashMap<Map<String, Map<Long, String>>>();
    private final TIntObjectHashMap<Map<String, Map<Long, String>>> importedBiomes = new TIntObjectHashMap<Map<String, Map<Long, String>>>();
    private final TIntObjectHashMap<Map<String, Map<Long, String>>> setBiomes = new TIntObjectHashMap<Map<String, Map<Long, String>>>();
    private boolean dirty;

    static
    {
        methodHandle_ChunkProviderServer_saveChunkData =
            MethodHandleUtils.getMethodHandleVirtual(ChunkProviderServer.class, "saveChunkData", "func_73242_b", Chunk.class);
        methodHandle_ChunkProviderServer_saveChunkExtraData =
                MethodHandleUtils.getMethodHandleVirtual(ChunkProviderServer.class, "saveChunkExtraData", "func_73243_a", Chunk.class);

        field_World_tileEntitiesToBeRemoved = ReflectionHelper.findField(World.class, "field_147483_b", "tileEntitiesToBeRemoved");
        field_World_unloadedEntityList = ReflectionHelper.findField(World.class, "field_72997_g", "unloadedEntityList");
        field_PlayerChunkMapEntry_chunk = ReflectionHelper.findField(PlayerChunkMapEntry.class, "field_187286_f", "chunk");
    }

    public static class ChunkChanges
    {
        public final ChangeType type;
        public final String worldName;

        public ChunkChanges(ChangeType type, String worldName)
        {
            this.type = type;
            this.worldName = worldName;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            result = prime * result + ((worldName == null) ? 0 : worldName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ChunkChanges other = (ChunkChanges) obj;
            if (type != other.type)
                return false;
            if (worldName == null)
            {
                if (other.worldName != null)
                    return false;
            }
            else if (!worldName.equals(other.worldName))
                return false;
            return true;
        }
    }

    public static enum ChangeType
    {
        CHUNK_CHANGE,
        BIOME_IMPORT,
        BIOME_SET;

        public static ChangeType fromId(int id)
        {
            return values()[id % values().length];
        }
    }

    private ChunkUtils()
    {
    }

    public static ChunkUtils instance()
    {
        return INSTANCE;
    }

    private static File getBaseWorldSaveLocation()
    {
        return DimensionManager.getCurrentSaveRootDirectory();
    }

    private static File getWorldSaveLocation(World world)
    {
        File dir = getBaseWorldSaveLocation();

        if (world.provider.getSaveFolder() != null)
        {
            dir = new File(dir, world.provider.getSaveFolder());
        }

        return dir;
    }

    private static File getAlternateWorldsBaseDirectory()
    {
        return new File(getBaseWorldSaveLocation(), "alternate_worlds");
    }

    private static File getAlternateWorldSaveLocation(World world, String worldName)
    {
        File baseDir = getAlternateWorldsBaseDirectory();

        if (world.provider.getSaveFolder() != null)
        {
            baseDir = new File(baseDir, world.provider.getSaveFolder());
        }

        return new File(baseDir, worldName);
    }

    public static int getNumberOfAlternateWorlds()
    {
        File dir = getAlternateWorldsBaseDirectory();
        String[] names = dir.list();
        int num = 0;

        if (names != null)
        {
            for (String name : names)
            {
                File tmp1 = new File(dir, name);
                File tmp2 = new File(tmp1, "region");

                if (tmp1.isDirectory() && tmp2.isDirectory())
                {
                    num++;
                }
            }

            return num;
        }

        return 0;
    }

    public static String getWorldName(int index)
    {
        File dir = getAlternateWorldsBaseDirectory();
        String[] names = dir.list();

        if (names != null)
        {
            List<String> namesList = new ArrayList<String>();

            for (String name : names)
            {
                File tmp1 = new File(dir, name);
                File tmp2 = new File(tmp1, "region");

                if (tmp1.isDirectory() && tmp2.isDirectory())
                {
                    namesList.add(name);
                }
            }

            Collections.sort(namesList);

            return index < namesList.size() ? namesList.get(index) : "";
        }

        return "";
    }

    public AnvilChunkLoader getChunkLoader(File worldDir)
    {
        AnvilChunkLoader loader = this.chunkLoaders.get(worldDir);

        if (loader == null)
        {
            loader = new AnvilChunkLoader(worldDir, FMLCommonHandler.instance().getMinecraftServerInstance().getDataFixer());
            this.chunkLoaders.put(worldDir, loader);
        }

        return loader;
    }

    public AnvilChunkLoader getChunkLoaderForWorld(World world)
    {
        File worldDir = getWorldSaveLocation(world);

        if (worldDir.exists() && worldDir.isDirectory())
        {
            return this.getChunkLoader(worldDir);
        }

        return null;
    }

    public AnvilChunkLoader getChunkLoaderForAlternateWorld(World world, String alternateWorldName)
    {
        File worldDir = getAlternateWorldSaveLocation(world, alternateWorldName);

        if (worldDir.exists() && worldDir.isDirectory())
        {
            return this.getChunkLoader(worldDir);
        }

        return null;
    }

    private void unloadChunk(WorldServer world, ChunkPos pos)
    {
        int chunkX = pos.x;
        int chunkZ = pos.z;
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);

        chunk.onChunkUnload();

        try
        {
            ChunkProviderServer provider = world.getChunkProvider();
            methodHandle_ChunkProviderServer_saveChunkData.invokeExact(provider, chunk);
            methodHandle_ChunkProviderServer_saveChunkExtraData.invokeExact(provider, chunk);
        }
        catch (Throwable t)
        {
            WorldUtils.logger.warn("Exception while trying to unload chunk ({}, {})", chunk.x, chunk.z, t);
        }

        List<Entity> unloadEntities = new ArrayList<Entity>();

        for (ClassInheritanceMultiMap<Entity> map : chunk.getEntityLists())
        {
            unloadEntities.addAll(map);
        }

        for (int i = 0; i < unloadEntities.size(); i++)
        {
            Entity entity = unloadEntities.get(i);

            if ((entity instanceof EntityPlayer) == false)
            {
                world.loadedEntityList.remove(entity);
                world.onEntityRemoved(entity);
            }
        }

        Collection<TileEntity> unloadTileEntities = chunk.getTileEntityMap().values();
        world.tickableTileEntities.removeAll(unloadTileEntities);
        world.loadedTileEntityList.removeAll(unloadTileEntities);

        try
        {
            @SuppressWarnings("unchecked")
            List<TileEntity> toRemove = (List<TileEntity>) field_World_tileEntitiesToBeRemoved.get(world);
            toRemove.removeAll(unloadTileEntities);

            @SuppressWarnings("unchecked")
            List<Entity> toRemoveEnt = (List<Entity>) field_World_unloadedEntityList.get(world);
            toRemoveEnt.removeAll(unloadEntities);
        }
        catch (Exception e)
        {
            WorldUtils.logger.warn("Exception while trying to unload chunk ({}, {})", chunk.x, chunk.z, e);
        }

        // For some reason getPendingBlockUpdates() checks for "x >= minX && x < maxX" and not x <= maxX...
        StructureBoundingBox bb = new StructureBoundingBox(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 17, 255, (chunkZ << 4) + 17);
        world.getPendingBlockUpdates(bb, true); // Remove pending block updates from the unloaded chunk area

        world.getChunkProvider().id2ChunkMap.remove(ChunkPos.asLong(chunk.x, chunk.z));
    }

    private Chunk loadChunk(WorldServer world, ChunkPos pos, String worldName)
    {
        AnvilChunkLoader loader = this.getChunkLoaderForAlternateWorld(world, worldName);

        if (loader != null)
        {
            try
            {
                Object[] data = loader.loadChunk__Async(world, pos.x, pos.z);

                if (data != null)
                {
                    this.unloadChunk(world, pos);

                    final Chunk chunk = (Chunk) data[0];
                    NBTTagCompound nbt = (NBTTagCompound) data[1];
                    loader.loadEntities(world, nbt.getCompoundTag("Level"), chunk);

                    chunk.setLastSaveTime(world.getTotalWorldTime());
                    world.getChunkProvider().id2ChunkMap.put(ChunkPos.asLong(pos.x, pos.z), chunk);
                    this.updatePlayerChunkMap(world, pos, chunk);

                    List<Entity> unloadEntities = new ArrayList<Entity>();

                    for (ClassInheritanceMultiMap<Entity> map : chunk.getEntityLists())
                    {
                        unloadEntities.addAll(map);
                    }

                    // Remove any entities from the world that have the same UUID
                    // as entities in the new chunk that will get loaded.
                    for (Entity entity : unloadEntities)
                    {
                        Entity match = world.getEntityFromUuid(entity.getUniqueID());

                        if (match != null)
                        {
                            world.removeEntityDangerously(match);
                        }
                    }

                    chunk.onChunkLoad();
                    chunk.setChunkModified();
                    // don't call chunk.populateChunk() because it would probably mess with structures in this kind of usage

                    // Add any player entities into the new chunk that are inside the chunk's area
                    for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, new Predicate<EntityPlayerMP>() {
                        @Override
                        public boolean apply(EntityPlayerMP playerIn)
                        {
                            double x = chunk.x << 4;
                            double z = chunk.z << 4;
                            return playerIn.posX >= x && playerIn.posX < x + 16 && playerIn.posZ >= z && playerIn.posZ < z + 16;
                        }
                        }))
                    {
                        chunk.addEntity(player);
                    }

                    return chunk;
                }
            }
            catch (IOException e)
            {
                WorldUtils.logger.warn("Exception while trying to load chunk ({}, {}) - IOException", pos.x, pos.z);
            }
        }

        return null;
    }

    private void updatePlayerChunkMap(WorldServer world, ChunkPos pos, Chunk chunk)
    {
        PlayerChunkMap map = world.getPlayerChunkMap();
        PlayerChunkMapEntry entry = map.getEntry(pos.x, pos.z);

        if (entry != null)
        {
            try
            {
                field_PlayerChunkMapEntry_chunk.set(entry, chunk);
            }
            catch (Exception e)
            {
                WorldUtils.logger.warn("Failed to update PlayerChunkMapEntry for chunk ({}, {})", pos.x, pos.z, e);
            }
        }
    }

    private void sendChunkToWatchers(final WorldServer world, final Chunk chunk)
    {
        for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, new Predicate<EntityPlayerMP>() {
            @Override
            public boolean apply(EntityPlayerMP playerIn)
            {
                return world.getPlayerChunkMap().isPlayerWatchingChunk(playerIn, chunk.x, chunk.z);
            }
            }))
        {
            player.connection.sendPacket(new SPacketUnloadChunk(chunk.x, chunk.z));
            Packet<?> packet = new SPacketChunkData(chunk, 65535);
            player.connection.sendPacket(packet);
            world.getEntityTracker().sendLeashedEntitiesInChunk(player, chunk);
        }
    }

    private Map<String, Map<Long, String>> getChangeMap(TIntObjectHashMap<Map<String, Map<Long, String>>> mainMap, World world)
    {
        int dim = world.provider.getDimension();
        Map<String, Map<Long, String>> map = mainMap.get(dim);

        if (map == null)
        {
            map = new HashMap<String, Map<Long, String>>();
            mainMap.put(dim, map);
        }

        return map;
    }

    private void addChangedChunkLocation(World world, ChunkPos pos, ChangeType type, String worldName, String user)
    {
        Map<String, Map<Long, String>> mainMap = null;

        if (type == ChangeType.CHUNK_CHANGE)
        {
            mainMap = this.getChangeMap(this.changedChunks, world);
        }
        else if (type == ChangeType.BIOME_IMPORT)
        {
            mainMap = this.getChangeMap(this.importedBiomes, world);
        }
        else
        {
            mainMap = this.getChangeMap(this.setBiomes, world);
        }

        Map<Long, String> map = mainMap.get(user);

        if (map == null)
        {
            map = new HashMap<Long, String>();
            mainMap.put(user, map);
        }

        Long posLong = ChunkPos.asLong(pos.x, pos.z);
        map.put(posLong, worldName);

        // A chunk change also removes an earlier biome import for that chunk
        if (type == ChangeType.CHUNK_CHANGE)
        {
            map = this.getChangeMap(this.importedBiomes, world).get(user);

            if (map != null)
            {
                map.remove(posLong);
            }

            map = this.getChangeMap(this.setBiomes, world).get(user);

            if (map != null)
            {
                map.remove(posLong);
            }
        }

        this.dirty = true;
    }

    public void readFromDisk(World world)
    {
        if ((world instanceof WorldServer) == false)
        {
            return;
        }

        try
        {
            File saveDir = getAlternateWorldsBaseDirectory();

            if (saveDir == null || saveDir.isDirectory() == false)
            {
                return;
            }

            final int dim = world.provider.getDimension();

            String[] files = saveDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name)
                {
                    return name.startsWith("dim" + dim + "_") && name.endsWith("_changed_chunks.nbt");
                }
                
            });

            for (String fileName : files)
            {
                File file = new File(saveDir, fileName);

                if (file.exists() && file.isFile())
                {
                    NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(file));
                    this.readFromNBT(this.getChangeMap(this.changedChunks, world), nbt, TAG_CHANGED_CHUNKS);
                    this.readFromNBT(this.getChangeMap(this.importedBiomes, world), nbt, TAG_BIOMES_IMPORTED);
                    this.readFromNBT(this.getChangeMap(this.setBiomes, world), nbt, TAG_BIOMES_SET);
                }
            }
        }
        catch (Exception e)
        {
            WorldUtils.logger.warn("Failed to read exported changed chunks data from file!");
        }
    }

    public void writeToDisk(World world)
    {
        if (this.dirty == false || (world instanceof WorldServer) == false)
        {
            return;
        }

        try
        {
            File saveDir = getAlternateWorldsBaseDirectory();

            if (saveDir == null || (saveDir.exists() == false && saveDir.mkdirs() == false))
            {
                WorldUtils.logger.warn("Failed to create the directory '" + saveDir + "'");
                return;
            }

            final int dim = world.provider.getDimension();

            for (String user : this.getChangeMap(this.changedChunks, world).keySet())
            {
                String fileName = "dim" + dim + "_" + user + "_changed_chunks.nbt";
                File fileTmp = new File(saveDir, fileName + ".tmp");
                File fileReal = new File(saveDir, fileName);
                CompressedStreamTools.writeCompressed(this.writeToNBT(world, user), new FileOutputStream(fileTmp));

                if (fileReal.exists())
                {
                    fileReal.delete();
                }

                fileTmp.renameTo(fileReal);
            }

            this.dirty = false;
        }
        catch (Exception e)
        {
            WorldUtils.logger.warn("Failed to write exported changed chunks data to file!");
        }
    }

    private void readFromNBT(Map<String, Map<Long, String>> mapIn, NBTTagCompound nbt, String tagName)
    {
        if (nbt == null || StringUtils.isBlank(nbt.getString("user")))
        {
            return;
        }

        Map<Long, String> chunks = new HashMap<Long, String>();
        NBTTagList list = nbt.getTagList(tagName, Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < list.tagCount(); i++)
        {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            String worldName = tag.getString("world");
            int[] arr = tag.getIntArray("chunks");

            for (int j = 0; j < arr.length - 1; j += 2)
            {
                long loc = ((long) arr[j + 1]) << 32 | (long) arr[j];
                chunks.put(loc, worldName);
            }
        }

        String user = nbt.getString("user");
        mapIn.put(user, chunks);

        WorldUtils.logger.info("ChunkChanger: Read {} stored chunk modifications of type '{}' from file for user {}", chunks.size(), tagName, user);
    }

    public NBTTagCompound writeToNBT(World world, String user)
    {
        NBTTagCompound nbt = new NBTTagCompound();
        final int dim = world.provider.getDimension();
        this.writeToNBT(this.getChangeMap(this.changedChunks, world), dim, user, nbt, TAG_CHANGED_CHUNKS);
        this.writeToNBT(this.getChangeMap(this.importedBiomes, world), dim, user, nbt, TAG_BIOMES_IMPORTED);
        this.writeToNBT(this.getChangeMap(this.setBiomes, world), dim, user, nbt, TAG_BIOMES_SET);

        return nbt;
    }

    private NBTTagCompound writeToNBT(Map<String, Map<Long, String>> mapIn, int dimension, String user, NBTTagCompound nbt, String tagName)
    {
        Map<Long, String> modifiedChunks = mapIn.get(user);

        if (modifiedChunks == null)
        {
            return nbt;
        }

        Map<String, List<Long>> changesPerWorld = new HashMap<String, List<Long>>();

        for (Map.Entry<Long, String> entry : modifiedChunks.entrySet())
        {
            String worldName = entry.getValue();
            List<Long> locations = changesPerWorld.get(worldName);

            if (locations == null)
            {
                locations = new ArrayList<Long>();
                changesPerWorld.put(worldName, locations);
            }

            locations.add(entry.getKey());
        }

        NBTTagList list = new NBTTagList();

        for (Map.Entry<String, List<Long>> entry : changesPerWorld.entrySet())
        {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("world", entry.getKey());

            List<Long> chunks = entry.getValue();
            int[] chunksArr = new int[chunks.size() * 2];
            int i = 0;

            for (long chunk : chunks)
            {
                chunksArr[i    ] = (int) (chunk & 0xFFFFFFFF);
                chunksArr[i + 1] = (int) (chunk >>> 32);
                i += 2;
            }

            tag.setIntArray("chunks", chunksArr);
            list.appendTag(tag);
        }

        nbt.setString("user", user);
        nbt.setInteger("dim", dimension);
        nbt.setTag(tagName, list);

        return nbt;
    }

    public void loadBiomesFromAlternateWorld(World worldIn, ChunkPos pos, String worldName, String user)
    {
        if ((worldIn instanceof WorldServer) == false)
        {
            return;
        }

        WorldServer world = (WorldServer) worldIn;

        if (StringUtils.isBlank(worldName) == false)
        {
            AnvilChunkLoader loader = this.getChunkLoaderForAlternateWorld(world, worldName);

            try
            {
                DataInputStream stream = RegionFileCache.getChunkInputStream(loader.chunkSaveLocation, pos.x, pos.z);

                if (stream != null)
                {
                    NBTTagCompound nbt = CompressedStreamTools.read(stream);

                    if (nbt != null && nbt.hasKey("Level", Constants.NBT.TAG_COMPOUND))
                    {
                        NBTTagCompound level = nbt.getCompoundTag("Level");

                        if (level.hasKey("Biomes", Constants.NBT.TAG_BYTE_ARRAY))
                        {
                            byte[] biomes = level.getByteArray("Biomes");

                            if (biomes.length == 256)
                            {
                                Chunk chunkCurrent = world.getChunkFromChunkCoords(pos.x, pos.z);
                                chunkCurrent.setBiomeArray(biomes);
                                chunkCurrent.setChunkModified();
                                this.sendChunkToWatchers(world, chunkCurrent);
                                this.addChangedChunkLocation(world, pos, ChangeType.BIOME_IMPORT, worldName, user);
                            }
                        }
                    }
                }
            }
            catch (IOException e)
            {
                WorldUtils.logger.warn("Failed to read chunk data for chunk ({}, {})", pos.x, pos.z);
            }
        }
    }

    public void setBiome(World worldIn, ChunkPos pos, Biome biome, String user)
    {
        if ((worldIn instanceof WorldServer) == false || biome == null)
        {
            return;
        }

        WorldServer world = (WorldServer) worldIn;
        Chunk chunkCurrent = world.getChunkFromChunkCoords(pos.x, pos.z);
        byte[] biomes = new byte[256];

        Arrays.fill(biomes, (byte) Biome.getIdForBiome(biome));
        chunkCurrent.setBiomeArray(biomes);
        chunkCurrent.setChunkModified();
        this.sendChunkToWatchers(world, chunkCurrent);
        this.addChangedChunkLocation(world, pos, ChangeType.BIOME_SET, "", user);
    }

    public void loadChunkFromAlternateWorld(World worldIn, ChunkPos pos, String worldName, String user)
    {
        if ((worldIn instanceof WorldServer) == false)
        {
            return;
        }

        WorldServer world = (WorldServer) worldIn;

        if (StringUtils.isBlank(worldName) == false)
        {
            Chunk chunk = this.loadChunk(world, pos, worldName);

            if (chunk != null)
            {
                this.sendChunkToWatchers(world, world.getChunkFromChunkCoords(pos.x, pos.z));
                this.addChangedChunkLocation(world, pos, ChangeType.CHUNK_CHANGE, worldName, user);
            }
        }
    }

    public void clearChangedChunksForUser(World world, String user)
    {
        this.getChangeMap(this.changedChunks, world).remove(user);
    }
}

