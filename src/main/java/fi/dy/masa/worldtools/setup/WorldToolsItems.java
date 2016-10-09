package fi.dy.masa.worldtools.setup;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.GameRegistry;
import fi.dy.masa.worldtools.item.ItemChunkWand;
import fi.dy.masa.worldtools.item.base.ItemWorldTools;
import fi.dy.masa.worldtools.reference.ReferenceNames;

public class WorldToolsItems
{
    public static final ItemWorldTools chunkWand = new ItemChunkWand();

    public static void init()
    {
        registerItem(chunkWand,     ReferenceNames.NAME_ITEM_CHUNK_WAND,            false);
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
