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
import fi.dy.masa.worldutils.data.BlockTools.LoadedType;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;
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
        this.subSubCommands.add("execute-all-chunks");
        this.subSubCommands.add("execute-loaded-chunks");
        this.subSubCommands.add("execute-unloaded-chunks");
        this.subSubCommands.add("replacement");
        this.subSubCommands.add("stoptask");
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
        this.printHelpBlockNameList(sender);
        this.printHelpBlockStateList(sender);
        this.printHelpReplacement(sender);
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-all-chunks <keep-listed | replace-listed> [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-loaded-chunks <keep-listed | replace-listed> [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " execute-unloaded-chunks <keep-listed | replace-listed> [dimension id]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " stoptask"));
    }

    private void printHelpBlockNameList(ICommandSender sender)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add <block | id>[@meta] ... Ex: minecraft:ice minecraft:wool@5"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add <block[prop1=val1,prop2=val2]> ... Ex: minecraft:stone[variant=granite]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add-all-from-mod <modId> [modId] ..."));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add-with-spaces <block> (same as add, but for names that include spaces)"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add-all-removed-blocks"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist add-all-removed-blocks-from-mod <modId>"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist clear"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist list"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist remove stringonthelist1 stringonthelist2 ..."));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " blocknamelist remove-with-spaces string parts of the name"));
    }

    private void printHelpBlockStateList(ICommandSender sender)
    {
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
                return CommandBase.getListOfStringsMatchingLastWord(args,
                        "add",
                        "add-all-from-mod",
                        "add-all-removed-blocks",
                        "add-all-removed-blocks-from-mod",
                        "add-with-spaces",
                        "clear",
                        "list",
                        "remove",
                        "remove-with-spaces");
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
        else if ((cmd.equals("execute-all-chunks") || cmd.equals("execute-loaded-chunks") || cmd.equals("execute-unloaded-chunks")) && args.length == 1)
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
            this.executeBlockNameList(args, sender);
        }
        else if (cmd.equals("blockstatelist") && args.length >= 1)
        {
            this.executeBlockStateList(args, sender);
        }
        else if (cmd.equals("replacement") && args.length >= 1)
        {
            cmd = args[0];
            args = CommandWorldUtils.dropFirstStrings(args, 1);

            if (cmd.equals("print") && args.length == 0)
            {
                this.printBlockData(replacement, sender);
            }
            else if (cmd.equals("set") && args.length >= 1)
            {
                replacement = String.join(" ", args);
                this.printBlockData(replacement, sender);
            }
            else
            {
                this.printHelpReplacement(sender);
            }
        }
        else if ((cmd.equals("execute-all-chunks") || cmd.equals("execute-loaded-chunks") || cmd.equals("execute-unloaded-chunks")) &&
                args.length >= 1 && args.length <= 2 &&
                (args[0].equals("keep-listed") || args[0].equals("replace-listed")))
        {
            this.sendMessage(sender, "worldutils.commands.blockreplace.execute.start");
            int dimension = this.getDimension(cmd, CommandWorldUtils.dropFirstStrings(args, 1), sender);
            boolean keepListedBlocks = args[0].equals("keep-listed");
            LoadedType loaded = LoadedType.UNLOADED;

            if (cmd.equals("execute-all-chunks")) { loaded = LoadedType.ALL; }
            else if (cmd.equals("execute-loaded-chunks")) { loaded = LoadedType.LOADED; }

            BlockTools.instance().replaceBlocks(dimension, replacement, blockNames, blockStates, keepListedBlocks, loaded, sender);
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

    private void executeBlockNameList(String[] args, ICommandSender sender) throws CommandException
    {
        String cmd = args[0];
        args = CommandWorldUtils.dropFirstStrings(args, 1);

        if (cmd.equals("clear") && args.length == 0)
        {
            blockNames.clear();
            this.sendMessage(sender, "worldutils.commands.blockreplace.blocknamelist.cleared");
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
            this.sendMessage(sender, "worldutils.commands.generic.list.print");
        }
        else if (cmd.equals("add") && args.length > 0)
        {
            for (int i = 0; i < args.length; i++)
            {
                this.addBlockNameToListIfValid(args[i], sender);
            }
        }
        else if (cmd.equals("add-with-spaces") && args.length > 0)
        {
            this.addBlockNameToListIfValid(String.join(" ", args), sender);
        }
        else if (cmd.equals("add-all-from-mod") && args.length > 0)
        {
            for (int i = 0; i < args.length; i++)
            {
                blockNames.addAll(BlockUtils.getAllBlockNamesInMod(args[i]));
                this.sendMessage(sender, "worldutils.commands.blockreplace.blocknamelist.mod.add", args[i]);
            }
        }
        else if (cmd.equals("add-all-removed-blocks") && args.length == 0)
        {
            // FIXME 1.12 registry rewrite broke this
            /*
            @SuppressWarnings("deprecation")
            FMLControlledNamespacedRegistry<Block> reg = GameData.getBlockRegistry();

            for (ResourceLocation rl : ForgeRegistries.BLOCKS.getKeys())
            {
                if (reg.isDummied(rl))
                {
                    this.addBlockNameToListIfValid(rl.toString(), sender);
                }
            }
            */
        }
        else if (cmd.equals("add-all-removed-blocks-from-mod") && args.length > 0)
        {
            // FIXME 1.12 registry rewrite broke this
            /*
            @SuppressWarnings("deprecation")
            FMLControlledNamespacedRegistry<Block> reg = GameData.getBlockRegistry();

            for (ResourceLocation rl : ForgeRegistries.BLOCKS.getKeys())
            {
                if (rl.getResourceDomain().equals(args[0]) && reg.isDummied(rl))
                {
                    this.addBlockNameToListIfValid(rl.toString(), sender);
                }
            }
            */
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
        else if (cmd.equals("remove-with-spaces") && args.length > 0)
        {
            String str = String.join(" ", args);

            if (blockNames.remove(str))
            {
                this.sendMessage(sender, "worldutils.commands.generic.list.remove.success", str);
            }
            else
            {
                this.sendMessage(sender, "worldutils.commands.generic.list.remove.failure", str);
            }
        }
        else
        {
            this.printHelpBlockNameList(sender);
        }
    }

    private void executeBlockStateList(String[] args, ICommandSender sender) throws CommandException
    {
        String cmd = args[0];
        args = CommandWorldUtils.dropFirstStrings(args, 1);

        if (cmd.equals("add-all-vanilla") && args.length > 0)
        {
            VanillaVersion version = VanillaVersion.fromVersion(args[0]);

            if (version == null)
            {
                throwCommand("worldutils.commands.error.invalidgameversion", args[0]);
            }

            blockStates.addAll(VanillaBlocks.getSerializableVanillaBlockStatesInVersion(version));

            this.sendMessage(sender, "worldutils.commands.blockreplace.blockstatelist.add", args[0]);
        }
        else if (cmd.equals("clear") && args.length == 0)
        {
            blockStates.clear();
            this.sendMessage(sender, "worldutils.commands.blockreplace.blockstatelist.cleared");
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

            this.sendMessage(sender, "worldutils.commands.blockreplace.blockstatelist.print");
        }
        else
        {
            this.printHelpBlockStateList(sender);
        }
    }

    private void addBlockNameToListIfValid(String name, ICommandSender sender) throws CommandException
    {
        BlockData type = BlockData.parseBlockTypeFromString(name);

        if (type != null && type.isValid())
        {
            blockNames.add(name);
            this.sendMessage(sender, "worldutils.commands.generic.list.add", name + " => " + type.toString());
        }
        else
        {
            throwCommand("worldutils.commands.generic.list.add.failure.invalid", name);
        }
    }

    private void printBlockData(String blockStr, ICommandSender sender) throws CommandException
    {
        BlockData type = BlockData.parseBlockTypeFromString(blockStr);

        if (type != null && type.isValid())
        {
            this.sendMessage(sender, "worldutils.commands.blockreplace.block.print.valid", type.toString());
        }
        else
        {
            throwCommand("worldutils.commands.blockreplace.block.print.invalid", blockStr);
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
