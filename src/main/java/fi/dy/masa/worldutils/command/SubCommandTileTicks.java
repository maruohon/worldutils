package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.worldutils.data.TileTickTools;
import fi.dy.masa.worldutils.data.TileTickTools.RemoveType;
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
                List<String> options = new ArrayList<String>();

                if (args[0].equals("add"))
                {
                    for (ResourceLocation rl : Block.REGISTRY.getKeys())
                    {
                        options.add(rl.toString());
                    }
                }
                else if (args[0].equals("remove"))
                {
                    options.addAll(TileTickTools.instance().getFilters());
                }

                return CommandBase.getListOfStringsMatchingLastWord(args, options);
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
            this.printHelpGeneric(sender);
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
            int count = TileTickTools.instance().readTileTicks(dimension, sender);

            this.sendMessage(sender, "worldutils.commands.tileticks.reading.complete", Integer.valueOf(count));
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

                    int count = TileTickTools.instance().findInvalid(dimension, forceRescan, sender);

                    this.sendMessage(sender, "worldutils.commands.tileticks.readingandfindinginvalid.complete", Integer.valueOf(count));
                }
                else
                {
                    this.sendMessage(sender, "worldutils.commands.tileticks.removeinvalid.start");

                    int count = TileTickTools.instance().removeInvalid(dimension, forceRescan, sender);

                    this.sendMessage(sender, "worldutils.commands.tileticks.remove.complete", Integer.valueOf(count));
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
            TileTickTools.instance().removeTileTicks(dimension, RemoveType.ALL, false, sender);
        }
        else if (cmd.equals("remove-by-mod") || cmd.equals("remove-by-name"))
        {
            if (args.length >= 2 && args[0].equals("add"))
            {
                for (int i = 1; i < args.length; i++)
                {
                    TileTickTools.instance().addFilter(args[i]);

                    this.sendMessage(sender, "worldutils.commands.generic.list.add", args[i]);
                }
            }
            else if (args.length >= 2 && args[0].equals("remove"))
            {
                for (int i = 1; i < args.length; i++)
                {
                    TileTickTools.instance().removeFilter(args[i]);

                    this.sendMessage(sender, "worldutils.commands.generic.list.remove", args[i]);
                }
            }
            else if (args.length == 1 && args[0].equals("list"))
            {
                Set<String> toRemove = TileTickTools.instance().getFilters();

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
                TileTickTools.instance().resetFilters();
                this.sendMessage(sender, "worldutils.commands.tileticks.list.clear");
            }
            else if (args.length >= 1 && args.length <= 2 && args[0].equals("execute"))
            {
                RemoveType types = cmd.equals("remove-by-name") ? RemoveType.BY_NAME : RemoveType.BY_MOD;
                int dimension = this.getDimension(sender, args, 1, " " + args[0]);
                int count = TileTickTools.instance().removeTileTicks(dimension, types, false, sender);

                this.sendMessage(sender, "worldutils.commands.tileticks.remove.complete", Integer.valueOf(count));
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
