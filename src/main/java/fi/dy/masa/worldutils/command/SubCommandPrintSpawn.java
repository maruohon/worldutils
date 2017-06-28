package fi.dy.masa.worldutils.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
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
    public void printHelpGeneric(ICommandSender sender)
    {
        this.sendMessage(sender, "worldutils.commands.help.generic.usage", this.getUsageStringCommon() + " [dimension id]");
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        int dimension = sender instanceof EntityPlayer ? sender.getEntityWorld().provider.getDimension() : 0;

        if (args.length == 1)
        {
            dimension = CommandBase.parseInt(args[0]);
        }
        else if (args.length > 1)
        {
            throwCommand("worldutils.commands.help.generic.usage", this.getUsageStringCommon() + " [dimension id]");
        }

        WorldServer world = server.getWorld(dimension);

        if (world != null)
        {
            BlockPos pos = world.getSpawnCoordinate();

            if (pos == null)
            {
                pos = world.getSpawnPoint();
            }

            this.sendMessage(sender, "worldutils.commands.printspawn.print",
                    Integer.valueOf(pos.getX()), Integer.valueOf(pos.getY()), Integer.valueOf(pos.getZ()));
        }
        else
        {
            throwNumber("worldutils.commands.error.dimensionnotloaded", Integer.valueOf(dimension));
        }
    }
}
