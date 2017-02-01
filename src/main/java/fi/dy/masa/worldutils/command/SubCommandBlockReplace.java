package fi.dy.masa.worldutils.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.BlockTools;
import fi.dy.masa.worldutils.util.BlockData;
import fi.dy.masa.worldutils.util.BlockUtils;
import fi.dy.masa.worldutils.util.VanillaBlocks;
import fi.dy.masa.worldutils.util.VanillaBlocks.VanillaVersion;

public class SubCommandBlockReplace extends SubCommand
{
    private static List<String> blockNames = new ArrayList<String>();
    private static List<IBlockState> blockStates = new ArrayList<IBlockState>();
    private static String replacement = "minecraft:air";

    public SubCommandBlockReplace(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("blocknamelist");
        this.subSubCommands.add("blockstatelist");
        this.subSubCommands.add("execute-immediate-all-chunks");
        this.subSubCommands.add("execute-immediate-unloaded-chunks");
        this.subSubCommands.add("execute-scheduled-all-chunks");
        this.subSubCommands.add("execute-scheduled-unloaded-chunks");
        this.subSubCommands.add("replacement");
    }

    @Override
    public String getName()
    {
        return "blockreplace";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.runhelpforallcommands", this.getUsageStringCommon() + " help");
    }

    @Override
    public void printFullHelp(ICommandSender sender, String[] args)
    {
        this.prinHelpBlocklist(sender);
        this.printHelpReplacement(sender);
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-immediate-all-chunks <keep-listed | replace-listed> [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-immediate-unloaded-chunks <keep-listed | replace-listed> [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-scheduled-all-chunks <keep-listed | replace-listed> [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-scheduled-unloaded-chunks <keep-listed | replace-listed> [dimension id]"));
    }

    private void prinHelpBlocklist(ICommandSender sender)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist clear"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist list"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add <block | id>[@meta] ... Ex: minecraft:ice minecraft:wool@5"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add <block[prop1=val1,prop2=val2]> ... Ex: minecraft:stone[variant=granite]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist remove <block | id>[@meta] ... Ex: minecraft:ice minecraft:wool@5"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist remove <block[prop1=val1,prop2=val2]> ... Ex: minecraft:stone[variant=granite]"));

        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blockstatelist add-all-vanilla <version> (where version = 1.5 ... 1.10)"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blockstatelist clear"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blockstatelist list"));
    }

    private void printHelpReplacement(ICommandSender sender)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " replacement set <block | id>[@meta] Ex: minecraft:ice minecraft:wool@5"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " replacement set <block[prop1=val1,prop2=val2]> Ex: minecraft:stone[variant=granite]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " replacement print"));
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

        if (cmd.equals("blocknamelist"))
        {
            if (args.length == 1)
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "add", "clear", "list", "remove");
            }
            else if (args.length >= 2)
            {
                cmd = args[0];

                if (cmd.equals("add"))
                {
                    return CommandBase.getListOfStringsMatchingLastWord(args, BlockUtils.getAllBlockNames());
                }
                else if (cmd.equals("remove"))
                {
                    return CommandBase.getListOfStringsMatchingLastWord(args, blockNames);
                }
            }
        }
        else if (cmd.equals("blockstatelist"))
        {
            if (args.length == 1)
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "add-all-vanilla", "clear", "list");
            }
        }
        else if ((cmd.equals("execute-immediate-all-chunks") || cmd.equals("execute-immediate-unloaded-chunks") ||
                  cmd.equals("execute-scheduled-all-chunks") || cmd.equals("execute-scheduled-unloaded-chunks")) && args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "keep-listed", "replace-listed");
        }
        else if (cmd.equals("replacement"))
        {
            if (args.length == 1)
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "print", "set");
            }
            else if (args.length >= 2)
            {
                cmd = args[0];

                if (cmd.equals("set"))
                {
                    List<String> options = new ArrayList<String>();

                    for (ResourceLocation rl : Block.REGISTRY.getKeys())
                    {
                        options.add(rl.toString());
                    }

                    return CommandBase.getListOfStringsMatchingLastWord(args, options);
                }
            }
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

        if (cmd.equals("blocknamelist") && args.length >= 1)
        {
            cmd = args[0];
            args = CommandWorldUtils.dropFirstStrings(args, 1);

            if (cmd.equals("clear") && args.length == 0)
            {
                blockNames.clear();
                this.sendMessage(sender, "worldutils.commands.blockprune.blocknamelist.cleared");
            }
            else if (cmd.equals("list") && args.length == 0)
            {
                WorldUtils.logger.info("----------------------------------");
                WorldUtils.logger.info("  Blocks on the name list:");
                WorldUtils.logger.info("----------------------------------");

                for (String str : blockNames)
                {
                    WorldUtils.logger.info(str);
                    sender.sendMessage(new TextComponentString(str));
                }

                WorldUtils.logger.info("-------------- END ---------------");
                this.sendMessage(sender, "worldutils.commands.blockprune.blocknamelist.print");
            }
            else if (cmd.equals("add") && args.length > 0)
            {
                for (int i = 0; i < args.length; i++)
                {
                    blockNames.add(args[i]);
                    this.sendMessage(sender, "worldutils.commands.generic.list.add", args[i]);
                }
            }
            else if (cmd.equals("remove") && args.length > 0)
            {
                for (int i = 0; i < args.length; i++)
                {
                    if (blockNames.remove(args[i]))
                    {
                        this.sendMessage(sender, "worldutils.commands.generic.list.remove.success", args[i]);
                    }
                    else
                    {
                        this.sendMessage(sender, "worldutils.commands.generic.list.remove.failure", args[i]);
                    }
                }
            }
            else
            {
                this.prinHelpBlocklist(sender);
            }
        }
        else if (cmd.equals("blockstatelist") && args.length >= 1)
        {
            cmd = args[0];
            args = CommandWorldUtils.dropFirstStrings(args, 1);

            if (cmd.equals("add-all-vanilla") && args.length > 0)
            {
                VanillaVersion version = VanillaVersion.fromVersion(args[0]);

                if (version == null)
                {
                    throwCommand("worldutils.commands.error.invalidgameversion", args[0]);
                }

                blockStates.addAll(VanillaBlocks.getSerializableVanillaBlockStatesInVersion(version));

                this.sendMessage(sender, "worldutils.commands.blockprune.blockstatelist.add", args[0]);
            }
            else if (cmd.equals("clear") && args.length == 0)
            {
                blockStates.clear();
                this.sendMessage(sender, "worldutils.commands.blockprune.blockstatelist.cleared");
            }
            else if (cmd.equals("list") && args.length == 0)
            {
                WorldUtils.logger.info("----------------------------------");
                WorldUtils.logger.info("  Blocks on the IBlockState list:");
                WorldUtils.logger.info("----------------------------------");

                for (IBlockState state : blockStates)
                {
                    WorldUtils.logger.info(state);
                }

                WorldUtils.logger.info("-------------- END ---------------");

                this.sendMessage(sender, "worldutils.commands.blockprune.blockstatelist.print");
            }
        }
        else if (cmd.equals("replacement") && args.length >= 1)
        {
            cmd = args[0];
            args = CommandWorldUtils.dropFirstStrings(args, 1);

            if (cmd.equals("print") && args.length == 0)
            {
                this.replacementPrint(replacement, sender);
            }
            else if (cmd.equals("set") && args.length == 1)
            {
                replacement = args[0];
                this.replacementPrint(replacement, sender);
            }
            else
            {
                this.printHelpReplacement(sender);
            }
        }
        else if ((cmd.equals("execute-immediate-all-chunks") || cmd.equals("execute-immediate-unloaded-chunks")) &&
                args.length >= 1 && args.length <= 2 &&
                (args[0].equals("keep-listed") || args[0].equals("replace-listed")))
        {
            this.sendMessage(sender, "worldutils.commands.blockprune.execute.start");
            int dimension = this.getDimension(cmd, CommandWorldUtils.dropFirstStrings(args, 1), sender);

            BlockTools.instance().replaceBlocks(dimension, replacement, blockNames, blockStates,
                    args[0].equals("keep-listed"), cmd.equals("execute-immediate-all-chunks"), false, sender);
        }
        else if ((cmd.equals("execute-scheduled-all-chunks") || cmd.equals("execute-scheduled-unloaded-chunks")) &&
                args.length >= 1 && args.length <= 2 &&
                (args[0].equals("keep-listed") || args[0].equals("replace-listed")))
        {
            this.sendMessage(sender, "worldutils.commands.blockprune.execute.start");
            int dimension = this.getDimension(cmd, CommandWorldUtils.dropFirstStrings(args, 1), sender);

            BlockTools.instance().replaceBlocks(dimension, replacement, blockNames, blockStates,
                    args[0].equals("keep-listed"), cmd.equals("execute-scheduled-all-chunks"), true, sender);
        }
        else
        {
            this.printFullHelp(sender, args);
        }
    }

    private void replacementPrint(String replacement, ICommandSender sender) throws CommandException
    {
        BlockData type = BlockData.parseBlockTypeFromString(replacement);

        if (type != null && type.isValid())
        {
            this.sendMessage(sender, "worldutils.commands.blockprune.replacement.print.valid", type.toString());
        }
        else
        {
            throwCommand("worldutils.commands.blockprune.replacement.print.invalid", replacement);
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
