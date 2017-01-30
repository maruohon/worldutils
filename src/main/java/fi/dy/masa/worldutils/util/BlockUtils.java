package fi.dy.masa.worldutils.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;
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
}
