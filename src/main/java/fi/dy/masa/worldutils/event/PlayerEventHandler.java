package fi.dy.masa.worldutils.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.worldutils.item.ItemChunkWand;
import fi.dy.masa.worldutils.network.MessageChunkChanges;
import fi.dy.masa.worldutils.network.MessageKeyPressed;
import fi.dy.masa.worldutils.network.PacketHandler;
import fi.dy.masa.worldutils.reference.HotKeys;
import fi.dy.masa.worldutils.setup.WorldUtilsItems;
import fi.dy.masa.worldutils.util.ChunkUtils;

public class PlayerEventHandler
{
    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        EntityPlayer player = event.getEntityPlayer();
        // You can only left click with the main hand, so this is fine here
        ItemStack stack = player.getHeldItemMainhand();

        if (stack.isEmpty())
        {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        EnumFacing face = event.getFace();

        if (stack.getItem() == WorldUtilsItems.CHUNK_WAND)
        {
            ((ItemChunkWand) stack.getItem()).onLeftClickBlock(player, world, stack, pos, player.dimension, face);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLeftClickAir(PlayerInteractEvent.LeftClickEmpty event)
    {
        if (event.getSide() == Side.CLIENT)
        {
            ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();

            if (stack.isEmpty() == false && stack.getItem() == WorldUtilsItems.CHUNK_WAND)
            {
                PacketHandler.INSTANCE.sendToServer(new MessageKeyPressed(HotKeys.KEYCODE_CUSTOM_1));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            sendChunkChanges(event.player.getEntityWorld(), (EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerChangedDimensionEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            World world = FMLCommonHandler.instance().getMinecraftServerInstance().worldServerForDimension(event.toDim);

            if (world != null)
            {
                sendChunkChanges(world, (EntityPlayerMP) event.player);
            }
        }
    }

    private static void sendChunkChanges(World world, EntityPlayerMP player)
    {
        NBTTagCompound nbt = ChunkUtils.instance().writeToNBT(world, player.getName());

        if (nbt.hasNoTags() == false)
        {
            PacketHandler.INSTANCE.sendTo(new MessageChunkChanges(nbt), player);
        }
    }
}
