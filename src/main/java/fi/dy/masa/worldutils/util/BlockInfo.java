package fi.dy.masa.worldutils.util;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import com.google.common.collect.UnmodifiableIterator;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.DataDump;

public class BlockInfo
{
    private static final Field field_REGISTRY = ReflectionHelper.findField(TileEntity.class, "field_190562_f", "REGISTRY");

    private static List<String> getBasicBlockInfo(int blockId, int meta, boolean hasTileNBT)
    {
        List<String> lines = new ArrayList<String>();
        IBlockState state = Block.getStateById(meta << 12 | blockId);
        Block block = state.getBlock();

        ItemStack stack = new ItemStack(block, 1, block.damageDropped(state));
        String name = ForgeRegistries.BLOCKS.getKey(block).toString();
        String dname;

        if (stack.isEmpty() == false)
        {
            dname = stack.getDisplayName();
        }
        // Blocks that are not obtainable/don't have an ItemBlock
        else
        {
            dname = name;
        }

        boolean shouldHaveTE = block.hasTileEntity(state);

        if (hasTileNBT == shouldHaveTE)
        {
            if (hasTileNBT)
            {
                lines.add(String.format("%s (%s - %d:%d) has a TileEntity", dname, name, blockId, meta));
            }
            else
            {
                lines.add(String.format("%s (%s - %d:%d) no TileEntity", dname, name, blockId, meta));
            }
        }
        else
        {
            if (hasTileNBT)
            {
                lines.add(String.format("%s (%s - %d:%d) !! is not supposed to have a TileEntity, but there is TileEntity NBT in the chunk data !!",
                        dname, name, blockId, meta));
            }
            else
            {
                lines.add(String.format("%s (%s - %d:%d) !! is supposed to have a TileEntity, but there isn't TileEntity NBT in the chunk !!",
                        dname, name, blockId, meta));
            }
        }

        return lines;
    }

    private static List<String> getFullBlockInfo(int blockId, int meta, NBTTagCompound tileNBT, int modified)
    {
        List<String> lines = getBasicBlockInfo(blockId, meta, tileNBT != null);
        IBlockState state = Block.getStateById(meta << 12 | blockId);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        String str = sdf.format(new Date((long) modified * 1000L));
        lines.add("Chunk last modified on " + str);
        lines.add("Block class: " + state.getBlock().getClass().getName());

        if (state.getProperties().size() > 0)
        {
            lines.add("IBlockState properties, excluding getActualState():");

            UnmodifiableIterator<Entry<IProperty<?>, Comparable<?>>> iter = state.getProperties().entrySet().iterator();

            while (iter.hasNext())
            {
                Entry<IProperty<?>, Comparable<?>> entry = iter.next();
                lines.add(entry.getKey().toString() + ": " + entry.getValue().toString());
            }
        }
        else
        {
            lines.add("IBlockState properties: <none>");
        }

        if (tileNBT != null)
        {
            TileEntity te = createTileEntity(tileNBT.getString("id"));

            if (te != null)
            {
                lines.add("TileEntity class: " + te.getClass().getName());
            }

            lines.add("");
            lines.add("TileEntity NBT (from Chunk NBT data on disk):");
            NBTFormatter.getPrettyFormattedNBT(lines, tileNBT);
        }

        return lines;
    }

    @Nullable
    private static TileEntity createTileEntity(String id)
    {
        try
        {
            @SuppressWarnings("unchecked")
            RegistryNamespaced <ResourceLocation, Class <? extends TileEntity>> registry = (RegistryNamespaced <ResourceLocation, Class <? extends TileEntity>>) field_REGISTRY.get(null);
            Class <? extends TileEntity> clazz = registry.getObject(new ResourceLocation(id));

            if (clazz != null)
            {
                return clazz.newInstance();
            }
        }
        catch (Throwable t)
        {
            WorldUtils.logger.warn("Failed to create block entity '{}'", id, t);
        }

        return null;
    }

    private static void printBasicBlockInfoToChat(int blockId, int meta, boolean hasTileNBT, ICommandSender sender)
    {
        for (String line : getBasicBlockInfo(blockId, meta, hasTileNBT))
        {
            sender.sendMessage(new TextComponentString(line));
        }
    }

    private static void printBlockInfoToConsole(int blockId, int meta, NBTTagCompound tileNBT, int modified)
    {
        List<String> lines = getFullBlockInfo(blockId, meta, tileNBT, modified);

        for (String line : lines)
        {
            WorldUtils.logger.info(line);
        }
    }

    private static void dumpBlockInfoToFile(int blockId, int meta, NBTTagCompound tileNBT, ICommandSender sender, int modified)
    {
        File f = DataDump.dumpDataToFile("block_and_tileentity_data", getFullBlockInfo(blockId, meta, tileNBT, modified));
        sender.sendMessage(new TextComponentString("Output written to file " + f.getName()));
    }

    public static void outputBlockInfo(final int blockId, final int meta, NBTTagCompound tileNBT,
            boolean dumpToFile, ICommandSender sender, int modified)
    {
        printBasicBlockInfoToChat(blockId, meta, tileNBT != null, sender);

        if (dumpToFile)
        {
            dumpBlockInfoToFile(blockId, meta, tileNBT, sender, modified);
        }
        else
        {
            printBlockInfoToConsole(blockId, meta, tileNBT, modified);
        }
    }
}
