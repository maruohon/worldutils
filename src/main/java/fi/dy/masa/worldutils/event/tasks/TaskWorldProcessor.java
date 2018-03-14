package fi.dy.masa.worldutils.event.tasks;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import fi.dy.masa.worldutils.data.IWorldDataHandler;
import fi.dy.masa.worldutils.util.FileUtils;

public class TaskWorldProcessor extends TaskRegionDirectoryProcessor
{
    private final int dimension;

    public TaskWorldProcessor(int dimension, IWorldDataHandler handler, ICommandSender sender, int maxTickTime) throws CommandException
    {
        super(FileUtils.getRegionDirectory(dimension), handler, sender, maxTickTime);

        this.dimension = dimension;
    }

    @Override
    protected void setWorldHandlerChunkProvider()
    {
        WorldServer world = DimensionManager.getWorld(this.dimension);

        if (world != null)
        {
            this.getWorldHandler().setChunkProvider(world.getChunkProvider());
        }
        else
        {
            this.getWorldHandler().setChunkProvider(null);
        }
    }
}
