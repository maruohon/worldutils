package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import fi.dy.masa.worldutils.data.TileTickTools;
import fi.dy.masa.worldutils.data.TileTickTools.RemoveType;
import fi.dy.masa.worldutils.util.FileUtils;

public class SubCommandTileTicks extends SubCommand
{
    public SubCommandTileTicks(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("list");
        this.subSubCommands.add("read-all");
        this.subSubCommands.add("remove-all");
        this.subSubCommands.add("remove-by-mod");
        this.subSubCommands.add("remove-by-name");
    }

    @Override
    public String getName()
    {
        return "tileticks";
    }

    @Override
    protected List<String> getTabCompletionOptionsSub(MinecraftServer server, ICommandSender sender, String[] args)
    {
        List<String> options = new ArrayList<String>();

        if (args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "read-all", "list", "remove-all", "remove-by-mod", "remove-by-name");
        }
        else if (args[1].equals("remove-by-mod") || args[1].equals("remove-by-name"))
        {
            if (args.length >= 4)
            {
                if (args[2].equals("add"))
                {
                    for (ResourceLocation rl : ForgeRegistries.BLOCKS.getKeys())
                    {
                        options.add(rl.toString());
                    }
                }
                else if (args[2].equals("remove"))
                {
                    options.addAll(TileTickTools.instance().getFilters());
                }

                return CommandBase.getListOfStringsMatchingLastWord(args, options);
            }
            else if (args.length >= 3)
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "add", "remove", "list", "clear-list", "execute");
            }
        }

        return options;
    }

    @Override
    public String getHelp()
    {
        return super.getHelp();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        super.execute(server, sender, args);

        if (args.length >= 2)
        {
            if (args[1].equals("help"))
            {
                sender.sendMessage(new TextComponentString(this.getUsageStringPre() + "read-all [dimension]"));
                sender.sendMessage(new TextComponentString(this.getUsageStringPre() + "list"));
                sender.sendMessage(new TextComponentString(this.getUsageStringPre() + "remove-all [dimension]"));
                sender.sendMessage(new TextComponentString(this.getUsageStringPre() + "<remove-by-mod | remove-by-name> <add | remove> <block name> [block name] ..."));
                sender.sendMessage(new TextComponentString(this.getUsageStringPre() + "<remove-by-mod | remove-by-name> clear-list"));
                sender.sendMessage(new TextComponentString(this.getUsageStringPre() + "<remove-by-mod | remove-by-name> list"));
                sender.sendMessage(new TextComponentString(this.getUsageStringPre() + "<remove-by-mod | remove-by-name> execute [dimension]"));
            }
            else if (args[1].equals("read-all"))
            {
                int dimension = this.getDimension(sender, args, 2, "");
                TileTickTools.instance().readTileTicks(dimension, sender);
            }
            else if (args[1].equals("list"))
            {
                File file = FileUtils.dumpDataToFile("tileticks", TileTickTools.instance().getAllTileTicksOutput(false));

                if (file != null)
                {
                    sender.sendMessage(new TextComponentString("Output written to file " + file.getName()));
                }
            }
            else if (args[1].equals("remove-all"))
            {
                int dimension = this.getDimension(sender, args, 2, "");
                TileTickTools.instance().removeTileTicks(dimension, RemoveType.ALL, false, sender);
            }
            else if (args[1].equals("remove-by-mod") || args[1].equals("remove-by-name"))
            {
                if (args.length >= 4 && args[2].equals("add"))
                {
                    for (int i = 3; i < args.length; i++)
                    {
                        TileTickTools.instance().addFilter(args[i]);
                        sender.sendMessage(new TextComponentString("Added '" + args[i] + "' to the list"));
                    }
                }
                else if (args.length >= 4 && args[2].equals("remove"))
                {
                    for (int i = 3; i < args.length; i++)
                    {
                        TileTickTools.instance().removeFilter(args[i]);
                        sender.sendMessage(new TextComponentString("Removed '" + args[i] + "' from the list"));
                    }
                }
                else if (args.length == 3 && args[2].equals("list"))
                {
                    Set<String> toRemove = TileTickTools.instance().getFilters();

                    if (toRemove.isEmpty())
                    {
                        sender.sendMessage(new TextComponentString("Nothing added to be removed"));
                    }
                    else
                    {
                        sender.sendMessage(new TextComponentString("Tile ticks will be be removed for anything matching: " + String.join(", ", toRemove)));
                    }
                }
                else if (args.length == 3 && args[2].equals("clear-list"))
                {
                    TileTickTools.instance().resetFilters();
                    sender.sendMessage(new TextComponentString("List cleared"));
                }
                else if (args.length >= 3 && args.length <= 4 && args[2].equals("execute"))
                {
                    RemoveType types = args[1].equals("remove-by-name") ? RemoveType.BY_NAME : RemoveType.BY_MOD;
                    int dimension = this.getDimension(sender, args, 3, " " + args[2]);
                    TileTickTools.instance().removeTileTicks(dimension, types, false, sender);
                }
                else
                {
                    sender.sendMessage(new TextComponentString(this.getUsageStringPre() + args[1] + " <add | remove> <block name> [block name] ..."));
                    sender.sendMessage(new TextComponentString(this.getUsageStringPre() + args[1] + " clear-list"));
                    sender.sendMessage(new TextComponentString(this.getUsageStringPre() + args[1] + " list"));
                    sender.sendMessage(new TextComponentString(this.getUsageStringPre() + args[1] + " execute [dimension]"));
                }
            }
            else
            {
                throw new WrongUsageException("Unknown sub-command argument '" + args[1] + "'", new Object[0]);
            }
        }
        else
        {
            //throw new WrongUsageException("Unknown sub-command argument '" + args[1] + "'", new Object[0]);
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
            throw new WrongUsageException(this.getUsageStringPre() + args[1] + usage + " [dimension]", new Object[0]);
        }

        return dimension;
    }
}