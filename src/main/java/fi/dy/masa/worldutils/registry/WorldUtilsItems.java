package fi.dy.masa.worldutils.registry;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import fi.dy.masa.worldutils.config.Configs;
import fi.dy.masa.worldutils.item.ItemChunkWand;
import fi.dy.masa.worldutils.item.base.ItemWorldUtils;
import fi.dy.masa.worldutils.reference.Reference;
import fi.dy.masa.worldutils.reference.ReferenceNames;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public class WorldUtilsItems
{
    @GameRegistry.ObjectHolder(Reference.MOD_ID + ":" + ReferenceNames.NAME_ITEM_CHUNK_WAND)
    public static final ItemWorldUtils CHUNK_WAND = null;

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        IForgeRegistry<Item> registry = event.getRegistry();

        registerItem(registry, new ItemChunkWand(ReferenceNames.NAME_ITEM_CHUNK_WAND), Configs.disableChunkWand);
    }

    private static void registerItem(IForgeRegistry<Item> registry, ItemWorldUtils item, boolean isDisabled)
    {
        if (isDisabled == false)
        {
            item.setRegistryName(Reference.MOD_ID + ":" + item.getItemNameWorldUtils());
            registry.register(item);
        }
    }
}
