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
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;
import fi.dy.masa.worldutils.util.BlockData;
import fi.dy.masa.worldutils.util.BlockUtils;

public class SubCommandBlockReplacePairs extends SubCommand
{
    private static List<Pair<String, String>> blockPairs = new ArrayList<Pair<String, String>>();

    public SubCommandBlockReplacePairs(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("add");
        this.subSubCommands.add("clear");
        this.subSubCommands.add("execute-all-chunks");
        this.subSubCommands.add("execute-unloaded-chunks");
        this.subSubCommands.add("list");
        this.subSubCommands.add("remove");
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
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " clear"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-all-chunks [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-unloaded-chunks [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " list"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " remove block1 block2 (you must give both the 'from' and 'to' block specifiers)"));
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

        if (cmd.equals("add"))
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, BlockUtils.getAllBlockNames());
        }
        else if (cmd.equals("remove"))
        {
            List<String> names = new ArrayList<String>();
            for (Pair<String, String> pair : blockPairs)
            {
                names.add(pair.getLeft());
                names.add(pair.getRight());
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
        else if (cmd.equals("add") && args.length == 2)
        {
            BlockData block1 = BlockData.parseBlockTypeFromString(args[0]);
            BlockData block2 = BlockData.parseBlockTypeFromString(args[1]);

            if (block1 != null && block1.isValid() && block2 != null && block2.isValid())
            {
                blockPairs.add(Pair.of(args[0], args[1]));
                this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.add.success", block1.toString(), block2.toString());
            }
            else if (block1 == null || block1.isValid() == false)
            {
                throwCommand("worldutils.commands.blockreplace.blockpairs.list.add.invalid", args[0]);
            }
            else if (block2 == null || block2.isValid() == false)
            {
                throwCommand("worldutils.commands.blockreplace.blockpairs.list.add.invalid", args[1]);
            }
        }
        else if (cmd.equals("remove") && args.length == 2)
        {
            int size = blockPairs.size();

            for (int i = 0; i < size; i++)
            {
                Pair<String, String> pair = blockPairs.get(i);

                if (args[0].equals(pair.getLeft()) && args[1].equals(pair.getRight()))
                {
                    blockPairs.remove(i);
                    this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.remove.success", args[0], args[1]);
                    return;
                }
            }

            this.sendMessage(sender, "worldutils.commands.blockreplace.blockpairs.list.remove.failure", args[0], args[1]);
        }
        else if ((cmd.equals("execute-all-chunks") || cmd.equals("execute-unloaded-chunks")) && args.length <= 1)
        {
            this.sendMessage(sender, "worldutils.commands.blockreplace.execute.start");
            int dimension = this.getDimension(cmd, CommandWorldUtils.dropFirstStrings(args, 1), sender);
            boolean unloadedChunks = cmd.equals("execute-all-chunks");

            BlockTools.instance().replaceBlocksInPairs(dimension, blockPairs, unloadedChunks, sender);
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
