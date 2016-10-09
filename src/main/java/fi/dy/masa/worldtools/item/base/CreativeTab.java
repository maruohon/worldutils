package fi.dy.masa.worldtools.item.base;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import fi.dy.masa.worldtools.reference.Reference;
import fi.dy.masa.worldtools.setup.WorldToolsItems;

public class CreativeTab
{
    public static final CreativeTabs ENDER_UTILITIES_TAB = new CreativeTabs(Reference.MOD_ID)
    {
        @SideOnly(Side.CLIENT)
        @Override
        public Item getTabIconItem()
        {
            return WorldToolsItems.chunkWand;
        }

        @SideOnly(Side.CLIENT)
        @Override
        public String getTranslatedTabLabel()
        {
            return Reference.MOD_NAME;
        }
    };
}
