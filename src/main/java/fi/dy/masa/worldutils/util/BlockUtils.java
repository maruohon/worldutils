package fi.dy.masa.worldutils.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

public class BlockUtils
{
    public static List<String> getAllBlockNames()
    {
        List<String> names = new ArrayList<String>();

        for (ResourceLocation rl : Block.REGISTRY.getKeys())
        {
            names.add(rl.toString());
        }

        return names;
    }

    public static List<IBlockState> getAllBlockStatesInMod(String modId)
    {
        List<IBlockState> list = new ArrayList<IBlockState>();

        for (ResourceLocation rl : Block.REGISTRY.getKeys())
        {
            if (rl.getResourceDomain().equals(modId))
            {
                Block block = Block.REGISTRY.getObject(rl);
                list.addAll(block.getBlockState().getValidStates());
            }
        }

        return list;
    }

    public static List<String> getAllBlockNamesInMod(String modId)
    {
        List<String> list = new ArrayList<String>();

        for (ResourceLocation rl : Block.REGISTRY.getKeys())
        {
            if (rl.getResourceDomain().equals(modId))
            {
                list.add(rl.toString());
            }
        }

        return list;
    }
}
