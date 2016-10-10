package fi.dy.masa.worldtools.util;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
import net.minecraft.server.management.PlayerChunkMap;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToAccessFieldException;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindFieldException;
import net.minecraftforge.fml.relauncher.ReflectionHelper.UnableToFindMethodException;
import fi.dy.masa.worldtools.WorldTools;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ChunkChanger
{
    private static final ChunkChanger INSTANCE = new ChunkChanger();
    private final Map<File, AnvilChunkLoader> chunkLoaders = new HashMap<File, AnvilChunkLoader>();
    private final TIntObjectHashMap<Map<String, Map<Long, String>>> changedChunks  = new TIntObjectHashMap<Map<String, Map<Long, String>>>();
    private final TIntObjectHashMap<Map<String, Map<Long, String>>> importedBiomes = new TIntObjectHashMap<Map<String, Map<Long, String>>>();
    private boolean dirty;

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
        BIOME_IMPORT;

        public static ChangeType fromId(int id)
        {
            return values()[id % values().length];
        }
    }

    private ChunkChanger()
    {
    }

    public static ChunkChanger instance()
    {
        return INSTANCE;
    }

    private File getWorldSaveLocation(WorldServer world)
    {
        File baseDir = world.getSaveHandler().getWorldDirectory();

        if (world.provider.getSaveFolder() != null)
        {
            baseDir = new File(baseDir, world.provider.getSaveFolder());
        }

        return baseDir;
    }

    private File getAlternateWorldsBaseDirectory(WorldServer world)
    {
        return new File(this.getWorldSaveLocation(world), "alternate_worlds");
    }

    private File getWorldLocation(WorldServer world, String worldName)
    {
        return new File(this.getAlternateWorldsBaseDirectory(world), worldName);
    }

    public int getNumberOfAlternateWorlds(World world)
    {
        if (world instanceof WorldServer)
        {
            File dir = this.getAlternateWorldsBaseDirectory((WorldServer) world);
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
        }

        return 0;
    }

    public String getWorldName(World world, int index)
    {
        if (world instanceof WorldServer && index < this.getNumberOfAlternateWorlds(world))
        {
            File dir = this.getAlternateWorldsBaseDirectory((WorldServer) world);
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
        }

        return "";
    }

    private AnvilChunkLoader getChunkLoader(File worldDir)
    {
        AnvilChunkLoader loader = this.chunkLoaders.get(worldDir);

        if (loader == null)
        {
            loader = new AnvilChunkLoader(worldDir, FMLCommonHandler.instance().getMinecraftServerInstance().getDataFixer());
            this.chunkLoaders.put(worldDir, loader);
        }

        return loader;
    }

    private AnvilChunkLoader getChunkLoader(WorldServer world, String worldName)
    {
        File worldDir = this.getWorldLocation(world, worldName);

        if (worldDir.exists() && worldDir.isDirectory())
        {
            return this.getChunkLoader(worldDir);
        }

        return null;
    }

    private void unloadChunk(WorldServer world, ChunkPos pos)
    {
        int chunkX = pos.chunkXPos;
        int chunkZ = pos.chunkZPos;
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);

        chunk.onChunkUnload();

        try
        {
            ChunkProviderServer provider = world.getChunkProvider();

            Method method = ReflectionHelper.findMethod(ChunkProviderServer.class, provider, new String[] { "func_73242_b", "saveChunkData" }, Chunk.class);
            method.invoke(provider, chunk);

            method = ReflectionHelper.findMethod(ChunkProviderServer.class, provider, new String[] { "func_73243_a", "saveChunkExtraData" }, Chunk.class);
            method.invoke(provider, chunk);
        }
        catch (UnableToFindMethodException e)
        {
            WorldTools.logger.warn("Exception while trying to unload chunk ({}, {}) - UnableToFindMethodException", chunk.xPosition, chunk.zPosition);
        }
        catch (InvocationTargetException e)
        {
            WorldTools.logger.warn("Exception while trying to unload chunk ({}, {}) - InvocationTargetException", chunk.xPosition, chunk.zPosition);
        }
        catch (IllegalAccessException e)
        {
            WorldTools.logger.warn("Exception while trying to unload chunk ({}, {}) - IllegalAccessException", chunk.xPosition, chunk.zPosition);
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
            Field field = ReflectionHelper.findField(World.class, "field_147483_b", "tileEntitiesToBeRemoved");
            @SuppressWarnings("unchecked")
            List<TileEntity> toRemove = (List<TileEntity>) field.get(world);
            toRemove.removeAll(unloadTileEntities);

            field = ReflectionHelper.findField(World.class, "field_72997_g", "unloadedEntityList");
            @SuppressWarnings("unchecked")
            List<Entity> toRemoveEnt = (List<Entity>) field.get(world);
            toRemoveEnt.removeAll(unloadEntities);
        }
        catch (UnableToFindFieldException e)
        {
            WorldTools.logger.warn("Exception while trying to unload chunk ({}, {}) - UnableToFindFieldException", chunk.xPosition, chunk.zPosition);
        }
        catch (IllegalAccessException e)
        {
            WorldTools.logger.warn("Exception while trying to unload chunk ({}, {}) - IllegalAccessException", chunk.xPosition, chunk.zPosition);
        }

        // For some reason getPendingBlockUpdates() checks for "x >= minX && x < maxX" and not x <= maxX...
        StructureBoundingBox bb = new StructureBoundingBox(chunkX << 4, 0, chunkZ << 4, (chunkX << 4) + 17, 255, (chunkZ << 4) + 17);
        world.getPendingBlockUpdates(bb, true); // Remove pending block updates from the unloaded chunk area

        world.getChunkProvider().id2ChunkMap.remove(ChunkPos.asLong(chunk.xPosition, chunk.zPosition));
    }

    private Chunk loadChunk(WorldServer world, ChunkPos pos, String worldName)
    {
        AnvilChunkLoader loader = this.getChunkLoader(world, worldName);

        if (loader != null)
        {
            try
            {
                Object[] data = loader.loadChunk__Async(world, pos.chunkXPos, pos.chunkZPos);

                if (data != null)
                {
                    this.unloadChunk(world, pos);

                    final Chunk chunk = (Chunk) data[0];
                    NBTTagCompound nbt = (NBTTagCompound) data[1];
                    loader.loadEntities(world, nbt.getCompoundTag("Level"), chunk);

                    chunk.setLastSaveTime(world.getTotalWorldTime());
                    world.getChunkProvider().id2ChunkMap.put(ChunkPos.asLong(pos.chunkXPos, pos.chunkZPos), chunk);
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
                            double x = chunk.xPosition << 4;
                            double z = chunk.zPosition << 4;
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
                WorldTools.logger.warn("Exception while trying to load chunk ({}, {}) - IOException", pos.chunkXPos, pos.chunkZPos);
            }
        }

        return null;
    }

    private void updatePlayerChunkMap(WorldServer world, ChunkPos pos, Chunk chunk)
    {
        PlayerChunkMap map = world.getPlayerChunkMap();
        PlayerChunkMapEntry entry = map.getEntry(pos.chunkXPos, pos.chunkZPos);

        if (entry != null)
        {
            try
            {
                ReflectionHelper.setPrivateValue(PlayerChunkMapEntry.class, entry, chunk, "field_187286_f", "chunk");
            }
            catch (UnableToAccessFieldException e)
            {
                WorldTools.logger.warn("Failed to update PlayerChunkMapEntry for chunk ({}, {})", pos.chunkXPos, pos.chunkZPos);
            }
        }
    }

    private void sendChunkToWatchers(final WorldServer world, final Chunk chunk)
    {
        for (EntityPlayerMP player : world.getPlayers(EntityPlayerMP.class, new Predicate<EntityPlayerMP>() {
            @Override
            public boolean apply(EntityPlayerMP playerIn)
            {
                return world.getPlayerChunkMap().isPlayerWatchingChunk(playerIn, chunk.xPosition, chunk.zPosition);
            }
            }))
        {
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
        else
        {
            mainMap = this.getChangeMap(this.importedBiomes, world);
        }

        Map<Long, String> map = mainMap.get(user);

        if (map == null)
        {
            map = new HashMap<Long, String>();
            mainMap.put(user, map);
        }

        Long posLong = ChunkPos.asLong(pos.chunkXPos, pos.chunkZPos);
        map.put(posLong, worldName);

        // A chunk change also removes an earlier biome import for that chunk
        if (type == ChangeType.CHUNK_CHANGE)
        {
            map = this.getChangeMap(this.importedBiomes, world).get(user);

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
            File saveDir = this.getAlternateWorldsBaseDirectory((WorldServer) world);

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
                    this.readFromNBT(this.getChangeMap(this.changedChunks, world), nbt, "changes");
                    this.readFromNBT(this.getChangeMap(this.importedBiomes, world), nbt, "biomes");
                }
            }
        }
        catch (Exception e)
        {
            WorldTools.logger.warn("Failed to read exported changed chunks data from file!");
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
            File saveDir = this.getAlternateWorldsBaseDirectory((WorldServer) world);

            if (saveDir == null || (saveDir.exists() == false && saveDir.mkdirs() == false))
            {
                WorldTools.logger.warn("Failed to create the directory '" + saveDir + "'");
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
            WorldTools.logger.warn("Failed to write exported changed chunks data to file!");
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

        WorldTools.logger.info("ChunkChanger: Read {} stored chunk modifications of type '{}' from file for user {}", chunks.size(), tagName, user);
    }

    public NBTTagCompound writeToNBT(World world, String user)
    {
        NBTTagCompound nbt = new NBTTagCompound();
        final int dim = world.provider.getDimension();
        this.writeToNBT(this.getChangeMap(this.changedChunks, world), dim, user, nbt, "changes");
        this.writeToNBT(this.getChangeMap(this.importedBiomes, world), dim, user, nbt, "biomes");

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
            IChunkLoader loader = this.getChunkLoader(world, worldName);

            try
            {
                DataInputStream stream = RegionFileCache.getChunkInputStream(((AnvilChunkLoader) loader).chunkSaveLocation, pos.chunkXPos, pos.chunkZPos);

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
                                Chunk chunkCurrent = world.getChunkFromChunkCoords(pos.chunkXPos, pos.chunkZPos);
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
                WorldTools.logger.warn("Failed to read chunk data for chunk ({}, {})", pos.chunkXPos, pos.chunkZPos);
            }
        }
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
                this.sendChunkToWatchers(world, world.getChunkFromChunkCoords(pos.chunkXPos, pos.chunkZPos));
                this.addChangedChunkLocation(world, pos, ChangeType.CHUNK_CHANGE, worldName, user);
            }
        }
    }

    public void clearChangedChunksForUser(World world, String user)
    {
        this.getChangeMap(this.changedChunks, world).remove(user);
    }
}

