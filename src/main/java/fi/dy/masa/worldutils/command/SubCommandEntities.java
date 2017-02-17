package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.EntityTools;
import fi.dy.masa.worldutils.data.EntityTools.EntityRenamer;
import fi.dy.masa.worldutils.util.FileUtils;

public class SubCommandEntities extends SubCommand
{
    private static List<String> removeList = new ArrayList<String>();
    private static List<Pair<String, String>> renamePairs = new ArrayList<Pair<String, String>>();
    private String preparedFrom = EMPTY_STRING;
    private String preparedTo = EMPTY_STRING;

    public SubCommandEntities(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("list-all");
        this.subSubCommands.add("list-duplicates-all");
        this.subSubCommands.add("list-duplicates-only");
        this.subSubCommands.add("read-all");
        this.subSubCommands.add("remove");
        this.subSubCommands.add("remove-duplicate-uuids");
        this.subSubCommands.add("rename");
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
        this.printHelpRemove(sender);
        this.printHelpRename(sender);
    }

    private void printHelpRemove(ICommandSender sender)
    {
        this.sendMessage(sender, this.getUsageStringCommon() + " remove add <name> [name] ...");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove add-with-spaces <name with spaces>");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove remove <name> [name] ...");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove remove-with-spaces <name with spaces>");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove list");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove clear");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove execute-for-entities");
        this.sendMessage(sender, this.getUsageStringCommon() + " remove execute-for-tileentities");
    }

    private void printHelpRename(ICommandSender sender)
    {
        this.sendMessage(sender, this.getUsageStringCommon() + " rename add <name-from> <name-to>");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename add-prepared");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename remove <name-from> [name-from] ...");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename remove-with-spaces <name with spaces>");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename prepare-from <name with spaces>");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename prepare-to <name with spaces>");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename list");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename clear");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename execute-for-entities");
        this.sendMessage(sender, this.getUsageStringCommon() + " rename execute-for-tileentities");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length < 1)
        {
            return Collections.emptyList();
        }

        String cmd = args[0];
        String[] argsOrig = args;
        args = CommandWorldUtils.dropFirstStrings(args, 1);

        if (cmd.equals("remove"))
        {
            if (args.length == 1)
            {
                return CommandBase.getListOfStringsMatchingLastWord(args,
                        "add", "add-with-spaces", "clear", "execute-for-entities", "execute-for-tileentities", "list", "remove", "remove-with-spaces");
            }
            else
            {
                cmd = args[0];
                args = CommandWorldUtils.dropFirstStrings(args, 1);

                if (cmd.equals("remove") && args.length >= 1)
                {
                    return CommandBase.getListOfStringsMatchingLastWord(args, removeList);
                }
            }
        }
        else if (cmd.equals("rename"))
        {
            if (args.length == 1)
            {
                return CommandBase.getListOfStringsMatchingLastWord(args,
                        "add", "add-prepared", "clear", "execute-for-entities", "execute-for-tileentities",
                        "list", "prepare-from", "prepare-to", "remove", "remove-with-spaces");
            }
            else
            {
                cmd = args[0];
                args = CommandWorldUtils.dropFirstStrings(args, 1);

                if ((cmd.equals("remove") || cmd.equals("remove-with-spaces")) && args.length >= 1)
                {
                    List<String> names = new ArrayList<String>();

                    for (Pair<String, String> pair : renamePairs)
                    {
                        names.add(pair.getLeft());
                    }

                    return CommandBase.getListOfStringsMatchingLastWord(args, names);
                }
            }
        }

        return super.getTabCompletions(server, sender, argsOrig);
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
            this.sendMessage(sender, "worldutils.commands.entities.readall.start");
            int dimension = this.getDimension(cmd, args, sender);
            EntityTools.instance().readEntities(dimension, sender);
        }
        else if (cmd.equals("remove-duplicate-uuids"))
        {
            this.sendMessage(sender, "worldutils.commands.entities.removeallduplicates.start");
            int dimension = this.getDimension(cmd, args, sender);
            EntityTools.instance().removeAllDuplicateEntities(dimension, sender);
        }
        else if (cmd.equals("remove"))
        {
            this.executeRemove(server, sender, args);
        }
        else if (cmd.equals("rename"))
        {
            this.executeRename(server, sender, args);
        }
        else
        {
            throwCommand("worldutils.commands.error.unknowncommandargument", cmd);
        }
    }

    private void executeRemove(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            this.printHelpRemove(sender);
            return;
        }

        String cmd = args[0];
        args = dropFirstStrings(args, 1);

        if (cmd.equals("add"))
        {
            for (int i = 0; i < args.length; i++)
            {
                removeList.add(args[i]);
                this.sendMessage(sender, "worldutils.commands.generic.list.add", args[i]);
            }
        }
        else if (cmd.equals("add-with-spaces"))
        {
            String str = String.join(" ", args);
            removeList.add(str);
            this.sendMessage(sender, "worldutils.commands.generic.list.add", str);
        }
        else if (cmd.equals("clear"))
        {
            removeList.clear();
            this.sendMessage(sender, "worldutils.commands.generic.list.clear");
        }
        else if (cmd.equals("execute-for-entities") || cmd.equals("execute-for-tileentities"))
        {
            this.sendMessage(sender, "worldutils.commands.entities.remove.start");
            int dimension = this.getDimension(cmd, args, sender);
            EntityRenamer.Type type = cmd.equals("execute-for-tileentities") ? EntityRenamer.Type.TILE_ENTITIES : EntityRenamer.Type.ENTITIES;
            EntityTools.instance().removeEntities(dimension, removeList, type, sender);
        }
        else if (cmd.equals("list"))
        {
            WorldUtils.logger.info("----------------------------------");
            WorldUtils.logger.info("  Names on the remove list:");
            WorldUtils.logger.info("----------------------------------");

            for (String str : removeList)
            {
                WorldUtils.logger.info(str);
                sender.sendMessage(new TextComponentString(str));
            }

            WorldUtils.logger.info("-------------- END ---------------");
            this.sendMessage(sender, "worldutils.commands.generic.list.print");
        }
        else if (cmd.equals("remove"))
        {
            for (int i = 0; i < args.length; i++)
            {
                if (removeList.remove(args[i]))
                {
                    this.sendMessage(sender, "worldutils.commands.generic.list.remove.success", args[i]);
                }
                else
                {
                    this.sendMessage(sender, "worldutils.commands.generic.list.remove.failure", args[i]);
                }
            }
        }
        else if (cmd.equals("remove-with-spaces"))
        {
            String str = String.join(" ", args);

            if (removeList.remove(str))
            {
                this.sendMessage(sender, "worldutils.commands.generic.list.remove.success", str);
            }
            else
            {
                this.sendMessage(sender, "worldutils.commands.generic.list.remove.failure", str);
            }
        }
    }

    private void executeRename(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 1)
        {
            this.printHelpRename(sender);
            return;
        }

        String cmd = args[0];
        args = dropFirstStrings(args, 1);

        if (cmd.equals("add-prepared") || (cmd.equals("add") && args.length == 2))
        {
            String name1;
            String name2;

            if (cmd.equals("add-prepared"))
            {
                name1 = this.preparedFrom;
                name2 = this.preparedTo;
            }
            else
            {
                name1 = args[0];
                name2 = args[1];
            }

            renamePairs.add(Pair.of(name1, name2));
            this.sendMessage(sender, "worldutils.commands.generic.list.add", name1 + " => " + name2);
        }
        else if (cmd.equals("prepare-from") && args.length >= 1)
        {
            this.preparedFrom = String.join(" ", args);
            this.sendMessage(sender, "worldutils.commands.entities.prepare.from", this.preparedFrom);
        }
        else if (cmd.equals("prepare-to") && args.length >= 1)
        {
            this.preparedTo = String.join(" ", args);
            this.sendMessage(sender, "worldutils.commands.entities.prepare.to", this.preparedTo);
        }
        else if (cmd.equals("clear"))
        {
            renamePairs.clear();
            this.sendMessage(sender, "worldutils.commands.generic.list.clear");
        }
        else if (cmd.equals("execute-for-entities") || cmd.equals("execute-for-tileentities"))
        {
            this.sendMessage(sender, "worldutils.commands.entities.rename.start");
            int dimension = this.getDimension(cmd, args, sender);
            EntityRenamer.Type type = cmd.equals("execute-for-tileentities") ? EntityRenamer.Type.TILE_ENTITIES : EntityRenamer.Type.ENTITIES;
            EntityTools.instance().renameEntities(dimension, renamePairs, type, sender);
        }
        else if (cmd.equals("list"))
        {
            WorldUtils.logger.info("----------------------------------");
            WorldUtils.logger.info("  Names on the rename list:");
            WorldUtils.logger.info("----------------------------------");

            for (Pair<String, String> pair : renamePairs)
            {
                String str = pair.getLeft() + " => " + pair.getRight();
                WorldUtils.logger.info(str);
                sender.sendMessage(new TextComponentString(str));
            }

            WorldUtils.logger.info("-------------- END ---------------");
            this.sendMessage(sender, "worldutils.commands.generic.list.print");
        }
        else if (cmd.equals("remove"))
        {
            for (int a = 0; a < args.length; a++)
            {
                int size = renamePairs.size();

                for (int i = 0; i < size; i++)
                {
                    Pair<String, String> pair = renamePairs.get(i);

                    if (args[a].equals(pair.getLeft()))
                    {
                        renamePairs.remove(i);
                        this.sendMessage(sender, "worldutils.commands.generic.list.remove.success", pair.getLeft() + " => " + pair.getRight());
                        break;
                    }
                }

                if (size == renamePairs.size())
                {
                    this.sendMessage(sender, "worldutils.commands.generic.list.remove.failure", args[a]);
                }
            }
        }
        else if (cmd.equals("remove-with-spaces"))
        {
            String str = String.join(" ", args);
            int size = renamePairs.size();

            for (int i = 0; i < size; i++)
            {
                Pair<String, String> pair = renamePairs.get(i);

                if (str.equals(pair.getLeft()))
                {
                    renamePairs.remove(i);
                    this.sendMessage(sender, "worldutils.commands.generic.list.remove.success", pair.getLeft() + " => " + pair.getRight());
                    return;
                }
            }

            this.sendMessage(sender, "worldutils.commands.generic.list.remove.failure", str);
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
