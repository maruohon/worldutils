package fi.dy.masa.worldutils.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.event.PlayerEventHandler;
import fi.dy.masa.worldutils.event.TickHandler;
import fi.dy.masa.worldutils.event.WorldEventHandler;

public class CommonProxy
{
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case SERVER:
                return ctx.getServerHandler().player;
            default:
                WorldUtils.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        MinecraftForge.EVENT_BUS.register(new TickHandler());
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
    }

    public void registerKeyBindings()
    {
    }

    public boolean isSinglePlayer()
    {
        return false;
    }

    public boolean isShiftKeyDown()
    {
        return false;
    }

    public boolean isControlKeyDown()
    {
        return false;
    }

    public boolean isAltKeyDown()
    {
        return false;
    }
}
