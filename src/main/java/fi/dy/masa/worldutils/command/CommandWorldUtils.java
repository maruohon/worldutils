package fi.dy.masa.worldutils.command;

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
import net.minecraft.util.text.TextComponentString;

public class CommandWorldUtils extends CommandBase
{
    private final Map<String, ISubCommand> subCommands = new HashMap<String, ISubCommand>();

    public CommandWorldUtils()
    {
        this.registerSubCommand(new SubCommandEntities(this));
        this.registerSubCommand(new SubCommandPrintSpawn(this));
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
        return "/" + this.getName() + " help";
    }

    @Override
    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] strArr, BlockPos pos)
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
                return command.getTabCompletions(server, sender, strArr);
            }
        }

        return null;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] commandArgs) throws CommandException
    {
        if (commandArgs.length > 0)
        {
            if (this.subCommands.containsKey(commandArgs[0]))
            {
                ISubCommand command = this.subCommands.get(commandArgs[0]);

                if (command != null)
                {
                    command.execute(server, sender, commandArgs);
                }
            }
            else if (commandArgs[0].equals("help"))
            {
                sender.sendMessage(new TextComponentString("Usage: /" + this.getName() + " " + String.join(", ", this.subCommands.keySet())));
            }
            else
            {
                throw new WrongUsageException("Unknown command: /" + this.getName() + " " + commandArgs[0], new Object[0]);
            }
        }
        else
        {
            throw new WrongUsageException("Usage: '" + this.getUsage(sender) + "'", new Object[0]);
        }
    }

    public void registerSubCommand(ISubCommand cmd)
    {
        if (this.subCommands.containsKey(cmd.getName()) == false)
        {
            this.subCommands.put(cmd.getName(), cmd);
        }
    }

    public Set<String> getSubCommandList()
    {
        return this.subCommands.keySet();
    }
}
