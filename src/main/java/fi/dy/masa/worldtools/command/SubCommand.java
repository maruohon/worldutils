package fi.dy.masa.worldtools.command;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

public abstract class SubCommand implements ISubCommand
{
    protected final CommandWorldTools baseCommand;
    protected final ArrayList<String> subSubCommands = new ArrayList<String>();

    public SubCommand(CommandWorldTools baseCommand)
    {
        this.baseCommand = baseCommand;
        this.subSubCommands.add("help");
    }

    public CommandWorldTools getBaseCommand()
    {
        return this.baseCommand;
    }

    @Override
    public List<String> getSubCommands()
    {
        return this.subSubCommands;
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length == 2 || (args.length == 3 && args[1].equals("help")))
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, this.getSubCommands());
        }

        return this.getTabCompletionOptionsSub(server, sender, args);
    }

    abstract protected List<String> getTabCompletionOptionsSub(MinecraftServer server, ICommandSender sender, String[] args);

    @Override
    public String getHelp()
    {
        return "Available sub-commands: " + String.join(", ", this.subSubCommands);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        // "/wt command"
        if (args.length == 1)
        {
            sender.sendMessage(new TextComponentString(this.getHelp()));
        }
        // "/wt command [help|unknown]"
        else if (args.length == 2)
        {
            if (args[1].equals("help"))
            {
                sender.sendMessage(new TextComponentString(this.getHelp()));
            }
            else if (this.subSubCommands.contains(args[1]) == false)
            {
                throw new WrongUsageException("Unknown sub-command '" + args[1] + "'", new Object[0]);
            }
        }
        // "/wt command help subsubcommand"
        else if (args.length == 3 && args[1].equals("help"))
        {
            if (args[2].equals("help"))
            {
                sender.sendMessage(new TextComponentString("info.subcommands.help"));
            }
            else if (this.subSubCommands.contains(args[2]))
            {
                sender.sendMessage(new TextComponentString("info.subcommand." + args[0] + ".help." + args[2]));
            }
            else
            {
                throw new WrongUsageException("Unknown sub-command argument " + args[3], new Object[0]);
            }
        }
    }

    public String getUsageStringPre()
    {
        return "/" + this.getBaseCommand().getName() + " " + this.getName() + " ";
    }
}
