package fi.dy.masa.worldutils.data.blockreplacer;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.command.ICommandSender;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.GameData;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.BlockTools.LoadedType;

public class BlockIDRemapper extends BlockReplacerBase
{
    private Map<String, Integer> registryOld = new HashMap<>();
    private Map<String, Integer> registryNew = new HashMap<>();

    public BlockIDRemapper()
    {
        super(LoadedType.UNLOADED);

        Arrays.fill(this.blocksToReplaceLookup, false);
        this.clearTileData = false;
    }

    public boolean parseBlockRegistries(File levelOld, File levelNew, ICommandSender sender)
    {
        boolean addedData = false;

        if (this.parseBlockRegistry(levelOld, this.registryOld, sender) &&
            this.parseBlockRegistry(levelNew, this.registryNew, sender))
        {
            final boolean neid = WorldUtils.isModLoadedNEID();
            final int metaShift = neid ? 16 : 12;
            final int airId = Block.getStateId(Blocks.AIR.getDefaultState());

            for (Map.Entry<String, Integer> entry : this.registryOld.entrySet())
            {
                Integer blockIdNewObj = this.registryNew.get(entry.getKey());
                int blockIdOld = entry.getValue();
                int blockIdNew = blockIdNewObj != null ? blockIdNewObj.intValue() : airId;

                for (int meta = 0; meta < 16; ++meta)
                {
                    int indexOld = (meta << metaShift) | blockIdOld;
                    int indexNew = (meta << metaShift) | blockIdNew;
                    this.blocksToReplaceLookup[indexOld] = true;
                    this.replacementBlockStateIds[indexOld] = indexNew;
                }

                addedData = true;
            }
        }

        this.validState = addedData;

        return this.validState;
    }

    private boolean parseBlockRegistry(File file, Map<String, Integer> map, ICommandSender sender)
    {
        if (file.exists() && file.isFile() && file.canRead())
        {
            try
            {
                FileInputStream is = new FileInputStream(file);
                NBTTagCompound nbt = CompressedStreamTools.readCompressed(is);
                is.close();

                if (nbt != null)
                {
                    NBTTagCompound tag = nbt.getCompoundTag("FML").getCompoundTag("Registries").getCompoundTag(GameData.BLOCKS.toString());

                    if (tag.isEmpty())
                    {
                        sender.sendMessage(new TextComponentTranslation("worldutils.commands.blockremapper.error.tagsnotfound"));
                        return false;
                    }

                    NBTTagList list = tag.getTagList("ids", Constants.NBT.TAG_COMPOUND);
                    final int size = list.tagCount();

                    for (int i = 0; i < size; ++i)
                    {
                        NBTTagCompound tmp = list.getCompoundTagAt(i);

                        if (tmp.hasKey("K", Constants.NBT.TAG_STRING) &&
                            tmp.hasKey("V", Constants.NBT.TAG_INT))
                        {
                            map.put(tmp.getString("K"), tmp.getInteger("V"));
                        }
                    }

                    return true;
                }
            }
            catch (Exception e)
            {
                return false;
            }
        }

        return false;
    }
}
