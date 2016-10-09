package fi.dy.masa.worldtools.proxy;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fi.dy.masa.worldtools.WorldTools;
import fi.dy.masa.worldtools.event.InputEventHandler;
import fi.dy.masa.worldtools.event.PlayerEventHandler;
import fi.dy.masa.worldtools.event.RenderEventHandler;
import fi.dy.masa.worldtools.event.WorldEventHandler;
import fi.dy.masa.worldtools.reference.HotKeys;
import fi.dy.masa.worldtools.reference.Keybindings;
import fi.dy.masa.worldtools.setup.Configs;
import fi.dy.masa.worldtools.setup.WorldToolsItems;

public class ClientProxy implements IProxy
{
    @Override
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case CLIENT:
                return FMLClientHandler.instance().getClientPlayerEntity();
            case SERVER:
                return ctx.getServerHandler().playerEntity;
            default:
                WorldTools.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new Configs());
        MinecraftForge.EVENT_BUS.register(new InputEventHandler());
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        MinecraftForge.EVENT_BUS.register(new RenderEventHandler());
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
    }

    @Override
    public void registerKeyBindings()
    {
        Keybindings.keyToggleMode = new KeyBinding(HotKeys.KEYBIND_NAME_TOGGLE_MODE,
                                                   HotKeys.DEFAULT_KEYBIND_TOGGLE_MODE,
                                                   HotKeys.KEYBIND_CATEGORY_ENDERUTILITIES);

        ClientRegistry.registerKeyBinding(Keybindings.keyToggleMode);
    }

    @Override
    public void registerModels()
    {
        this.registerAllItemModels();
    }

    private void registerAllItemModels()
    {
        this.registerItemModel(WorldToolsItems.chunkWand);
    }

    private void registerItemModel(Item item)
    {
        this.registerItemModel(item, 0);
    }

    private void registerItemModel(Item item, int meta)
    {
        if (item.getRegistryName() != null)
        {
            ModelLoader.setCustomModelResourceLocation(item, meta, new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
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
