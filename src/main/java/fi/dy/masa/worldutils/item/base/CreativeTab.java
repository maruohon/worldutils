package fi.dy.masa.worldutils.item.base;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.worldutils.reference.Reference;
import fi.dy.masa.worldutils.setup.WorldUtilsItems;

public class CreativeTab
{
    public static final CreativeTabs WORLD_UTILS_TAB = new CreativeTabs(Reference.MOD_ID)
    {
        @SideOnly(Side.CLIENT)
        @Override
        public Item getTabIconItem()
        {
            return WorldUtilsItems.CHUNK_WAND;
        }

        @SideOnly(Side.CLIENT)
        @Override
        public String getTranslatedTabLabel()
        {
            return Reference.MOD_NAME;
        }
    };
}
