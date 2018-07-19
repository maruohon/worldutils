package fi.dy.masa.worldutils.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.util.MethodHandleUtils.UnableToFindMethodHandleException;

public class RegistryUtils
{
    private static MethodHandle methodHandle_ForgeRegistry_isDummied;

    static
    {
        try
        {
            methodHandle_ForgeRegistry_isDummied = MethodHandleUtils.getMethodHandleVirtual(
                    ForgeRegistry.class, new String[] { "isDummied" }, ResourceLocation.class);
        }
        catch (UnableToFindMethodHandleException e)
        {
            WorldUtils.logger.error("RegistryUtils: Failed to get MethodHandle for ForgeRegistry#isDummied()", e);
        }
    }

    public static <K extends IForgeRegistryEntry<K>> boolean isDummied(IForgeRegistry<K> registry, ResourceLocation rl)
    {
        try
        {
            return (boolean) methodHandle_ForgeRegistry_isDummied.invoke(registry, rl);
        }
        catch (Throwable t)
        {
            WorldUtils.logger.error("RegistryUtils: Error while trying invoke ForgeRegistry#isDummied()", t);
            return false;
        }
    }

    public static void removeDummyBlocksFromRegistry(String fileName, ICommandSender sender)
    {
        File file;

        if (StringUtils.isBlank(fileName))
        {
            file = new File(WorldUtils.configDirPath, "level.dat");
        }
        else
        {
            file = new File(WorldUtils.configDirPath, fileName);
        }

        if (file.exists() && file.isFile())
        {
            try
            {
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(new FileInputStream(file));

                if (nbt != null)
                {
                    NBTTagCompound tag = nbt.getCompoundTag("FML").getCompoundTag("Registries").getCompoundTag(GameData.BLOCKS.toString());

                    if (tag.isEmpty())
                    {
                        sender.sendMessage(new TextComponentTranslation("worldutils.commands.registrycleanup.error.tagsnotfound"));
                        return;
                    }

                    IForgeRegistry<Block> registry = ForgeRegistries.BLOCKS;
                    NBTTagList list = tag.getTagList("ids", Constants.NBT.TAG_COMPOUND);
                    int size = list.tagCount();
                    int count = 0;

                    for (int i = 0; i < size; i++)
                    {
                        NBTTagCompound tmp = list.getCompoundTagAt(i);

                        if (tmp.hasKey("K", Constants.NBT.TAG_STRING))
                        {
                            String key = tmp.getString("K");
                            ResourceLocation rl = new ResourceLocation(key);

                            if (registry.containsKey(rl) == false || isDummied(registry, rl))
                            {
                                WorldUtils.logger.info("Removing missing or dummied block registry entry '{}'", key);
                                list.removeTag(i);
                                i--;
                                count++;
                            }
                        }
                    }

                    tag.setTag("dummied", new NBTTagList());

                    if (count > 0)
                    {
                        CompressedStreamTools.writeCompressed(nbt, new FileOutputStream(file));
                    }

                    sender.sendMessage(new TextComponentTranslation("worldutils.commands.registrycleanup.info.complete",
                            Integer.valueOf(count), file.getName()));
                    WorldUtils.logger.info("Removed {} missing or dummied block entries from the registry in '{}'", count, file.getName());
                }
            }
            catch (IOException e)
            {
                WorldUtils.logger.info("Exception while trying to remove dummied block entries from the registry", e);
            }
        }
        else
        {
            sender.sendMessage(new TextComponentTranslation("worldutils.commands.registrycleanup.error.filenotfound", file.getName()));
        }
    }
}
