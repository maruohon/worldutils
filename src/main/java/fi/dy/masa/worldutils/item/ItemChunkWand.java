package fi.dy.masa.worldutils.item;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import fi.dy.masa.worldutils.item.base.IKeyBound;
import fi.dy.masa.worldutils.item.base.ItemWorldUtils;
import fi.dy.masa.worldutils.network.MessageChunkChanges;
import fi.dy.masa.worldutils.network.PacketHandler;
import fi.dy.masa.worldutils.reference.HotKeys;
import fi.dy.masa.worldutils.reference.HotKeys.EnumKey;
import fi.dy.masa.worldutils.reference.ReferenceNames;
import fi.dy.masa.worldutils.util.ChunkUtils;
import fi.dy.masa.worldutils.util.ChunkUtils.ChangeType;
import fi.dy.masa.worldutils.util.EntityUtils;
import fi.dy.masa.worldutils.util.NBTUtils;
import fi.dy.masa.worldutils.util.PositionUtils;

public class ItemChunkWand extends ItemWorldUtils implements IKeyBound
{
    public static final String WRAPPER_TAG_NAME = "ChunkWand";
    public static final String TAG_NAME_MODE = "Mode";
    public static final String TAG_NAME_CONFIGS = "Configs";
    public static final String TAG_NAME_CONFIG_PRE = "Mode_";
    public static final String TAG_NAME_SELECTION = "Sel";

    protected Map<UUID, Long> lastLeftClick = new HashMap<UUID, Long>();

