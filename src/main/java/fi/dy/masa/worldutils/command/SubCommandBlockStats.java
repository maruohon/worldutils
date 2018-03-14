package fi.dy.masa.worldutils.command;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import com.google.common.collect.Maps;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.BlockStats;
import fi.dy.masa.worldutils.data.DataDump;
import fi.dy.masa.worldutils.data.DataDump.Format;
import fi.dy.masa.worldutils.event.tasks.TaskRegionDirectoryProcessor;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.util.FileUtils;

public class SubCommandBlockStats extends SubCommand
{
    private final Map<UUID, BlockStats> blockStats = Maps.newHashMap();
    private final BlockStats blockStatsConsole = new BlockStats();

    public SubCommandBlockStats(CommandWorldUtils baseCommand)
    {
        super(baseCommand);

        this.subSubCommands.add("count");
        this.subSubCommands.add("count-append");
        this.subSubCommands.add("dump");
        this.subSubCommands.add("dump-csv");
        this.subSubCommands.add("query");
        this.subSubCommands.add("stoptask");
    }

    @Override
    public String getName()
    {
        return "blockstats";
    }

    @Override
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.runhelpforallcommands", this.getUsageStringCommon() + " help");
    }

    @Override
    public void printFullHelp(ICommandSender sender, String[] args)
    {
        this.printUsageCount(sender);
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " <dump | query> [modid:blockname[:meta] modid:blockname[:meta] ...]"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " stoptask"));
    }

    private void printUsageCount(ICommandSender sender)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " count[-append] all-chunks <path/to/regiondir>"));
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " count[-append] chunk-radius <radius> <path/to/regiondir> [x y z (of center)]"));
    }

    private void printUsageDump(ICommandSender sender)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " dump [modid:blockname[:meta] modid:blockname[:meta] ...]"));
    }

    private void printUsageQuery(ICommandSender sender)
    {
        sender.sendMessage(new TextComponentString(this.getUsageStringCommon() + " query [modid:blockname[:meta] modid:blockname[:meta] ...]"));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos)
    {
        if (args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, this.subSubCommands);
        }
        else if (args.length == 2)
        {
            if (args[0].equals("dump") || args[0].equals("dump-csv") || args[0].equals("query"))
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, ForgeRegistries.BLOCKS.getKeys());
            }
            else if (args[0].equals("count") || args[0].equals("count-append"))
            {
                return CommandBase.getListOfStringsMatchingLastWord(args, "all-chunks", "chunk-radius");
            }
        }
        else if (args.length == 3 && args[1].equals("all-chunks"))
        {
            return FileUtils.getPossibleFileNameCompletions(args[2]);
        }
        else if (args.length == 4 && args[1].equals("chunk-radius"))
        {
            return FileUtils.getPossibleFileNameCompletions(args[3]);
        }

        return Collections.emptyList();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        // "/tellme bockstats"
        if (args.length < 1)
        {
            this.sendMessage(sender, "Usage:");
            this.printFullHelp(sender, args);
            return;
        }

        super.execute(server, sender, args);

        String cmd = args[0];
        BlockStats blockStats = sender instanceof EntityPlayer ? this.getBlockStatsForPlayer((EntityPlayer) sender) : this.blockStatsConsole;

        // "/tellme blockstats count ..."
        if ((cmd.equals("count") || cmd.equals("count-append")) && args.length >= 2)
        {
            if (TaskScheduler.getInstance().hasTasks())
            {
                throwCommand("worldutils.commands.error.taskalreadyrunning");
            }

            // Possible command formats are:
            // count all-chunks <path>
            // count chunk-radius <radius> <path> [x y z (of the center)]
            String type = args[1];
            args = dropFirstStrings(args, 2);
            blockStats.setAppend(cmd.equals("count-append"));

            // Get the world - either the player's current world, or the one based on the provided dimension ID
            //World world = this.getWorld(type, args, sender, server);
            //BlockPos pos = sender instanceof EntityPlayer ? sender.getPosition() : WorldUtils.getSpawnPoint(world);
            String pre = this.getUsageStringCommon();

            if (type.equals("all-chunks") && args.length == 1)
            {
                File regionDir = new File(args[0]);

                if (regionDir.exists() && regionDir.isDirectory())
                {
                    this.sendMessage(sender, "Counting blocks...");
                    blockStats.init(0);

                    TaskScheduler.getInstance().scheduleTask(new TaskRegionDirectoryProcessor(regionDir, blockStats, sender, 50), 1);
                }
                else
                {
                    WorldUtils.logger.warn("The directory '{}' doesn't exist", regionDir.getPath());
                    this.sendMessage(sender, "The directory '" + regionDir.getPath() + "' doesn't exist");
                }
            }
            /*
            // count chunk-radius <radius> <path> [x y z (of the center)]
            else if (type.equals("chunk-radius") && (args.length == 2 || args.length == 5))
            {
                if (args.length == 5)
                {
                    int x = CommandBase.parseInt(args[2]);
                    int y = CommandBase.parseInt(args[3]);
                    int z = CommandBase.parseInt(args[4]);
                    pos = new BlockPos(x, y, z);
                }

                int radius = 0;

                try
                {
                    radius = Integer.parseInt(args[0]);
                }
                catch (NumberFormatException e)
                {
                    throw new WrongUsageException(pre + " count chunk-radius <radius> [dimension] [x y z (of the center)]");
                }

                int chunkCount = (radius * 2 + 1) * (radius * 2 + 1);

                this.sendMessage(sender, "Loading all the " + chunkCount + " chunks in the given radius of " + radius + " chunks ...");

                List<Chunk> chunks = WorldUtils.loadAndGetChunks(world, pos, radius);

                this.sendMessage(sender, "Counting blocks in the selected " + chunks.size() + " chunks...");

                blockStats.processChunks(chunks);

                this.sendMessage(sender, "Done");
            }
            */
            else
            {
                this.printUsageCount(sender);
                throw new CommandException("Invalid (number of?) arguments!");
            }
        }
        // "/tellme blockstats query ..." or "/tellme blockstats dump ..."
        else if (cmd.equals("query") || cmd.equals("dump") || cmd.equals("dump-csv"))
        {
            List<String> lines;
            Format format = cmd.equals("dump-csv") ? Format.CSV : Format.ASCII;

            // We have some filters specified
            if (args.length > 1)
            {
                lines = blockStats.query(format, Arrays.asList(dropFirstStrings(args, 1)));
            }
            else
            {
                lines = blockStats.queryAll(format);
            }

            if (cmd.equals("query"))
            {
                DataDump.printDataToLogger(lines);
                this.sendMessage(sender, "Command output printed to console");
            }
            else
            {
                File file = DataDump.dumpDataToFile("block_stats", lines);
                CommandWorldUtils.sendClickableLinkMessage(sender, "Output written to file %s", file);
            }
        }
        else if (cmd.equals("stoptask"))
        {
            if (TaskScheduler.getInstance().removeTask(TaskRegionDirectoryProcessor.class))
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
            this.sendMessage(sender, "Usage:");
            this.printUsageCount(sender);
            this.printUsageDump(sender);
            this.printUsageQuery(sender);
        }
    }

    private BlockStats getBlockStatsForPlayer(EntityPlayer player)
    {
        BlockStats stats = this.blockStats.get(player.getUniqueID());

        if (stats == null)
        {
            stats = new BlockStats();
            this.blockStats.put(player.getUniqueID(), stats);
        }

        return stats;
    }
}
