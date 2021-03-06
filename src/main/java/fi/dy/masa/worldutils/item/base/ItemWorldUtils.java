package fi.dy.masa.worldutils.item.base;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.reference.Reference;
import fi.dy.masa.worldutils.reference.ReferenceNames;

public class ItemWorldUtils extends Item
{
    protected final String name;

    public ItemWorldUtils(String name)
    {
        super();

        this.name = name;

        this.setCreativeTab(CreativeTab.WORLD_UTILS_TAB);
        this.setTranslationKey(name);
        this.addItemOverrides();
    }

    public String getItemNameWorldUtils()
    {
        return this.name;
    }

    @Override
    public Item setTranslationKey(String name)
    {
        return super.setTranslationKey(ReferenceNames.getPrefixedName(name));
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || oldStack.equals(newStack) == false;
    }

    public String getBaseItemDisplayName(ItemStack stack)
    {
        // If the item has been renamed, show that name
        if (stack.hasDisplayName() == true)
        {
            NBTTagCompound tag = stack.getTagCompound().getCompoundTag("display");
            return TextFormatting.ITALIC.toString() + tag.getString("Name") + TextFormatting.RESET.toString();
        }

        return super.getItemStackDisplayName(stack);
    }

    /**
     * Custom addInformation() method, which allows selecting a subset of the tooltip strings.
     */
    public void addInformationSelective(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag advanced, boolean verbose)
    {
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag advanced)
    {
        ArrayList<String> tmpList = new ArrayList<String>();
        boolean verbose = WorldUtils.proxy.isShiftKeyDown();

        // "Fresh" items without NBT data: display the tips before the usual tooltip data
        if (stack.getTagCompound() == null)
        {
            this.addTooltips(stack, tmpList, verbose);

            if (verbose == false && tmpList.size() > 2)
            {
                list.add(I18n.format(Reference.MOD_ID + ".tooltip.item.holdshiftfordescription"));
            }
            else
            {
                list.addAll(tmpList);
            }
        }

        tmpList.clear();
        this.addInformationSelective(stack, world, tmpList, advanced, true);

        // If we want the compact version of the tooltip, and the compact list has more than 2 lines, only show the first line
        // plus the "Hold Shift for more" tooltip.
        if (verbose == false && tmpList.size() > 2)
        {
            tmpList.clear();
            this.addInformationSelective(stack, world, tmpList, advanced, false);

            if (tmpList.size() > 0)
            {
                list.add(tmpList.get(0));
            }

            list.add(I18n.format(Reference.MOD_ID + ".tooltip.item.holdshift"));
        }
        else
        {
            list.addAll(tmpList);
        }
    }

    public static void addTooltips(String key, List<String> list, boolean verbose)
    {
        String translated = I18n.format(key);
        // Translation found
        if (translated.equals(key) == false)
        {
            // We currently use '|lf' as a delimiter to split the string into multiple lines
            if (translated.contains("|lf"))
            {
                String[] lines = translated.split(Pattern.quote("|lf"));
                for (String line : lines)
                {
                    list.add(line);
                }
            }
            else
            {
                list.add(translated);
            }
        }
    }

    public void addTooltips(ItemStack stack, List<String> list, boolean verbose)
    {
        addTooltips(this.getTranslationKey(stack) + ".tooltips", list, verbose);
    }

    public ResourceLocation[] getItemVariants()
    {
        return new ResourceLocation[] { ForgeRegistries.ITEMS.getKey(this) };
    }

    public ModelResourceLocation getModelLocation(ItemStack stack)
    {
        return null;
    }

    protected void addItemOverrides()
    {
    }
}
