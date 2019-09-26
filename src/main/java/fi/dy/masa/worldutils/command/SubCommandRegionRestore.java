package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.DataDump;
import fi.dy.masa.worldutils.util.FileUtils;
import fi.dy.masa.worldutils.util.RegionFileUtils;

public class SubCommandRegionRestore extends SubCommand
{
    public static final File DIR = new File(new File(WorldUtils.configDirPath), "worlds");

    public SubCommandRegionRestore(CommandWorldUtils baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "region-restore";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.runhelpforallcommands", this.getUsageStringCommon() + " help");
    }

    @Override
    public void printFullHelp(ICommandSender sender, String[] args)
    {
        sender.sendMessage(new TextComponentString(this.getUsageString()));
    }

    private String getUsageString()
    {
        return this.getUsageStringCommon() + " <check | restore> <file_name_in> [file_name_in ...]";
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos lookPos)
    {
        if (args.length < 1)
        {
            return Collections.emptyList();
        }

        if (args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "check", "restore");
        }

        return CommandBase.getListOfStringsMatchingLastWord(args, this.getFileNames());
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 2)
        {
            throwUsage(this.getUsageString());
        }

        boolean check = args[0].equals("check");
        boolean restore = args[0].equals("restore");

        if (check == false && restore == false)
        {
            throwUsage(this.getUsageString());
        }

        for (int i = 1; i < args.length; ++i)
        {
            String name = args[i];
            File file = new File(DIR, name);

            if (file.exists() && file.isFile() && file.canRead())
            {
                if (check)
                {
                    File fileOut = DataDump.dumpDataToFile("region_file_possible_chunks_" + file.getName(), RegionFileUtils.findPossibleChunksAligned(file));

                    if (fileOut != null)
                    {
                        CommandWorldUtils.sendClickableLinkMessage(sender, "Output written to file %s", fileOut);
                    }
                }
                else if (restore)
                {
                    String fileNameOut = "restored_" + DataDump.getDateString() + "_" + name;
                    int count = RegionFileUtils.tryRestoreChunksFromRegion(file, new File(DIR, fileNameOut));

                    if (count > 0)
                    {
                        this.sendMessage(sender, String.format("Restored %d chunks from region file '%s'", count, name));
                    }
                    else
                    {
                        this.sendMessage(sender, String.format("Failed to restore any chunks from region file '%s'", count, name));
                    }
                }
            }
            else
            {
                sender.sendMessage(new TextComponentString("No such file: '" + name + "' in " + DIR.getPath()));
            }
        }
    }

    private List<String> getFileNames()
    {
        if (DIR.isDirectory())
        {
            String[] names = DIR.list(FileUtils.FILTER_FILES);
            return Arrays.asList(names);
        }

        return Collections.emptyList();
    }
}
