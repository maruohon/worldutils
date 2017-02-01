package fi.dy.masa.worldutils.event.tasks;

import java.io.File;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.IWorldDataHandler;
import fi.dy.masa.worldutils.event.TickHandler;
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class TaskWorldProcessor implements ITask
{
    private final ICommandSender commandSender;
    private final IWorldDataHandler worldHandler;
    private final File[] regionFiles;
    private File currentRegionFile;
    private Region currentRegion;
    private State state = State.REGION;
    private final int dimension;
    private int regionIndex = 0;
    private int chunkIndex = 0;
    private int regionCount = 1;
    private int chunkCount = 0;

    public TaskWorldProcessor(int dimension, IWorldDataHandler handler, ICommandSender sender)
    {
        this.dimension = dimension;
        this.worldHandler = handler;
        this.commandSender = sender;

        File regionDir = FileUtils.getRegionDirectory(dimension);

        if (regionDir.exists() && regionDir.isDirectory())
        {
            this.regionFiles = regionDir.listFiles(FileUtils.ANVIL_REGION_FILE_FILTER);
        }
        else
        {
            this.regionFiles = null;
        }
    }

    @Override
    public void init()
    {
        this.currentRegionFile = this.regionFiles != null && this.regionFiles.length > 0 ? this.regionFiles[0] : null;
        this.currentRegion = this.currentRegionFile != null ? Region.fromRegionFile(this.currentRegionFile) : null;
    }

    @Override
    public boolean canExecute()
    {
        return this.currentRegion != null;
    }

    @Override
    public boolean execute()
    {
        WorldServer world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(this.dimension);

        if (world == null)
        {
            return true;
        }

        this.worldHandler.setChunkProvider(world.getChunkProvider());

        if (this.state == State.REGION)
        {
            while (true)
            {
                if (this.checkTickTime())
                {
                    return false;
                }

                // A return value of != 0 means that the region was processed by this method
                // and per-chunk processing should not occur.
                if (this.worldHandler.processRegion(this.currentRegion, false) != 0)
                {
                    if (this.advanceRegion())
                    {
                        // Processing finished, terminate the task
                        return true;
                    }
                }
                else
                {
                    this.state = State.CHUNKS;
                    break;
                }
            }
        }

        if (this.state == State.CHUNKS)
        {
            while (this.chunkIndex < 1024)
            {
                if (this.checkTickTime())
                {
                    return false;
                }

                int chunkX = this.chunkIndex & 0x1F;
                int chunkZ = this.chunkIndex >> 5;

                if (this.currentRegion.getRegionFile().isChunkSaved(chunkX, chunkZ))
                {
                    this.worldHandler.processChunk(this.currentRegion, chunkX, chunkZ, false);
                    this.chunkCount++;
                }

                this.chunkIndex++;

                if ((this.chunkIndex & 0x1FF) == 0)
                {
                    WorldUtils.logger.info("TaskWorldProcessor progress: Handled {} chunks in {} region files...", this.chunkCount, this.regionCount);
                }
            }

            if (this.chunkIndex >= 1024)
            {
                if (this.advanceRegion())
                {
                    // Processing finished, terminate the task
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkTickTime()
    {
        long timeCurrent = System.currentTimeMillis();

        if ((timeCurrent - TickHandler.instance().getTickStartTime()) >= 48L)
        {
            return true;
        }

        return false;
    }

    /**
     * Advances the regionIndex and sets the File and Region fields for the new region,
     * and resets the chunkIndex to 0.
     * @return true if there are no more region files and processing should terminate
     */
    private boolean advanceRegion()
    {
        this.regionIndex++;
        this.state = State.REGION;

        if (this.regionIndex < this.regionFiles.length)
        {
            this.currentRegionFile = this.regionFiles[this.regionIndex];
            this.currentRegion = Region.fromRegionFile(this.currentRegionFile);
            this.regionCount++;
            this.chunkIndex = 0;
            return false;
        }
        else
        {
            return true;
        }
    }

    @Override
    public void stop()
    {
        this.worldHandler.finish(this.commandSender, false);
        WorldUtils.logger.info("TaskWorldProcessor exiting, handled {} chunks in {} region files", this.chunkCount, this.regionCount);
    }

    private enum State
    {
        REGION,
        CHUNKS;
    }
}
