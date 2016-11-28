package fi.dy.masa.worldutils.setup;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.worldutils.item.ItemChunkWand;
import fi.dy.masa.worldutils.item.base.ItemWorldUtils;
import fi.dy.masa.worldutils.reference.ReferenceNames;

public class WorldUtilsItems
{
    public static final ItemWorldUtils chunkWand = new ItemChunkWand();

    public static void init()
    {
        registerItem(chunkWand, ReferenceNames.NAME_ITEM_CHUNK_WAND, Configs.disableChunkWand);
    }

    private static void registerItem(Item item, String registryName, boolean isDisabled)
    {
        if (isDisabled == false)
        {
            item.setRegistryName(registryName);
            GameRegistry.register(item);
        }
    }
}
