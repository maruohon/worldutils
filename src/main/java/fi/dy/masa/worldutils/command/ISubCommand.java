package fi.dy.masa.worldutils.command;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

public interface ISubCommand
{
    /** Returns the command name */
    String getName();

    /** Processes the command */
    void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException;

    /** Adds the tab completion options */
    List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos);

    /** Gets the sub commands for this (sub) command.*/
    List<String> getSubSubCommands();

    /** Gets the sub command's generic help to be sent to the user */
    void printHelpGeneric(ICommandSender sender);

    /** Sends the sub command's in-depth, possibly argument-dependent list of help strings to the user */
    void printFullHelp(ICommandSender sender, String[] args);
}
