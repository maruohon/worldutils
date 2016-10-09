package fi.dy.masa.worldtools.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EntitySelectors;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldtools.client.renderer.item.ChunkWandRenderer;
import fi.dy.masa.worldtools.setup.WorldToolsItems;
import fi.dy.masa.worldtools.util.EntityUtils;

public class RenderEventHandler
{
    private static RenderEventHandler INSTANCE;
    public Minecraft mc;
    protected ChunkWandRenderer chunkWandRenderer;

    public RenderEventHandler()
    {
        this.mc = Minecraft.getMinecraft();
        this.chunkWandRenderer = new ChunkWandRenderer();
        INSTANCE = this;
    }

    public static RenderEventHandler getInstance()
    {
        return INSTANCE;
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        this.renderItemExtras(this.mc.theWorld, this.mc.thePlayer, this.mc.thePlayer, event.getPartialTicks());

        //if (Configs.buildersWandRenderForOtherPlayers)
        {
            for (EntityPlayer player : this.mc.theWorld.getPlayers(EntityPlayer.class, EntitySelectors.NOT_SPECTATING))
            {
                if (player != this.mc.thePlayer)
                {
                    this.renderItemExtras(this.mc.theWorld, player, this.mc.thePlayer, event.getPartialTicks());
                }
            }
        }
    }

    private void renderItemExtras(World world, EntityPlayer usingPlayer, EntityPlayer clientPlayer, float partialTicks)
    {
        ItemStack stack = EntityUtils.getHeldItemOfType(usingPlayer, WorldToolsItems.chunkWand);

        if (stack != null && stack.getItem() == WorldToolsItems.chunkWand)
        {
            this.chunkWandRenderer.renderSelectedArea(world, usingPlayer, stack, clientPlayer, partialTicks);
        }
    }

    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Post event)
    {
        if (event.getType() != ElementType.ALL)
        {
            return;
        }

        if ((this.mc.currentScreen instanceof GuiChat) == false && this.mc.thePlayer != null)
        {
            this.chunkWandRenderer.renderHudChunkWand(this.mc.thePlayer);
        }
    }
}
