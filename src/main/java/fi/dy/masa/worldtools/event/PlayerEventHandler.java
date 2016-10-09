package fi.dy.masa.worldtools.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.worldtools.item.ItemChunkWand;
import fi.dy.masa.worldtools.network.MessageKeyPressed;
import fi.dy.masa.worldtools.network.PacketHandler;
import fi.dy.masa.worldtools.reference.HotKeys;
import fi.dy.masa.worldtools.setup.WorldToolsItems;

public class PlayerEventHandler
{
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        EntityPlayer player = event.getEntityPlayer();
        // You can only left click with the main hand, so this is fine here
        ItemStack stack = player.getHeldItemMainhand();
        if (stack == null)
        {
            return;
        }

        World world = event.getWorld();
        BlockPos pos = event.getPos();
        EnumFacing face = event.getFace();

        if (stack.getItem() == WorldToolsItems.chunkWand)
        {
            ((ItemChunkWand) stack.getItem()).onLeftClickBlock(player, world, stack, pos, player.dimension, face);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLeftClickAir(PlayerInteractEvent.LeftClickEmpty event)
    {
        if (event.getSide() == Side.CLIENT)
        {
            ItemStack stack = event.getEntityPlayer().getHeldItemMainhand();

            if (stack != null && stack.getItem() == WorldToolsItems.chunkWand)
            {
                PacketHandler.INSTANCE.sendToServer(new MessageKeyPressed(HotKeys.KEYCODE_CUSTOM_1));
            }
        }
    }
}
