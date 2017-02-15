package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.worldutils.data.TileTickTools;
import fi.dy.masa.worldutils.data.TileTickTools.Operation;
import fi.dy.masa.worldutils.util.BlockUtils;
import fi.dy.masa.worldutils.util.FileUtils;

public class SubCommandTileTicks extends SubCommand
{
    public SubCommandTileTicks(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("find-invalid");
        this.subSubCommands.add("list");
        this.subSubCommands.add("read-all");
        this.subSubCommands.add("remove-all");
        this.subSubCommands.add("remove-by-mod");
        this.subSubCommands.add("remove-by-name");
        this.subSubCommands.add("remove-invalid");
    }

    @Override
    public String getName()
    {
        return "tileticks";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.runhelpforallcommands", this.getUsageStringCommon() + " help");
    }

    @Override
    public void printFullHelp(ICommandSender sender, String[] args)
    {
        this.sendMessage(sender, this.getUsageStringCommon() + " find-invalid [rescan] [dimension id]");
        this.sendMessage(sender, this.getUsageStringCommon() + " find-invalid list");
        this.sendMessage(sender, this.getUsageStringCommon() + " list");
        this.sendMessage(sender, this.getUsageStringCommon() + " read-all [dimension]");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove-all [dimension]");
        this.sendMessage(sender, this.getUsageStringCommon() + " <remove-by-mod | remove-by-name> <add | remove> <block name> [block name] ...");
        this.sendMessage(sender, this.getUsageStringCommon() + " <remove-by-mod | remove-by-name> clear-list");
        this.sendMessage(sender, this.getUsageStringCommon() + " <remove-by-mod | remove-by-name> list");
        this.sendMessage(sender, this.getUsageStringCommon() + " <remove-by-mod | remove-by-name> execute [dimension id]");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove-invalid [rescan] [dimension id]");
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args)
    {
        String cmd = args[0];
        args = dropFirstStrings(args, 1);

        if (cmd.equals("find-invalid") && args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "list", "rescan");
        }
        else if (cmd.equals("remove-invalid") && args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "rescan");
        }
        else if (cmd.equals("remove-by-mod") || cmd.equals("remove-by-name"))
        {
            if (args.length >= 2)
            {
                if (args[0].equals("add"))
                {
                    return CommandBase.getListOfStringsMatchingLastWord(args, BlockUtils.getAllBlockNames());
                }
                else if (args[0].equals("remove"))
                {
                    Operation operation = cmd.equals("remove-by-mod") ? Operation.REMOVE_BY_MOD : Operation.REMOVE_BY_NAME;
                    return CommandBase.getListOfStringsMatchingLastWord(args, TileTickTools.instance().getFilters(operation));
                }
            }
            else if (args.length == 1)
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "add", "remove", "list", "clear-list", "execute");
            }
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            this.printFullHelp(sender, args);
            return;
        }

        String cmd = args[0];
        args = dropFirstStrings(args, 1);

        if (cmd.equals("help"))
        {
            this.printFullHelp(sender, args);
        }
        else if (cmd.equals("read-all"))
        {
            this.sendMessage(sender, "worldutils.commands.tileticks.reading.start");
            int dimension = this.getDimension(sender, args, 0, "");
            TileTickTools.instance().startTask(dimension, Operation.READ, true, sender);
        }
        else if (cmd.equals("find-invalid") || cmd.equals("remove-invalid"))
        {
            if (cmd.equals("find-invalid") && args.length == 1 && args[0].equals("list"))
            {
                File file = FileUtils.dumpDataToFile("tileticks_invalid", TileTickTools.instance().getInvalidTileTicksOutput(true));

                if (file != null)
                {
                    this.sendMessage(sender, "worldutils.commands.info.outputtofile", file.getName());
                }
            }
            else
            {
                boolean forceRescan = false;
                int dimIndex = 0;
                if (args.length >= 1 && args[0].equals("rescan"))
                {
                    forceRescan = true;
                    dimIndex++;
                }

                int dimension = this.getDimension(sender, args, dimIndex, "");

                if (cmd.equals("find-invalid"))
                {
                    this.sendMessage(sender, "worldutils.commands.tileticks.readingandfindinginvalid.start");
                    TileTickTools.instance().startTask(dimension, Operation.FIND_INVALID, forceRescan, sender);
                }
                else
                {
                    this.sendMessage(sender, "worldutils.commands.tileticks.removeinvalid.start");
                    TileTickTools.instance().startTask(dimension, Operation.REMOVE_INVALID, forceRescan, sender);
                }
            }
        }
        else if (cmd.equals("list"))
        {
            File file = FileUtils.dumpDataToFile("tileticks_list", TileTickTools.instance().getAllTileTicksOutput(false));

            if (file != null)
            {
                this.sendMessage(sender, "worldutils.commands.info.outputtofile", file.getName());
            }
        }
        else if (cmd.equals("remove-all"))
        {
            int dimension = this.getDimension(sender, args, 0, "");
            TileTickTools.instance().startTask(dimension, Operation.REMOVE_ALL, true, sender);
        }
        else if (cmd.equals("remove-by-mod") || cmd.equals("remove-by-name"))
        {
            Operation operation = cmd.equals("remove-by-mod") ? Operation.REMOVE_BY_MOD : Operation.REMOVE_BY_NAME;

            if (args.length >= 2 && args[0].equals("add"))
            {
                for (int i = 1; i < args.length; i++)
                {
                    TileTickTools.instance().addFilter(args[i], operation);

                    this.sendMessage(sender, "worldutils.commands.generic.list.add", args[i]);
                }
            }
            else if (args.length >= 2 && args[0].equals("remove"))
            {
                for (int i = 1; i < args.length; i++)
                {
                    TileTickTools.instance().removeFilter(args[i], operation);

                    this.sendMessage(sender, "worldutils.commands.generic.list.remove.success", args[i]);
                }
            }
            else if (args.length == 1 && args[0].equals("list"))
            {
                Set<String> toRemove = TileTickTools.instance().getFilters(operation);

                if (toRemove.isEmpty())
                {
                    this.sendMessage(sender, "worldutils.commands.tileticks.list.list.empty");
                }
                else
                {
                    this.sendMessage(sender, "worldutils.commands.tileticks.list.list.print", String.join(", ", toRemove));
                }
            }
            else if (args.length == 1 && args[0].equals("clear-list"))
            {
                TileTickTools.instance().resetFilters(operation);
                this.sendMessage(sender, "worldutils.commands.generic.list.clear");
            }
            else if (args.length >= 1 && args.length <= 2 && args[0].equals("execute"))
            {
                this.sendMessage(sender, "worldutils.commands.tileticks.remove.start");
                int dimension = this.getDimension(sender, args, 1, " " + args[0]);
                TileTickTools.instance().startTask(dimension, operation, true, sender);
            }
            else
            {
                this.sendMessage(sender, this.getUsageStringCommon() + " " + cmd + " <add | remove> <block name> [block name] ...");
                this.sendMessage(sender, this.getUsageStringCommon() + " " + cmd + " clear-list");
                this.sendMessage(sender, this.getUsageStringCommon() + " " + cmd + " list");
                this.sendMessage(sender, this.getUsageStringCommon() + " " + cmd + " execute [dimension]");
            }
        }
        else
        {
            throwCommand("worldutils.commands.error.unknowncommandargument", cmd);
        }
    }

    private int getDimension(ICommandSender sender, String[] args, int dimIndex, String usage) throws CommandException
    {
        int dimension = sender instanceof EntityPlayer ? ((EntityPlayer) sender).getEntityWorld().provider.getDimension() : 0;

        if (args.length == dimIndex + 1)
        {
            dimension = CommandBase.parseInt(args[dimIndex]);
        }
        else if (args.length > dimIndex + 1)
        {
            throwUsage(this.getUsageStringCommon() + " " + usage + " [dimension id]");
        }

        return dimension;
    }
}
