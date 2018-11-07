package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.blockreplacer.BlockIDRemapper;
import fi.dy.masa.worldutils.event.tasks.TaskRegionDirectoryProcessor;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.util.FileUtils;

public class SubCommandBlockRemapper extends SubCommand
{

    public SubCommandBlockRemapper(CommandWorldUtils baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "blockremapper";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.runhelpforallcommands", this.getUsageStringCommon() + " help");
    }

    @Override
    public void printFullHelp(ICommandSender sender, String[] args)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " from <old world> to <new level.dat>"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " stoptask"));
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if (args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "from", "stoptask");
        }
        else if (args.length == 3)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "to");
        }
        else if (args.length == 2)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, this.getFileNames(true));
        }
        else if (args.length == 4)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, this.getFileNames(false));
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1 || args[0].equals("help"))
        {
            this.printFullHelp(sender, args);
            return;
        }

        String cmd = args[0];
        args = CommandWorldUtils.dropFirstStrings(args, 1);

        if (args.length == 3 && cmd.equals("from") && args[1].equals("to"))
        {
            File dirWorlds = new File(new File(WorldUtils.configDirPath), "worlds");
            File levelNew = new File(dirWorlds, args[2]);
            File dirOldWorld = new File(dirWorlds, args[0]);

            if (dirOldWorld.exists() && dirOldWorld.isDirectory() &&
                levelNew.exists() && levelNew.isFile() && levelNew.canRead())
            {
                BlockIDRemapper remapper = new BlockIDRemapper();

                if (remapper.parseBlockRegistries(new File(dirOldWorld, "level.dat"), levelNew, sender))
                {
                    this.sendMessage(sender, "worldutils.commands.blockremapper.start");
                    TaskScheduler.getInstance().scheduleTask(new TaskRegionDirectoryProcessor(new File(dirOldWorld, "region"), remapper, sender, 50), 1);
                    return;
                }
            }

            throwCommand("worldutils.commands.error.blockremapper.failed_to_read_worlds", dirOldWorld.getAbsolutePath(), levelNew.getAbsolutePath());
        }
        else if (cmd.equals("stoptask"))
        {
            if (TaskScheduler.getInstance().removeTask(TaskRegionDirectoryProcessor.class))
            {
                this.sendMessage(sender, "worldutils.commands.info.taskstopped");
            }
            else
            {
                throwCommand("worldutils.commands.error.notaskfound");
            }
        }
        else
        {
            this.printFullHelp(sender, args);
        }
    }

    private List<String> getFileNames(boolean directories)
    {
        File dir = new File(new File(WorldUtils.configDirPath), "worlds");

        if (dir.isDirectory())
        {
            String[] names = dir.list(directories ? FileUtils.FILTER_DIRECTORIES : FileUtils.FILTER_FILES);
            return Arrays.asList(names);
        }

        return Collections.emptyList();
    }
}
