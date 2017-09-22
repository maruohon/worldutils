package fi.dy.masa.worldutils.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import fi.dy.masa.worldutils.data.BlockSetter;
import fi.dy.masa.worldutils.util.BlockData;

public class SubCommandSetBlock extends SubCommand
{
    public SubCommandSetBlock(CommandWorldUtils baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "setblock";
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
        return this.getUsageStringCommon() + " <x> <y> <z> <block> [dimension id]";
    }

    @Override
    protected List<String> getTabCompletionsSub(MinecraftServer server, ICommandSender sender, String[] args)
    {
        if (args.length != 4)
        {
            return Collections.emptyList();
        }

        List<String> options = new ArrayList<String>();

        for (ResourceLocation rl : ForgeRegistries.BLOCKS.getKeys())
        {
            options.add(rl.toString());
        }

        return CommandBase.getListOfStringsMatchingLastWord(args, options);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length < 4)
        {
            throwUsage(this.getUsageString());
        }

        Entity senderEntity = sender.getCommandSenderEntity();
        String blockStr = args[3];
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

        args = dropFirstStrings(args, 4);
        int dim = this.getDimension(this.getUsageString(), args, sender);
        World world = DimensionManager.getWorld(dim);

        if (world != null && world.isBlockLoaded(pos))
        {
            throwCommand("worldutils.commands.error.setblock.block_loaded");
        }
        else if (world != null && world.isValid(pos) == false)
        {
            throwCommand("worldutils.commands.error.setblock.invalid_position");
        }

        BlockData blockData = BlockData.parseBlockTypeFromString(blockStr);

        if (blockData != null && blockData.isValid())
        {
            if (BlockSetter.setBlock(dim, pos, blockData))
            {
                this.sendMessage(sender, "worldutils.commands.setblock.success",
                        Integer.valueOf(dim),
                        Integer.valueOf(pos.getX()),
                        Integer.valueOf(pos.getY()),
                        Integer.valueOf(pos.getZ()),
                        blockData.toString());
            }
            else
            {
                throwCommand("worldutils.commands.error.setblock.fail",
                        Integer.valueOf(dim),
                        Integer.valueOf(pos.getX()),
                        Integer.valueOf(pos.getY()),
                        Integer.valueOf(pos.getZ()),
                        blockData.toString());
            }
        }
        else
        {
            throwCommand("worldutils.commands.blockreplace.block.print.invalid", blockStr);
        }
    }

    private int getDimension(String usage, String[] args, ICommandSender sender) throws CommandException
    {
        int dimension = sender instanceof EntityPlayer ? ((EntityPlayer) sender).getEntityWorld().provider.getDimension() : 0;

        if (args.length == 1)
        {
            dimension = CommandBase.parseInt(args[0]);
        }
        else if (args.length > 1)
        {
            throwUsage(usage);
        }

        return dimension;
    }
}
