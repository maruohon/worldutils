package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.util.RegistryUtils;

public class SubCommandRegistry extends SubCommand
{
    public SubCommandRegistry(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("remove-missing-blocks");
    }

    @Override
    public String getName()
    {
        return "registry";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.usage", this.getUsageStringCommon() + " remove-missing-blocks [filename]");
    }

    @Override
    public List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if (args.length < 1)
        {
            return Collections.emptyList();
        }

        String cmd = args[0];
        args = CommandWorldUtils.dropFirstStrings(args, 1);

        if (cmd.equals("remove-missing-blocks") && args.length == 1)
        {
            File dir = new File(WorldUtils.configDirPath);
            return CommandBase.getListOfStringsMatchingLastWord(args, dir.list());
        }

        return super.getTabCompletions(server, sender, args, targetPos);
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

        if (cmd.equals("remove-missing-blocks"))
        {
            String fileName = "level.dat";

            if (args.length == 1)
            {
                fileName = args[0];
            }

            RegistryUtils.removeDummyBlocksFromRegistry(fileName, sender);
        }
        else
        {
            this.printHelpGeneric(sender);
        }
    }
}
