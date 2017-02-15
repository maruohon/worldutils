package fi.dy.masa.worldutils.command;

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
import fi.dy.masa.worldutils.data.BlockTools;
import fi.dy.masa.worldutils.data.BlockTools.LoadedType;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;
import fi.dy.masa.worldutils.util.BlockData;
import fi.dy.masa.worldutils.util.BlockUtils;

public class SubCommandBlockReplacePairs extends SubCommand
{
    private static List<Pair<String, String>> blockPairs = new ArrayList<Pair<String, String>>();
    private String preparedFrom = EMPTY_STRING;
    private String preparedTo = EMPTY_STRING;

    public SubCommandBlockReplacePairs(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("add");
        this.subSubCommands.add("add-prepared");
        this.subSubCommands.add("clear");
        this.subSubCommands.add("execute-all-chunks");
        this.subSubCommands.add("execute-loaded-chunks");
        this.subSubCommands.add("execute-unloaded-chunks");
        this.subSubCommands.add("list");
        this.subSubCommands.add("prepare-from");
        this.subSubCommands.add("prepare-to");
        this.subSubCommands.add("remove");
        this.subSubCommands.add("remove-with-spaces");
        this.subSubCommands.add("stoptask");
    }

    @Override
    public String getName()
    {
        return "blockreplacepairs";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.runhelpforallcommands", this.getUsageStringCommon() + " help");
    }

    @Override
    public void printFullHelp(ICommandSender sender, String[] args)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " add <block1 | id1>[@meta1] <block2 | id2>[@meta2] Ex: minecraft:ice minecraft:wool@5"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " add <block1[prop1=val1,prop2=val2]> <block2[prop1=val1,prop2=val2]> Ex: minecraft:stone[variant=granite]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " add-prepared (adds the prepared space-containing names)"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " clear"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-all-chunks [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-loaded-chunks [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-unloaded-chunks [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " list"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " prepare-from <block specifier containing spaces>"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " prepare-to <block specifier containing spaces>"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " remove <block-from> [block-from] ..."));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " remove-with-spaces <block-from>"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " stoptask"));
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length < 1)
        {
            return Collections.emptyList();
        }

        String cmd = args[0];
        args = CommandWorldUtils.dropFirstStrings(args, 1);

        if (cmd.equals("add") || cmd.equals("prepare-from") || cmd.equals("prepare-to"))
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, BlockUtils.getAllBlockNames());
        }
        else if ((cmd.equals("remove") || cmd.equals("remove-with-spaces")) && args.length >= 1)
        {
            List<String> names = new ArrayList<String>();

            for (Pair<String, String> pair : blockPairs)
            {
                names.add(pair.getLeft());
            }

            return CommandBase.getListOfStringsMatchingLastWord(args, names);
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

        if (cmd.equals("clear") && args.length == 0)
        {
            blockPairs.clear();
            this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.cleared");
        }
        else if (cmd.equals("list") && args.length == 0)
        {
            WorldUtils.logger.info("----------------------------------");
            WorldUtils.logger.info("  Block pairs on the list:");
            WorldUtils.logger.info("----------------------------------");

            for (Pair<String, String> pair : blockPairs)
            {
                String str = pair.getLeft() + " => " + pair.getRight();
                WorldUtils.logger.info(str);
                sender.sendMessage(new TextComponentString(str));
            }

            WorldUtils.logger.info("-------------- END ---------------");
            this.sendMessage(sender, "worldutils.commands.blockreplace.blocknamelist.print");
        }
        else if (cmd.equals("add-prepared") || (cmd.equals("add") && args.length == 2))
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

            BlockData block1 = BlockData.parseBlockTypeFromString(name1);
            BlockData block2 = BlockData.parseBlockTypeFromString(name2);

            if (block1 != null && block1.isValid() && block2 != null && block2.isValid())
            {
                blockPairs.add(Pair.of(name1, name2));
                this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.add.success", block1.toString(), block2.toString());
            }
            else if (block1 == null || block1.isValid() == false)
            {
                throwCommand("worldutils.commands.blockreplace.blockpairs.list.add.invalid", name1);
            }
            else if (block2 == null || block2.isValid() == false)
            {
                throwCommand("worldutils.commands.blockreplace.blockpairs.list.add.invalid", name2);
            }
        }
        else if (cmd.equals("prepare-from") && args.length >= 1)
        {
            this.preparedFrom = String.join(" ", args);
            this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.prepare.from", this.preparedFrom);
        }
        else if (cmd.equals("prepare-to") && args.length >= 1)
        {
            this.preparedTo = String.join(" ", args);
            this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.prepare.to", this.preparedTo);
        }
        else if (cmd.equals("remove") && args.length >= 1)
        {
            for (int a = 0; a < args.length; a++)
            {
                int size = blockPairs.size();

                for (int i = 0; i < size; i++)
                {
                    Pair<String, String> pair = blockPairs.get(i);

                    if (args[a].equals(pair.getLeft()))
                    {
                        blockPairs.remove(i);
                        this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.remove.success", pair.getLeft(), pair.getRight());
                        break;
                    }
                }

                if (size == blockPairs.size())
                {
                    this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.remove.failure", args[a]);
                }
            }
        }
        else if (cmd.equals("remove-with-spaces") && args.length >= 1)
        {
            String str = String.join(" ",  args);
            int size = blockPairs.size();

            for (int i = 0; i < size; i++)
            {
                Pair<String, String> pair = blockPairs.get(i);

                if (str.equals(pair.getLeft()))
                {
                    blockPairs.remove(i);
                    this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.remove.success", pair.getLeft(), pair.getRight());
                    return;
                }
            }

            this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.remove.failure", args[0]);
        }
        else if ((cmd.equals("execute-all-chunks") || cmd.equals("execute-loaded-chunks") || cmd.equals("execute-unloaded-chunks")) && args.length <= 1)
        {
            if (TaskScheduler.getInstance().hasTasks())
            {
                throwCommand("worldutils.commands.error.taskalreadyrunning");
            }

            this.sendMessage(sender, "worldutils.commands.blockreplace.execute.start");
            int dimension = this.getDimension(cmd, CommandWorldUtils.dropFirstStrings(args, 1), sender);
            LoadedType loaded = LoadedType.UNLOADED;

            if (cmd.equals("execute-all-chunks")) { loaded = LoadedType.ALL; }
            else if (cmd.equals("execute-loaded-chunks")) { loaded = LoadedType.LOADED; }

            BlockTools.instance().replaceBlocksInPairs(dimension, blockPairs, loaded, sender);
        }
        else if (cmd.equals("stoptask"))
        {
            if (TaskScheduler.getInstance().removeTask(TaskWorldProcessor.class))
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
