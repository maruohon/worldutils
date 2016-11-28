package fi.dy.masa.worldutils.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.event.PlayerEventHandler;
import fi.dy.masa.worldutils.event.WorldEventHandler;

public class ServerProxy implements IProxy
{

    @Override
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx)
    {
        switch (ctx.side)
        {
            case SERVER:
                return ctx.getServerHandler().playerEntity;
            default:
                WorldUtils.logger.warn("Invalid side in getPlayerFromMessageContext(): " + ctx.side);
                return null;
        }
    }

    @Override
    public void registerEventHandlers()
    {
        MinecraftForge.EVENT_BUS.register(new PlayerEventHandler());
        MinecraftForge.EVENT_BUS.register(new WorldEventHandler());
    }

    @Override
    public void registerKeyBindings()
    {
    }

    @Override
    public void registerModels()
    {
    }

    @Override
    public boolean isShiftKeyDown()
    {
        return false;
    }

    @Override
    public boolean isControlKeyDown()
    {
        return false;
    }

    @Override
    public boolean isAltKeyDown()
    {
        return false;
    }
}