    public ItemChunkWand()
    {
        super();
        this.setMaxStackSize(1);
        this.setMaxDamage(0);
        this.setUnlocalizedName(ReferenceNames.NAME_ITEM_CHUNK_WAND);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        if (world.isRemote == false)
        {
            this.setPosition(player.getHeldItem(hand), PositionUtils.getLookedAtChunk(world, player, 256), Corner.END);
        }

        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, player.getHeldItem(hand));
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ)
    {
        if (world.isRemote == false)
        {
            this.setPosition(player.getHeldItem(hand), PositionUtils.getChunkPosFromBlockPos(pos), Corner.END);
        }

        return EnumActionResult.SUCCESS;
    }

    public void onItemLeftClick(ItemStack stack, World world, EntityPlayer player)
    {
        this.setPosition(stack, PositionUtils.getLookedAtChunk(world, player, 256), Corner.START);
    }

    public void onLeftClickBlock(EntityPlayer player, World world, ItemStack stack, BlockPos pos, int dimension, EnumFacing side)
    {
        if (world.isRemote == true)
        {
            return;
        }

        // Hack to work around the fact that when the NBT changes, the left click event will fire again the next tick,
        // so it would easily result in the state toggling multiple times per left click
        Long last = this.lastLeftClick.get(player.getUniqueID());

        if (last == null || (world.getTotalWorldTime() - last) >= 4)
        {
            this.setPosition(stack, PositionUtils.getChunkPosFromBlockPos(pos), Corner.START);
        }

        this.lastLeftClick.put(player.getUniqueID(), world.getTotalWorldTime());
    }

    @Override
    public boolean onEntitySwing(EntityLivingBase entityLiving, ItemStack stack)
    {
        return true;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return slotChanged || oldStack.equals(newStack) == false;
    }

    public ChunkPos getPosition(ItemStack stack, Corner corner)
    {
        return this.getPosition(stack, Mode.getMode(stack), corner);
    }

    public ChunkPos getPosition(ItemStack stack, Mode mode, Corner corner)
    {
        NBTTagCompound tag = NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true);
        String tagName = corner == Corner.START ? "Pos1" : "Pos2";

        if (tag.hasKey(tagName, Constants.NBT.TAG_COMPOUND))
        {
            tag = tag.getCompoundTag(tagName);
            return new ChunkPos(tag.getInteger("chunkX"), tag.getInteger("chunkZ"));
        }

        return null;
    }

    private void setPosition(ItemStack stack, ChunkPos pos, Corner corner)
    {
        this.setPosition(stack, Mode.getMode(stack), pos, corner);
    }

    private void setPosition(ItemStack stack, Mode mode, ChunkPos pos, Corner corner)
    {
        NBTTagCompound tag = NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true);
        String tagName = corner == Corner.START ? "Pos1" : "Pos2";

        if (pos == null || pos.equals(this.getPosition(stack, mode, corner)))
        {
            tag.removeTag(tagName);
        }
        else
        {
            NBTTagCompound posTag = new NBTTagCompound();
            posTag.setInteger("chunkX", pos.x);
            posTag.setInteger("chunkZ", pos.z);
            tag.setTag(tagName, posTag);
        }
    }

    private void movePosition(ItemStack stack, EnumFacing direction, boolean reverse, Corner corner)
    {
        ChunkPos pos = this.getPosition(stack, corner);

        if (pos != null)
        {
            int amount = reverse ? 1 : -1;
            pos = new ChunkPos(pos.x + direction.getFrontOffsetX() * amount, pos.z + direction.getFrontOffsetZ() * amount);
            this.setPosition(stack, pos, corner);
        }
    }

    public Collection<ChunkPos> getCurrentAndStoredSelections(ItemStack stack)
    {
        Collection<ChunkPos> chunks = this.getCurrentSelection(stack);
        chunks.addAll(this.getStoredSelection(stack));
        return chunks;
    }

    public Collection<ChunkPos> getCurrentSelection(ItemStack stack)
    {
        List<ChunkPos> list = new ArrayList<ChunkPos>();
        ChunkPos start = this.getPosition(stack, Corner.START);
        ChunkPos end = this.getPosition(stack, Corner.END);

        if (start != null && end != null)
        {
            int minX = Math.min(start.x, end.x);
            int minZ = Math.min(start.z, end.z);
            int maxX = Math.max(start.x, end.x);
            int maxZ = Math.max(start.z, end.z);

            for (int z = minZ; z <= maxZ; z++)
            {
                for (int x = minX; x <= maxX; x++)
                {
                    list.add(new ChunkPos(x, z));
                }
            }
        }
        else if (start != null)
        {
            list.add(start);
        }
        else if (end != null)
        {
            list.add(end);
        }

        return list;
    }

    public Collection<ChunkPos> getStoredSelection(ItemStack stack)
    {
        Set<ChunkPos> stored = new HashSet<ChunkPos>();
        NBTTagCompound tag = NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true);
        NBTTagList list = tag.getTagList("Chunks", Constants.NBT.TAG_LONG);

        for (int i = 0; i < list.tagCount(); i++)
        {
            NBTBase nbt = list.get(i);

            if (nbt.getId() == Constants.NBT.TAG_LONG)
            {
                long val = ((NBTTagLong) nbt).getLong();
                stored.add(new ChunkPos((int)(val & 0xFFFFFFFF), (int)(val >>> 32) & 0xFFFFFFFF));
            }
        }

        return stored;
    }

    private void setStoredChunks(ItemStack stack, Collection<ChunkPos> chunks)
    {
        NBTTagCompound tag = NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true);
        NBTTagList list = new NBTTagList();

        for (ChunkPos pos : chunks)
        {
           list.appendTag(new NBTTagLong(ChunkPos.asLong(pos.x, pos.z)));
        }

        tag.setTag("Chunks", list);
    }

    private void addCurrentSelectionToStoredSet(ItemStack stack)
    {
        Collection<ChunkPos> current = this.getCurrentSelection(stack);
        Collection<ChunkPos> stored = this.getStoredSelection(stack);
        stored.addAll(current);
        this.setStoredChunks(stack, stored);
    }

    private void removeCurrentSelectionFromStoredSet(ItemStack stack)
    {
        Collection<ChunkPos> current = this.getCurrentSelection(stack);
        Collection<ChunkPos> stored = this.getStoredSelection(stack);
        stored.removeAll(current);
        this.setStoredChunks(stack, stored);
    }

    public int getNumTargets(ItemStack stack)
    {
        return NBTUtils.getByte(stack, WRAPPER_TAG_NAME, "NumTargets");
    }

    private void setNumTargets(ItemStack stack, World world)
    {
        int num = ChunkUtils.getNumberOfAlternateWorlds();
        NBTUtils.setByte(stack, WRAPPER_TAG_NAME, "NumTargets", (byte) num);
    }

    public int getTargetSelection(ItemStack stack)
    {
        return NBTUtils.getByte(stack, WRAPPER_TAG_NAME, TAG_NAME_SELECTION);
    }

    private void changeTargetSelection(ItemStack stack, World world, boolean reverse)
    {
        NBTTagCompound tag = NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true);

        int max = this.getNumTargets(stack);
        NBTUtils.cycleByteValue(tag, TAG_NAME_SELECTION, 0, max - 1, reverse);
        tag.setString("WorldName", ChunkUtils.getWorldName(tag.getByte(TAG_NAME_SELECTION)));
    }

    public String getWorldName(ItemStack stack)
    {
        return NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true).getString("WorldName");
    }

    public String getBiomeName(ItemStack stack)
    {
        return NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true).getString("BiomeName");
    }

    public int getBiomeIndex(ItemStack stack)
    {
        return NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true).getByte("BiomeIndex");
    }

    private EnumActionResult useWand(ItemStack stack, World world, EntityPlayer player)
    {
        Mode mode = Mode.getMode(stack);

        if (mode == Mode.CHUNK_SWAP)
        {
            return this.useWandChunkSwap(stack, world, player);
        }
        else if (mode == Mode.BIOME_IMPORT)
        {
            return this.useWandBiomeImport(stack, world, player);
        }
        else if (mode == Mode.BIOME_SET)
        {
            return this.useWandBiomeSet(stack, world, player);
        }

        return EnumActionResult.PASS;
    }

    private EnumActionResult useWandChunkSwap(ItemStack stack, World world, EntityPlayer player)
    {
        Collection<ChunkPos> locations = this.getCurrentAndStoredSelections(stack);
        String worldName = this.getWorldName(stack);

        for (ChunkPos pos : locations)
        {
            ChunkUtils.instance().loadChunkFromAlternateWorld(world, pos, worldName, player.getName());
        }

        PacketHandler.INSTANCE.sendTo(new MessageChunkChanges(ChangeType.CHUNK_CHANGE, locations, worldName), (EntityPlayerMP) player);

        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult useWandBiomeImport(ItemStack stack, World world, EntityPlayer player)
    {
        Collection<ChunkPos> locations = this.getCurrentAndStoredSelections(stack);
        String worldName = this.getWorldName(stack);

        for (ChunkPos pos : locations)
        {
            ChunkUtils.instance().loadBiomesFromAlternateWorld(world, pos, worldName, player.getName());
        }

        PacketHandler.INSTANCE.sendTo(new MessageChunkChanges(ChangeType.BIOME_IMPORT, locations, worldName), (EntityPlayerMP) player);

        return EnumActionResult.SUCCESS;
    }

    private EnumActionResult useWandBiomeSet(ItemStack stack, World world, EntityPlayer player)
    {
        Collection<ChunkPos> locations = this.getCurrentAndStoredSelections(stack);
        Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(this.getBiomeName(stack)));

        for (ChunkPos pos : locations)
        {
            ChunkUtils.instance().setBiome(world, pos, biome, player.getName());
        }

        PacketHandler.INSTANCE.sendTo(new MessageChunkChanges(ChangeType.BIOME_SET, locations, ""), (EntityPlayerMP) player);

        return EnumActionResult.SUCCESS;
    }

    private void changeSelectedBiome(ItemStack stack, boolean reverse)
    {
        ResourceLocation selected = new ResourceLocation(this.getBiomeName(stack));
        List<ResourceLocation> biomes = new ArrayList<ResourceLocation>();
        List<Integer> biomeIds = new ArrayList<Integer>();

        for (Map.Entry<ResourceLocation, Biome> entry : ForgeRegistries.BIOMES.getEntries())
        {
            biomeIds.add(Biome.getIdForBiome(ForgeRegistries.BIOMES.getValue(entry.getKey())));
        }

        Collections.sort(biomeIds);
        int index = -1;
        int i = 0;

        for (Integer id : biomeIds)
        {
            ResourceLocation rl = ForgeRegistries.BIOMES.getKey(Biome.getBiome(id));
            biomes.add(rl);

            if (rl.equals(selected))
            {
                index = i;
            }

            i++;
        }

        if (index == -1)
        {
            index = 0;
        }
        else
        {
            index += reverse ? -1 : 1;

            if (index < 0)
            {
                index = biomes.size() - 1;
            }
            else if (index >= biomes.size())
            {
                index = 0;
            }
        }

        NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true).setString("BiomeName", biomes.get(index).toString());
        NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true).setByte("BiomeId", (byte) biomeIds.get(index).intValue());
        NBTUtils.getCompoundTag(stack, WRAPPER_TAG_NAME, true).setByte("BiomeIndex", (byte) index);
    }

    @Override
    public void doKeyBindingAction(EntityPlayer player, ItemStack stack, int key)
    {
        if (key == HotKeys.KEYCODE_CUSTOM_1)
        {
            this.onItemLeftClick(stack, player.getEntityWorld(), player);
            return;
        }

        Mode mode = Mode.getMode(stack);

        // Alt + Shift + Scroll: Move the start position
        if (EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT_ALT))
        {
            this.movePosition(stack, EntityUtils.getHorizontalLookingDirection(player), EnumKey.keypressActionIsReversed(key), Corner.START);
        }
        // Shift + Scroll: Move the end position
        else if (EnumKey.SCROLL.matches(key, HotKeys.MOD_SHIFT))
        {
            this.movePosition(stack, EntityUtils.getHorizontalLookingDirection(player), EnumKey.keypressActionIsReversed(key), Corner.END);
        }
        // Alt + Scroll: Change world selection
        else if (EnumKey.SCROLL.matches(key, HotKeys.MOD_ALT))
        {
            if (mode == Mode.BIOME_SET)
            {
                this.changeSelectedBiome(stack, EnumKey.keypressActionIsReversed(key));
            }
            else
            {
                this.setNumTargets(stack, player.getEntityWorld());
                this.changeTargetSelection(stack, player.getEntityWorld(), EnumKey.keypressActionIsReversed(key));
            }
        }
        // Ctrl + Scroll: Cycle the mode
        else if (EnumKey.SCROLL.matches(key, HotKeys.MOD_CTRL))
        {
            Mode.cycleMode(stack, EnumKey.keypressActionIsReversed(key) || EnumKey.keypressContainsShift(key), player);
        }
        // Ctrl + Alt + Toggle key: Clear stored chunk selection
        else if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_CTRL_ALT))
        {
            this.setStoredChunks(stack, new ArrayList<ChunkPos>());
        }
        // Shift + Toggle key: Remove current area from stored area
        else if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT))
        {
            this.removeCurrentSelectionFromStoredSet(stack);
        }
        // Alt + Toggle key: Add current area to stored area
        else if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_ALT))
        {
            this.addCurrentSelectionToStoredSet(stack);
        }
        // Ctrl + Shift + Alt + Toggle key: Clear chunk change tracker
        else if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_SHIFT_CTRL_ALT))
        {
            ChunkUtils.instance().clearChangedChunksForUser(player.getEntityWorld(), player.getName());
            player.sendMessage(new TextComponentTranslation("worldutils.chat.message.chunkwand.clearedchangedchunks"));
        }
        // Just Toggle key: Execute the chunk operation
        else if (EnumKey.TOGGLE.matches(key, HotKeys.MOD_NONE))
        {
            if (this.useWand(stack, player.getEntityWorld(), player) == EnumActionResult.SUCCESS)
            {
                player.getEntityWorld().playSound(null, player.getPosition(), SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.MASTER, 0.4f, 0.7f);
            }
        }
    }

    public static enum Mode
    {
        CHUNK_SWAP      ("chunk_swap",      "worldutils.tooltip.item.chunkwand.chunkswap",      true),
        BIOME_IMPORT    ("biome_import",    "worldutils.tooltip.item.chunkwand.biomeimport",    true),
        BIOME_SET       ("biome_set",       "worldutils.tooltip.item.chunkwand.biomeset",       true);

        private final String name;
        private final String unlocName;
        private final boolean hasUseDelay;

        private Mode (String name, String unlocName, boolean useDelay)
        {
            this.name = name;
            this.unlocName = unlocName;
            this.hasUseDelay = useDelay;
        }

        public String getName()
        {
            return this.name;
        }

        public String getDisplayName()
        {
            return I18n.format(this.unlocName);
        }

        public boolean hasUseDelay()
        {
            return this.hasUseDelay;
        }

        public static Mode getMode(ItemStack stack)
        {
            return values()[getModeOrdinal(stack)];
        }

        public static void cycleMode(ItemStack stack, boolean reverse, EntityPlayer player)
        {
            NBTUtils.cycleByteValue(stack, WRAPPER_TAG_NAME, TAG_NAME_MODE, values().length - 1, reverse);
        }

        public static int getModeOrdinal(ItemStack stack)
        {
            int id = NBTUtils.getByte(stack, WRAPPER_TAG_NAME, TAG_NAME_MODE);
            return (id >= 0 && id < values().length) ? id : 0;
        }

        public static int getModeCount(EntityPlayer player)
        {
            return values().length;
        }
    }

    public static enum Corner
    {
        START,
        END;
    }
}
