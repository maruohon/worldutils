package fi.dy.masa.worldutils.data;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import com.google.common.collect.Lists;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import fi.dy.masa.worldutils.data.blockreplacer.BlockReplacerPairs;
import fi.dy.masa.worldutils.data.blockreplacer.BlockReplacerSet;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;
import fi.dy.masa.worldutils.event.tasks.TaskWorldProcessor;

public class BlockTools
{
    public enum LoadedType
    {
        ALL,
        UNLOADED,
        LOADED;
    }

    public static void replaceBlocks(int dimension, String replacement, List<String> blockNames, List<IBlockState> blockStates,
            boolean keepListedBlocks, LoadedType loaded, ICommandSender sender) throws CommandException
    {
        BlockReplacerSet replacer = new BlockReplacerSet(replacement, keepListedBlocks, loaded);
        replacer.addBlocksFromBlockStates(blockStates);
        replacer.addBlocksFromStrings(blockNames);

        if (keepListedBlocks)
        {
            replacer.addBlocksFromBlockStates(Lists.newArrayList(Blocks.AIR.getDefaultState()));
        }

        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, replacer, sender), 1);
    }

    public static void replaceBlocksInPairs(int dimension, List<Pair<String, String>> blockPairs,
            LoadedType loaded, ICommandSender sender) throws CommandException
    {
        BlockReplacerPairs replacer = new BlockReplacerPairs(loaded);
        replacer.addBlockPairs(blockPairs);
        TaskScheduler.getInstance().scheduleTask(new TaskWorldProcessor(dimension, replacer, sender), 1);
    }
}
