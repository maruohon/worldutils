package fi.dy.masa.worldutils.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.FMLControlledNamespacedRegistry;
import net.minecraftforge.fml.common.registry.GameData;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import fi.dy.masa.worldutils.WorldUtils;

public class RegistryUtils
{
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
                    NBTTagCompound tag = nbt.getCompoundTag("FML").getCompoundTag("Registries").getCompoundTag(PersistentRegistryManager.BLOCKS.toString());

                    if (tag.hasNoTags())
                    {
                        sender.sendMessage(new TextComponentTranslation("worldutils.commands.registrycleanup.error.tagsnotfound"));
                        return;
                    }

                    @SuppressWarnings("deprecation")
                    FMLControlledNamespacedRegistry<Block> registry = GameData.getBlockRegistry();
                    NBTTagList list = tag.getTagList("ids", Constants.NBT.TAG_COMPOUND);
                    int size = list.tagCount();
                    int count = 0;

                    for (int i = 0; i < size; i++)
                    {
                        NBTTagCompound tmp = list.getCompoundTagAt(i);

                        if (tmp.hasKey("K", Constants.NBT.TAG_STRING))
                        {
                            ResourceLocation rl = new ResourceLocation(tmp.getString("K"));

                            if (registry.containsKey(rl) == false || registry.isDummied(rl))
                            {
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
                
            }
        }
        else
        {
            sender.sendMessage(new TextComponentTranslation("worldutils.commands.registrycleanup.error.filenotfound", file.getName()));
        }
    }
}
