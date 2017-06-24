package fi.dy.masa.worldutils.proxy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public interface IProxy
{
    public EntityPlayer getPlayerFromMessageContext(MessageContext ctx);

    public void registerEventHandlers();

    public void registerKeyBindings();

    public boolean isShiftKeyDown();

    public boolean isControlKeyDown();

    public boolean isAltKeyDown();
}
