package fi.dy.masa.worldtools.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public class CommandWorldTools extends CommandBase
{
    private final Map<String, ISubCommand> subCommands = new HashMap<String, ISubCommand>();

    public CommandWorldTools()
    {
        this.registerSubCommand(new SubCommandEntities(this));
    }

    @Override
    public String getCommandName()
    {
        return "wt";
    }

    @Override
    public String getCommandUsage(ICommandSender sender)
    {
        return "/" + this.getCommandName() + " help";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] strArr, BlockPos pos)
    {
        if (strArr.length == 1)
        {
            return getListOfStringsMatchingLastWord(strArr, this.subCommands.keySet());
        }
        else if (this.subCommands.containsKey(strArr[0]))
        {
            ISubCommand command = this.subCommands.get(strArr[0]);
            if (command != null)
            {
                return command.getTabCompletionOptions(server, sender, strArr);
            }
        }
        return null;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] commandArgs) throws CommandException
    {
        if (commandArgs.length > 0)
        {
            if (this.subCommands.containsKey(commandArgs[0]) == true)
            {
                ISubCommand command = this.subCommands.get(commandArgs[0]);
                if (command != null)
                {
                    command.execute(server, sender, commandArgs);
                    return;
                }
            }
            else
            {
                throw new WrongUsageException("Unknown command: /" + this.getCommandName() + " " + commandArgs[0], new Object[0]);
            }
        }

        throw new WrongUsageException("Usage: '" + this.getCommandUsage(sender) + "'", new Object[0]);
    }

    public void registerSubCommand(ISubCommand cmd)
    {
        if (this.subCommands.containsKey(cmd.getCommandName()) == false)
        {
            this.subCommands.put(cmd.getCommandName(), cmd);
        }
    }

    public Set<String> getSubCommandList()
    {
        return this.subCommands.keySet();
    }
}
