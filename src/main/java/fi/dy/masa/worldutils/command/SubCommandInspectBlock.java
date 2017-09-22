package fi.dy.masa.worldutils.command;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import fi.dy.masa.worldutils.data.BlockTools;

public class SubCommandInspectBlock extends SubCommand
{
    public SubCommandInspectBlock(CommandWorldUtils baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "inspectblock";
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
        return this.getUsageStringCommon() + " <dump | print> <x> <y> <z> [dimension id]";
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos lookPos)
    {
        if (args.length == 1)
        {
            return CommandBase.getListOfStringsMatchingLastWord(args, "dump", "print");
        }
        else
        {
            return CommandBase.getTabCompletionCoordinate(args, 1, lookPos);
        }
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 4 || (args[0].equals("print") == false && args[0].equals("dump") == false))
        {
            throwUsage(this.getUsageString());
        }

        boolean dumpToFile = args[0].equals("dump");
        args = dropFirstStrings(args, 1);
        Entity senderEntity = sender.getCommandSenderEntity();
        BlockPos pos;

        if (senderEntity != null)
        {
            Vec3d senderPos = senderEntity.getPositionVector();

            pos = new BlockPos(((int) CommandBase.parseCoordinate(senderPos.x, args[0], false).getResult()),
                               ((int) CommandBase.parseCoordinate(senderPos.y, args[1], false).getResult()),
                               ((int) CommandBase.parseCoordinate(senderPos.z, args[2], false).getResult()));
        }
        else
        {
            pos = new BlockPos(CommandBase.parseInt(args[0]), CommandBase.parseInt(args[1]), CommandBase.parseInt(args[2]));
        }

        args = dropFirstStrings(args, 3);
        int dim = this.getDimension(this.getUsageString(), args, sender);
        World world = DimensionManager.getWorld(dim);

        if (world != null && world.isValid(pos) == false)
        {
            throwCommand("worldutils.commands.error.invalid_block_position");
        }

        if (BlockTools.inspectBlock(dim, pos, dumpToFile, sender))
        {
            if (dumpToFile)
            {
                this.sendMessage(sender, "worldutils.commands.inspectblock.success.file");
            }
            else
            {
                this.sendMessage(sender, "worldutils.commands.inspectblock.success.console");
            }
        }
        else
        {
            throwCommand("worldutils.commands.error.inspectblock.fail");
        }
    }
}
