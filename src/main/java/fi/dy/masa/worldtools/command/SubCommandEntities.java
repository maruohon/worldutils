package fi.dy.masa.worldtools.command;

import java.io.File;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import fi.dy.masa.worldtools.util.EntityReader;
import fi.dy.masa.worldtools.util.FileHelpers;

public class SubCommandEntities extends SubCommand
{
    public SubCommandEntities(CommandWorldTools baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("fix-duplicate-uuids");
        this.subSubCommands.add("list");
        this.subSubCommands.add("read-all");
    }

    @Override
    public String getCommandName()
    {
        return "entities";
    }

    @Override
    protected List<String> getTabCompletionOptionsSub(MinecraftServer server, ICommandSender sender, String[] args)
    {
        return null;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        super.execute(server, sender, args);

        if (args.length >= 2)
        {
            if (args[1].equals("fix-duplicate-uuids"))
            {
            }
            else if (args[1].equals("list"))
            {
                File file = FileHelpers.dumpDataToFile("entities", EntityReader.instance().getEntityList());

                if (file != null)
                {
                    sender.addChatMessage(new TextComponentString("Output written to file " + file.getName()));
                }
            }
            else if (args[1].equals("read-all"))
            {
                if (args.length == 3)
                {
                    int dim = CommandBase.parseInt(args[2]);
                    String output = EntityReader.instance().readEntities(dim);

                    if (StringUtils.isBlank(output) == false)
                    {
                        sender.addChatMessage(new TextComponentString(output));
                    }
                }
                else
                {
                    throw new WrongUsageException("/" + this.getBaseCommand().getCommandName() + " " + args[1] + " <dimension>", new Object[0]);
                }
            }
            else
            {
                throw new WrongUsageException("Unknown sub-command argument '" + args[1] + "'", new Object[0]);
            }
        }
        else
        {
            //throw new WrongUsageException("Unknown sub-command argument '" + args[1] + "'", new Object[0]);
        }
    }
}
