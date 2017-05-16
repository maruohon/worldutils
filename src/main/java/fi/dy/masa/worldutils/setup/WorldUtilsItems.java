package fi.dy.masa.worldutils.setup;

import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.worldutils.item.ItemChunkWand;
import fi.dy.masa.worldutils.item.base.ItemWorldUtils;
import fi.dy.masa.worldutils.reference.Reference;
import fi.dy.masa.worldutils.reference.ReferenceNames;

public class WorldUtilsItems
{
    public static final ItemWorldUtils CHUNK_WAND = new ItemChunkWand();

    public static void init()
    {
        registerItem(CHUNK_WAND, ReferenceNames.NAME_ITEM_CHUNK_WAND, Configs.disableChunkWand);
    }

    private static void registerItem(ItemWorldUtils item, String registryName, boolean isDisabled)
    {
        if (isDisabled == false)
        {
            item.setRegistryName(Reference.MOD_ID + ":" + registryName);
            GameRegistry.register(item);
        }
        else
        {
            item.setEnabled(false);
        }
    }
}
