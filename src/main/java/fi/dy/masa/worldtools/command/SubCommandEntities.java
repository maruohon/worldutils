package fi.dy.masa.worldtools.command;

import java.io.File;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import fi.dy.masa.worldtools.data.EntityReader;
import fi.dy.masa.worldtools.util.FileUtils;

public class SubCommandEntities extends SubCommand
{
    public SubCommandEntities(CommandWorldTools baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("list");
        this.subSubCommands.add("list-duplicates-all");
        this.subSubCommands.add("list-duplicates-only");
        this.subSubCommands.add("read-all");
        this.subSubCommands.add("remove-duplicate-uuids");
    }

    @Override
    public String getName()
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
            if (args[1].equals("list"))
            {
                File file = FileUtils.dumpDataToFile("entities", EntityReader.instance().getAllEntitiesOutput(true));

                if (file != null)
                {
                    sender.sendMessage(new TextComponentString("Output written to file " + file.getName()));
                }
            }
            else if (args[1].equals("list-duplicates-all") || args[1].equals("list-duplicates-only"))
            {
                File file;

                if (args[1].equals("list-duplicates-all"))
                {
                    file = FileUtils.dumpDataToFile("entity_duplicates_all", EntityReader.instance().getDuplicateEntitiesOutput(true, true));
                }
                else
                {
                    file = FileUtils.dumpDataToFile("entity_duplicates_only", EntityReader.instance().getDuplicateEntitiesOutput(false, true));
                }

                if (file != null)
                {
                    sender.sendMessage(new TextComponentString("Output written to file " + file.getName()));
                }
            }
            else if (args[1].equals("read-all"))
            {
                int dimension = sender instanceof EntityPlayer ? ((EntityPlayer) sender).dimension : 0;

                if (args.length == 3)
                {
                    dimension = CommandBase.parseInt(args[2]);
                }
                else if (args.length > 3)
                {
                    throw new WrongUsageException(this.getUsageStringPre() + args[1] + " [dimension]", new Object[0]);
                }

                EntityReader.instance().readEntities(dimension, sender);
            }
            else if (args[1].equals("remove-duplicate-uuids"))
            {
                int dimension = sender instanceof EntityPlayer ? ((EntityPlayer) sender).dimension : 0;

                if (args.length == 3)
                {
                    dimension = CommandBase.parseInt(args[2]);
                }
                else if (args.length > 3)
                {
                    throw new WrongUsageException(this.getUsageStringPre() + args[1] + " [dimension]", new Object[0]);
                }

                String output = EntityReader.instance().removeAllDuplicateEntities(dimension, false, sender);
                sender.sendMessage(new TextComponentString(output));
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
