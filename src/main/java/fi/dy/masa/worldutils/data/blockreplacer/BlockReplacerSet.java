package fi.dy.masa.worldutils.data.blockreplacer;

import java.util.Arrays;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.BlockTools.LoadedType;
import fi.dy.masa.worldutils.util.BlockData;

public class BlockReplacerSet extends BlockReplacerBase
{
    protected final boolean keepListedBlocks;
    protected final BlockData replacementBlockData;

    public BlockReplacerSet(String replacement, boolean keepListedBlocks, LoadedType loaded)
    {
        super(loaded);

        Arrays.fill(this.blocksToReplaceLookup, keepListedBlocks);
        this.keepListedBlocks = keepListedBlocks;
        this.replacementBlockData = BlockData.parseBlockTypeFromString(replacement);

        if (this.replacementBlockData != null && this.replacementBlockData.isValid())
        {
            int id = this.replacementBlockData.getBlockStateId();
            IBlockState state = Block.getStateById(id);
            Arrays.fill(this.replacementBlockStateIds, id);
            Arrays.fill(this.replacementBlockStates, state);
        }
        else
        {
            WorldUtils.logger.warn("Failed to parse block from string '{}'", replacement);

            IBlockState state = Blocks.AIR.getDefaultState();
            int id = Block.getStateId(state);
            Arrays.fill(this.replacementBlockStateIds, id);
            Arrays.fill(this.replacementBlockStates, state);
        }
    }

    public void addBlocksFromBlockStates(List<IBlockState> blockStates)
    {
        boolean replace = this.keepListedBlocks == false;

        for (IBlockState state : blockStates)
        {
            this.blocksToReplaceLookup[Block.getStateId(state)] = replace;
        }

        this.validState |= this.replacementBlockData != null &&
                           this.replacementBlockData.isValid() &&
                           blockStates.isEmpty() == false;
    }

    public void addBlocksFromStrings(List<String> blockEntries)
    {
        boolean replace = this.keepListedBlocks == false;
        boolean hasData = false;

        for (String str : blockEntries)
        {
            BlockData data = BlockData.parseBlockTypeFromString(str);

            if (data != null && data.isValid())
            {
                hasData = true;

                for (int id : data.getBlockStateIds())
                {
                    this.blocksToReplaceLookup[id] = replace;
                }
            }
            else
            {
                WorldUtils.logger.warn("Failed to parse block from string '{}'", str);
            }
        }

        this.validState |= this.replacementBlockData != null &&
                           this.replacementBlockData.isValid() && hasData;
    }
}
