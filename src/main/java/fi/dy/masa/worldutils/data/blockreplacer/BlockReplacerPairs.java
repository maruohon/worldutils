package fi.dy.masa.worldutils.data.blockreplacer;

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import net.minecraft.block.Block;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.BlockTools.LoadedType;
import fi.dy.masa.worldutils.util.BlockData;

public class BlockReplacerPairs extends BlockReplacerBase
{
    public BlockReplacerPairs(LoadedType loaded)
    {
        super(loaded);

        Arrays.fill(this.blocksToReplaceLookup, false);
    }

    public void addBlockPairs(List<Pair<String, String>> blockPairs)
    {
        boolean addedData = false;

        for (Pair<String, String> pair : blockPairs)
        {
            BlockData dataFrom = BlockData.parseBlockTypeFromString(pair.getLeft());
            BlockData dataTo   = BlockData.parseBlockTypeFromString(pair.getRight());

            if (dataFrom != null && dataFrom.isValid() && dataTo != null && dataTo.isValid())
            {
                int idFrom = dataFrom.getBlockStateId();
                int idTo = dataTo.getBlockStateId();
                this.blocksToReplaceLookup[idFrom] = true;
                this.replacementBlockStateIds[idFrom] = idTo;
                this.replacementBlockStates[idFrom] = Block.getStateById(idTo);
                addedData = true;
            }
            else
            {
                WorldUtils.logger.warn("Failed to parse block from string '{}' or '{}'", pair.getLeft(), pair.getRight());
            }
        }

        this.validState |= addedData;
    }
}
