package fi.dy.masa.worldutils.command;

import java.util.List;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.WorldServer;

public class SubCommandPrintSpawn extends SubCommand
{
    public SubCommandPrintSpawn(CommandWorldUtils baseCommand)
    {
        super(baseCommand);
    }

    @Override
    public String getName()
    {
        return "printspawn";
    }

    @Override
    protected List<String> getTabCompletionOptionsSub(MinecraftServer server, ICommandSender sender, String[] args)
    {
        return null;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        if (args.length >= 1)
        {
            int dimension = sender instanceof EntityPlayer ? ((EntityPlayer) sender).getEntityWorld().provider.getDimension() : 0;

            if (args.length == 2)
            {
                dimension = CommandBase.parseInt(args[1]);
            }
            else if (args.length > 2)
            {
                throw new WrongUsageException(this.getUsageStringPre() + args[0] + " [dimension]", new Object[0]);
            }

            WorldServer world = server.worldServerForDimension(dimension);

            if (world != null)
            {
                BlockPos pos = world.getSpawnPoint();
                sender.sendMessage(new TextComponentString(String.format("The world spawn is at x: %d, y: %d, z: %d", pos.getX(), pos.getY(), pos.getZ())));
            }
        }
        else
        {
            throw new WrongUsageException(this.getUsageStringPre() + args[0] + " [dimension]", new Object[0]);
        }
    }
}
