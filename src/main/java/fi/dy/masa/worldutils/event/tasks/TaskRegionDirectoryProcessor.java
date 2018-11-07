package fi.dy.masa.worldutils.event.tasks;

import java.io.File;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.chunk.storage.RegionFileCache;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.command.SubCommand;
import fi.dy.masa.worldutils.data.IWorldDataHandler;
import fi.dy.masa.worldutils.event.TickHandler;
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class TaskRegionDirectoryProcessor implements ITask
{
    private final IWorldDataHandler worldHandler;
    private final ICommandSender commandSender;
    private final File[] regionFiles;
    private File currentRegionFile;
    private Region currentRegion;
    private State state = State.REGION;
    private final int maxTickTime;
    private int regionIndex = 0;
    private int chunkIndex = 0;
    private int regionCount = 1;
    private int chunkCount = 0;
    private int tickCount = 0;

    public TaskRegionDirectoryProcessor(File regionDir, IWorldDataHandler handler, ICommandSender sender, int maxTickTime) throws CommandException
    {
        if (TaskScheduler.getInstance().hasTasks())
        {
            SubCommand.throwCommand("worldutils.commands.error.taskalreadyrunning");
        }

        this.worldHandler = handler;
        this.commandSender = sender;
        this.maxTickTime = maxTickTime;

        if (regionDir != null && regionDir.exists() && regionDir.isDirectory())
        {
            this.regionFiles = regionDir.listFiles(FileUtils.ANVIL_REGION_FILE_FILTER);
        }
        else
        {
            this.regionFiles = null;
            String path = regionDir.getPath();
            WorldUtils.logger.warn("Region file directory '{}' does not exist!", path);
            SubCommand.throwCommand("Region file directory '" + path + "' does not exist!");
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

    protected IWorldDataHandler getWorldHandler()
    {
        return this.worldHandler;
    }

    protected void setWorldHandlerChunkProvider()
    {
    }

    @Override
    public boolean execute()
    {
        this.setWorldHandlerChunkProvider();
        this.tickCount++;

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

        this.printStatusMessage();

        return false;
    }

    protected boolean checkTickTime()
    {
        long timeCurrent = System.currentTimeMillis();

        if ((timeCurrent - TickHandler.instance().getTickStartTime()) >= this.maxTickTime)
        {
            this.printStatusMessage();
            return true;
        }

        return false;
    }

    protected void printStatusMessage()
    {
        // Status message every 10 seconds
        if ((this.tickCount % 200) == 0)
        {
            WorldUtils.logger.info("{}: Handled {} chunks in {} region files...",
                    this.getClass().getSimpleName(), this.chunkCount, this.regionCount);
        }
    }

    /**
     * Advances the regionIndex and sets the File and Region fields for the new region,
     * and resets the chunkIndex to 0.
     * @return true if there are no more region files and processing should terminate
     */
    protected boolean advanceRegion()
    {
        this.regionIndex++;
        this.state = State.REGION;

        // Still more region files to process, update the internal state for the next region
        if (this.regionIndex < this.regionFiles.length)
        {
            this.currentRegionFile = this.regionFiles[this.regionIndex];
            this.currentRegion = Region.fromRegionFile(this.currentRegionFile);
            this.regionCount++;
            this.chunkIndex = 0;
            return false;
        }
        // All done
        else
        {
            return true;
        }
    }

    @Override
    public void stop()
    {
        this.worldHandler.finish(this.commandSender, false);
        RegionFileCache.clearRegionFileReferences();

        WorldUtils.logger.info("{} exiting, handled {} chunks in {} region files",
                this.getClass().getSimpleName(), this.chunkCount, this.regionCount);
    }

    public enum State
    {
        REGION,
        CHUNKS;
    }
}
