package fi.dy.masa.worldutils.command;

import java.io.File;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.worldutils.data.EntityTools;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.util.FileUtils;

public class SubCommandEntities extends SubCommand
{
    public SubCommandEntities(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("list-all");
        this.subSubCommands.add("list-duplicates-all");
        this.subSubCommands.add("list-duplicates-only");
        this.subSubCommands.add("read-all");
        this.subSubCommands.add("remove-duplicate-uuids");
    }

    @Override
    public String getName()
    {
        return "entities";
    }

    @Override
    public void printFullHelp(ICommandSender sender, String[] args)
    {
        this.sendMessage(sender, this.getUsageStringCommon() + " list-all");
        this.sendMessage(sender, this.getUsageStringCommon() + " list-duplicates-all");
        this.sendMessage(sender, this.getUsageStringCommon() + " list-duplicates-only");
        this.sendMessage(sender, this.getUsageStringCommon() + " read-all [dimension id]");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove-duplicate-uuids [dimension id]");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            this.printHelpGeneric(sender);
            return;
        }

        String cmd = args[0];
        args = dropFirstStrings(args, 1);

        if (cmd.equals("help"))
        {
            this.printFullHelp(sender, args);
        }
        else if (cmd.equals("list-all"))
        {
            File file = FileUtils.dumpDataToFile("entities", EntityTools.instance().getAllEntitiesOutput(true));

            if (file != null)
            {
                this.sendMessage(sender, "worldutils.commands.info.outputtofile", file.getName());
            }
        }
        else if (cmd.equals("list-duplicates-all") || cmd.equals("list-duplicates-only"))
        {
            File file;

            if (cmd.equals("list-duplicates-all"))
            {
                file = FileUtils.dumpDataToFile("entity_duplicates_all", EntityTools.instance().getDuplicateEntitiesOutput(true, true));
            }
            else
            {
                file = FileUtils.dumpDataToFile("entity_duplicates_only", EntityTools.instance().getDuplicateEntitiesOutput(false, true));
            }

            if (file != null)
            {
                this.sendMessage(sender, "worldutils.commands.info.outputtofile", file.getName());
            }
        }
        else if (cmd.equals("read-all"))
        {
            if (TaskScheduler.getInstance().hasTasks())
            {
                throwCommand("worldutils.commands.error.taskalreadyrunning");
            }

            this.sendMessage(sender, "worldutils.commands.entities.readall.start");
            int dimension = this.getDimension(cmd, args, sender);
            EntityTools.instance().readEntities(dimension, sender);
        }
        else if (cmd.equals("remove-duplicate-uuids"))
        {
            if (TaskScheduler.getInstance().hasTasks())
            {
                throwCommand("worldutils.commands.error.taskalreadyrunning");
            }

            this.sendMessage(sender, "worldutils.commands.entities.removeallduplicates.start");
            int dimension = this.getDimension(cmd, args, sender);
            EntityTools.instance().removeAllDuplicateEntities(dimension, sender);
        }
        else
        {
            throwCommand("worldutils.commands.error.unknowncommandargument", cmd);
        }
    }

    private int getDimension(String cmd, String[] args, ICommandSender sender) throws CommandException
    {
        int dimension = sender instanceof EntityPlayer ? ((EntityPlayer) sender).getEntityWorld().provider.getDimension() : 0;

        if (args.length == 1)
        {
            dimension = CommandBase.parseInt(args[0]);
        }
        else if (args.length > 1)
        {
            throwUsage(this.getUsageStringCommon() + " " + cmd + " [dimension id]");
        }

        return dimension;
    }
}
