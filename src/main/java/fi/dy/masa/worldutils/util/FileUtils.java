package fi.dy.masa.worldutils.util;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.chunk.storage.RegionFileCache;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.IChunkDataHandler;
import fi.dy.masa.worldutils.data.IWorldDataHandler;

public class FileUtils
{
    public static final FilenameFilter ANVIL_REGION_FILE_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            //return name.startsWith("r.") && name.endsWith(".mca");
            try
            {
                return Pattern.matches("r\\.-?[0-9]+\\.-?[0-9]+\\.mca", name);
            }
            catch (PatternSyntaxException e)
            {
                WorldUtils.logger.error("Failed to regex match a region file '{}'", name);
                e.printStackTrace();
                return false;
            }
        }
    };

    public static class Region
    {
        private final String regionName;
        private final File file;
        private final RegionFile regionFile;

        private Region(File worldDir, int regionX, int regionZ, boolean create)
        {
            this.file = new File(new File(worldDir, "region"), "r." + regionX + "." + regionZ + ".mca");
            this.regionName = this.file.getName();

            if (create)
            {
                this.regionFile = RegionFileCache.createOrLoadRegionFile(worldDir, regionX << 5, regionZ << 5);
            }
            else
            {
                this.regionFile = RegionFileCache.getRegionFileIfExists(worldDir, regionX << 5, regionZ << 5);
            }
        }

        public static Region fromRegionFile(File regionFile)
        {
            ChunkPos regionPos = getRegionPos(regionFile);

            if (regionPos != null)
            {
                return fromRegionCoords(regionFile.getParentFile().getParentFile(), regionPos.x, regionPos.z);
            }

            return null;
        }

        public static Region fromRegionCoords(File worldDir, ChunkPos regionPos)
        {
            return fromRegionCoords(worldDir, regionPos.x, regionPos.z);
        }

        public static Region fromRegionCoords(File worldDir, int regionX, int regionZ)
        {
            return fromRegionCoords(worldDir, regionX, regionZ, true);
        }

        public static Region fromRegionCoords(File worldDir, int regionX, int regionZ, boolean create)
        {
            return new Region(worldDir, regionX, regionZ, create);
        }

        public String getName()
        {
            return this.regionName;
        }

        public String getFileName()
        {
            return this.file.getAbsolutePath();
        }

        @Nullable
        public RegionFile getRegionFile()
        {
            return this.regionFile;
        }

        public static ChunkPos getRegionPos(File regionFile)
        {
            String name = regionFile.getName();

            if (ANVIL_REGION_FILE_FILTER.accept(regionFile.getParentFile(), name))
            {
                try
                {
                    Pattern pattern = Pattern.compile("^r\\.(-?[0-9]+)\\.(-?[0-9]+)\\.mca$");
                    Matcher matcher = pattern.matcher(name);

                    if (matcher.matches())
                    {
                        int x = Integer.valueOf(matcher.group(1));
                        int z = Integer.valueOf(matcher.group(2));

                        return new ChunkPos(x, z);
                    }
                }
                catch (Exception e)
                {
                    WorldUtils.logger.error("getRegionPos(): Failed to regex match a region file '{}'", name);
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    public static void worldDataProcessor(int dimension, IWorldDataHandler worldDataHandler, ICommandSender sender, boolean simulate)
    {
        World world = DimensionManager.getWorld(dimension);
        ChunkProviderServer provider = null;

        if (world != null && world.getChunkProvider() instanceof ChunkProviderServer)
        {
            provider = (ChunkProviderServer) world.getChunkProvider();
        }

        File regionDir = getRegionDirectory(dimension);

        if (regionDir != null && regionDir.exists() && regionDir.isDirectory())
        {
            worldDataHandler.setChunkProvider(provider);

            for (File regionFile : regionDir.listFiles(ANVIL_REGION_FILE_FILTER))
            {
                regionProcessor(regionFile, worldDataHandler, simulate);
            }

            worldDataHandler.finish(sender, simulate);
        }
        else
        {
            WorldUtils.logger.warn("Dimension {} could not be loaded or does not exist!", dimension);
            sender.sendMessage(new TextComponentTranslation("worldutils.commands.error.invaliddimension", Integer.valueOf(dimension)));
        }
    }

    private static void regionProcessor(File regionFile, IWorldDataHandler worldDataHandler, boolean simulate)
    {
        Region region = Region.fromRegionFile(regionFile);

        if (region == null)
        {
            WorldUtils.logger.warn("regionProcessor(): Failed to get region data for region '{}'", regionFile.getName());
            return;
        }

        if (worldDataHandler.processRegion(region, simulate) == 0)
        {
            for (int chunkZ = 0; chunkZ < 32; chunkZ++)
            {
                for (int chunkX = 0; chunkX < 32; chunkX++)
                {
                    if (region.getRegionFile().isChunkSaved(chunkX, chunkZ))
                    {
                        worldDataHandler.processChunk(region, chunkX, chunkZ, simulate);
                    }
                }
            }
        }
    }

    public static int handleChunkInRegion(Region region, ChunkPos chunkPos, IChunkDataHandler chunkDataHandler, boolean simulate)
    {
        int count = 0;
        int chunkX = chunkPos.x & 0x1F;
        int chunkZ = chunkPos.z & 0x1F;
        RegionFile regionFile = region.getRegionFile();

        if (regionFile.isChunkSaved(chunkX, chunkZ) == false)
        {
            WorldUtils.logger.warn("handleChunkInRegion(): Chunk ({}, {}) was not found in region '{}'",
                    chunkPos.x, chunkPos.z, region.getName());
            return 0;
        }

        DataInputStream data = regionFile.getChunkDataInputStream(chunkX, chunkZ);

        if (data == null)
        {
            WorldUtils.logger.warn("handleChunkInRegion(): Failed to read chunk data for chunk ({}, {}) from region '{}'",
                    chunkPos.x, chunkPos.z, region.getName());

            return 0;
        }

        try
        {
            NBTTagCompound chunkNBT = CompressedStreamTools.read(data);
            data.close();

            count = chunkDataHandler.processChunkData(chunkPos, chunkNBT, simulate);

            if (count > 0 && simulate == false)
            {
                DataOutputStream dataOut = regionFile.getChunkDataOutputStream(chunkX, chunkZ);
                CompressedStreamTools.write(chunkNBT, dataOut);
                dataOut.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return count;
    }

    public static File dumpDataToFile(String fileNameBase, List<String> lines)
    {
        File outFile = null;
        File cfgDir = new File(WorldUtils.configDirPath);

        if (cfgDir.exists() == false)
        {
            try
            {
                cfgDir.mkdirs();
            }
            catch (Exception e)
            {
                WorldUtils.logger.error("dumpDataToFile(): Failed to create the configuration directory.", e);
                return null;
            }

        }

        String fileNameBaseWithDate = fileNameBase + "_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date(System.currentTimeMillis()));
        String fileName = fileNameBaseWithDate + ".txt";
        outFile = new File(cfgDir, fileName);
        int postFix = 1;

        while (outFile.exists())
        {
            fileName = fileNameBaseWithDate + "_" + postFix + ".txt";
            outFile = new File(cfgDir, fileName);
            postFix++;
        }

        try
        {
            outFile.createNewFile();
        }
        catch (IOException e)
        {
            WorldUtils.logger.error("dumpDataToFile(): Failed to create data dump file '" + fileName + "'", e);
            return null;
        }

        try
        {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));

            for (int i = 0; i < lines.size(); ++i)
            {
                writer.write(lines.get(i));
                writer.newLine();
            }

            writer.close();
        }
        catch (IOException e)
        {
            WorldUtils.logger.error("dumpDataToFile(): Exception while writing data dump to file '" + fileName + "'", e);
        }

        return outFile;
    }

    @Nullable
    public static File getWorldSaveLocation(int dimension)
    {
        File dir = DimensionManager.getCurrentSaveRootDirectory();
        World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(dimension);

        if (world != null)
        {
            if (world.provider.getSaveFolder() != null)
            {
                return new File(dir, world.provider.getSaveFolder());
            }
            else if (world.provider.getDimension() == 0)
            {
                return dir;
            }
        }

        return null;
    }

    @Nullable
    public static File getRegionDirectory(int dimension)
    {
        File worldDir = getWorldSaveLocation(dimension);

        if (worldDir != null)
        {
            return new File(worldDir, "region");
        }

        return null;
    }
}
