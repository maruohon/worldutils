package fi.dy.masa.worldtools.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.RegionFile;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import fi.dy.masa.worldtools.WorldTools;
import fi.dy.masa.worldtools.data.IChunkDataHandler;
import fi.dy.masa.worldtools.data.IWorldDataHandler;

public class FileUtils
{
    private static final FilenameFilter ANVIL_REGION_FILE_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.startsWith("r.") && name.endsWith(".mca");
        }
    };

    public static class Region
    {
        private final String regionName;
        private final File regionFile;
        private final RegionFile region;

        public Region(File regionDir, ChunkPos regionPos)
        {
            this(new File(regionDir, "r." + regionPos.chunkXPos + "." + regionPos.chunkZPos + ".mca"));
        }

        public Region(File regionFile)
        {
            this.regionName = regionFile.getName();
            this.regionFile = regionFile;
            this.region = new RegionFile(this.regionFile);
        }

        public String getName()
        {
            return this.regionName;
        }

        public RegionFile getRegionFile()
        {
            return this.region;
        }
    }

    public static void worldDataProcessor(int dimension, IWorldDataHandler worldDataHandler, ICommandSender sender)
    {
        World world = DimensionManager.getWorld(dimension);
        ChunkProviderServer provider = null;

        if (world != null && world.getChunkProvider() instanceof ChunkProviderServer)
        {
            provider = (ChunkProviderServer) world.getChunkProvider();
        }

        File worldSaveLocation = FileUtils.getWorldSaveLocation(dimension);
        File regionDir = new File(worldSaveLocation, "region");

        if (regionDir.exists() && regionDir.isDirectory())
        {
            worldDataHandler.setChunkProvider(provider);

            for (File regionFile : regionDir.listFiles(ANVIL_REGION_FILE_FILTER))
            {
                regionProcessor(regionFile, worldDataHandler);
            }
        }

        worldDataHandler.finish(sender);
    }

    private static void regionProcessor(File regionFile, IWorldDataHandler worldDataHandler)
    {
        try
        {
            Region region = new Region(regionFile);

            if (worldDataHandler.processRegion(region) == 0)
            {
                for (int chunkZ = 0; chunkZ < 32; chunkZ++)
                {
                    for (int chunkX = 0; chunkX < 32; chunkX++)
                    {
                        if (region.getRegionFile().isChunkSaved(chunkX, chunkZ))
                        {
                            worldDataHandler.processChunk(region, chunkX, chunkZ);
                        }
                    }
                }
            }

            region.getRegionFile().close();
        }
        catch (IOException e)
        {
            WorldTools.logger.warn("Exception while processing region '{}'", regionFile.getName());
            e.printStackTrace();
        }
    }

    public static int handleChunkInRegion(Region region, ChunkPos chunkPos, IChunkDataHandler chunkDataHandler, boolean simulate)
    {
        int count = 0;
        int chunkX = chunkPos.chunkXPos & 0x1F;
        int chunkZ = chunkPos.chunkZPos & 0x1F;
        RegionFile regionFile = region.getRegionFile();

        if (regionFile.isChunkSaved(chunkX, chunkZ) == false)
        {
            return 0;
        }

        DataInputStream data = regionFile.getChunkDataInputStream(chunkX, chunkZ);

        if (data == null)
        {
            WorldTools.logger.warn("Failed to read chunk data for chunk ({}, {}) from region '{}'",
                    chunkPos.chunkXPos, chunkPos.chunkZPos, region.getName());

            return 0;
        }

        try
        {
            NBTTagCompound chunkNBT = CompressedStreamTools.read(data);
            data.close();

            count = chunkDataHandler.processData(chunkPos, chunkNBT, simulate);

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

        File cfgDir = new File(WorldTools.configDirPath);
        if (cfgDir.exists() == false)
        {
            try
            {
                cfgDir.mkdirs();
            }
            catch (Exception e)
            {
                WorldTools.logger.error("dumpDataToFile(): Failed to create the configuration directory.");
                e.printStackTrace();
                return null;
            }

        }

        String fileNameBaseWithDate = fileNameBase + "_" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date(System.currentTimeMillis()));
        String fileName = fileNameBaseWithDate + ".txt";
        outFile = new File(cfgDir, fileName);
        int postFix = 1;

        while (outFile.exists() == true)
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
            WorldTools.logger.error("dumpDataToFile(): Failed to create data dump file '" + fileName + "'");
            e.printStackTrace();
            return null;
        }

        try
        {
            for (int i = 0; i < lines.size(); ++i)
            {
                org.apache.commons.io.FileUtils.writeStringToFile(outFile, lines.get(i) + System.getProperty("line.separator"), true);
            }
        }
        catch (IOException e)
        {
            WorldTools.logger.error("dumpDataToFile(): Exception while writing data dump to file '" + fileName + "'");
            e.printStackTrace();
        }

        return outFile;
    }

    public static File getWorldSaveLocation(int dimension)
    {
        World world = DimensionManager.getWorld(dimension);
        File dir = DimensionManager.getCurrentSaveRootDirectory();

        if (world != null && world.provider.getSaveFolder() != null)
        {
            dir = new File(dir, world.provider.getSaveFolder());
        }

        return dir;
    }
}
