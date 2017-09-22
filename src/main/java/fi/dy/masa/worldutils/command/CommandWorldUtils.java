package fi.dy.masa.worldutils.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;

public class CommandWorldUtils extends CommandBase
{
    private final Map<String, ISubCommand> subCommands = new HashMap<String, ISubCommand>();

    public CommandWorldUtils()
    {
        this.registerSubCommand(new SubCommandBatchRun(this));
        this.registerSubCommand(new SubCommandBlockReplace(this));
        this.registerSubCommand(new SubCommandBlockReplacePairs(this));
        this.registerSubCommand(new SubCommandDump(this));
        this.registerSubCommand(new SubCommandEntities(this));
        this.registerSubCommand(new SubCommandInspectBlock(this));
        this.registerSubCommand(new SubCommandPrintSpawn(this));
        this.registerSubCommand(new SubCommandRegistry(this));
        this.registerSubCommand(new SubCommandSetBlock(this));
        this.registerSubCommand(new SubCommandTileTicks(this));
    }

    @Override
    public String getName()
    {
        return "worldutils";
    }

    @Override
    public String getUsage(ICommandSender sender)
    {
        return "/" + this.getName() + " <" + String.join(" | ", this.getCommandNamesSorted()) + ">";
    }

    public List<String> getCommandNamesSorted()
    {
        List<String> commands = new ArrayList<String>();
        commands.addAll(this.getSubCommandNames());
        commands.add("help");
        Collections.sort(commands);

        return commands;
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if (args.length == 1 || (args.length == 2 && args[0].equals("help")))
        {
            return getListOfStringsMatchingLastWord(args, this.getCommandNamesSorted());
        }
        else if (args.length > 1 && this.subCommands.containsKey(args[0]))
        {
            ISubCommand command = this.subCommands.get(args[0]);
            return command.getTabCompletions(server, sender, dropFirstStrings(args, 1), targetPos);
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length == 0)
        {
            throwUsage(this.getUsage(sender));
        }

        String cmd = args[0];

        if (this.subCommands.containsKey(cmd))
        {
            this.subCommands.get(cmd).execute(server, sender, dropFirstStrings(args, 1));
        }
        else if (cmd.equals("help"))
        {
            if (args.length == 1)
            {
                sendMessage(sender, "worldutils.commands.help.generic.usage", this.getUsage(sender));
            }
            else if (args.length == 2)
            {
                if (this.subCommands.containsKey(args[1]))
                {
                    this.subCommands.get(args[1]).execute(server, sender, new String[] { "help" });
                }
                else
                {
                    throwCommand("worldutils.commands.error.unknowncommandvariant", args[1]);
                }
            }
            else
            {
                throwUsage(this.getUsage(sender));
            }
        }
        else
        {
            throwCommand("worldutils.commands.error.unknowncommandvariant", args[1]);
        }
    }

    public void registerSubCommand(ISubCommand cmd)
    {
        if (this.subCommands.containsKey(cmd.getName()) == false)
        {
            this.subCommands.put(cmd.getName(), cmd);
        }
    }

    public Collection<String> getSubCommandNames()
    {
        return this.subCommands.keySet();
    }

    public static String[] dropFirstStrings(String[] input, int toDrop)
    {
        if (toDrop >= input.length)
        {
            return new String[0];
        }

        String[] arr = new String[input.length - toDrop];
        System.arraycopy(input, toDrop, arr, 0, input.length - toDrop);
        return arr;
    }

    public static void sendMessage(ICommandSender sender, String message, Object... params)
    {
        sender.sendMessage(new TextComponentTranslation(message, params));
    }

    public static void throwUsage(String message, Object... params) throws CommandException
    {
        throw new WrongUsageException(message, params);
    }

    public static void throwNumber(String message, Object... params) throws CommandException
    {
        throw new NumberInvalidException(message, params);
    }

    public static void throwCommand(String message, Object... params) throws CommandException
    {
        throw new CommandException(message, params);
    }
}
