package fi.dy.masa.worldutils.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.config.Configs;
import fi.dy.masa.worldutils.event.InputEventHandler;
import fi.dy.masa.worldutils.event.RenderEventHandler;
import fi.dy.masa.worldutils.item.base.ItemWorldUtils;
import fi.dy.masa.worldutils.reference.HotKeys;
import fi.dy.masa.worldutils.reference.Keybindings;
import fi.dy.masa.worldutils.registry.WorldUtilsItems;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy
{
    @Override
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case CLIENT:
                return FMLClientHandler.instance().getClientPlayerEntity();
            case SERVER:
                return ctx.getServerHandler().player;
            default:
                WorldUtils.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

    @Override
    public void registerEventHandlers()
    {
        super.registerEventHandlers();

        MinecraftForge.EVENT_BUS.register(new Configs());

        if (Configs.disableChunkWand == false)
        {
            MinecraftForge.EVENT_BUS.register(new InputEventHandler());
            MinecraftForge.EVENT_BUS.register(new RenderEventHandler());
        }
    }

    @Override
    public void registerKeyBindings()
    {
        if (Configs.disableChunkWand == false)
        {
            Keybindings.keyToggleMode = new KeyBinding(HotKeys.KEYBIND_NAME_TOGGLE_MODE,
                                                       HotKeys.DEFAULT_KEYBIND_TOGGLE_MODE,
                                                       HotKeys.KEYBIND_CATEGORY_WORLD_UTILS);

            ClientRegistry.registerKeyBinding(Keybindings.keyToggleMode);
        }
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event)
    {
        registerItemModel(WorldUtilsItems.CHUNK_WAND, 0);
    }

    private static void registerItemModel(ItemWorldUtils item, int meta)
    {
        if (item != null)
        {
            ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
    }

    @Override
    public boolean isSinglePlayer()
    {
        return Minecraft.getMinecraft().isSingleplayer();
    }

    @Override
    public boolean isShiftKeyDown()
    {
        return GuiScreen.isShiftKeyDown();
    }

    @Override
    public boolean isControlKeyDown()
    {
        return GuiScreen.isCtrlKeyDown();
    }

    @Override
    public boolean isAltKeyDown()
    {
        return GuiScreen.isAltKeyDown();
    }
}
