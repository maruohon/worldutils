package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.List;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import fi.dy.masa.worldutils.data.BlockDump;
import fi.dy.masa.worldutils.data.DataDump;

public class SubCommandDump extends SubCommand
{
    public SubCommandDump(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("blocks");
    }

    @Override
    public String getName()
    {
        return "dump";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        super.execute(server, sender, args);

        if (args.length == 1)
        {
            List<String> data = this.getData(args[0]);

            if (data.isEmpty())
            {
                throwUsage("worldutils.commands.error.unknowncommandvariant", args[0]);
            }

            File file = DataDump.dumpDataToFile(args[0], data);

            if (file != null)
            {
                this.sendMessage(sender, "worldutils.commands.generic.output.to.file", file.getName());
            }
        }
        else
        {
            throwUsage(this.getUsageStringCommon() + " blocks");
        }
    }

    protected List<String> getData(String type)
    {
        if (type.equals("blocks"))
        {
            return BlockDump.getFormattedBlockDump();
        }

        return null;
    }
}
