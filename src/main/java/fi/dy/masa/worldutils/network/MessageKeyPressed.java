package fi.dy.masa.worldutils.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.item.base.IKeyBound;
import fi.dy.masa.worldutils.util.EntityUtils;
import io.netty.buffer.ByteBuf;

public class MessageKeyPressed implements IMessage
{
    private int keyPressed;

    public MessageKeyPressed()
    {
    }

    public MessageKeyPressed(int key)
    {
        this.keyPressed = key;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.keyPressed = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeInt(this.keyPressed);
    }

    public static class Handler implements IMessageHandler<MessageKeyPressed, IMessage>
    {
        @Override
        public IMessage onMessage(final MessageKeyPressed message, MessageContext ctx)
        {
            if (ctx.side != Side.SERVER)
            {
                WorldUtils.logger.error("Wrong side in MessageKeyPressed: " + ctx.side);
                return null;
            }

            final EntityPlayerMP sendingPlayer = ctx.getServerHandler().player;
            if (sendingPlayer == null)
            {
                WorldUtils.logger.error("Sending player was null in MessageKeyPressed");
                return null;
            }

            final WorldServer playerWorldServer = sendingPlayer.getServerWorld();
            if (playerWorldServer == null)
            {
                WorldUtils.logger.error("World was null in MessageKeyPressed");
                return null;
            }

            playerWorldServer.addScheduledTask(new Runnable()
            {
                public void run()
                {
                    processMessage(message, sendingPlayer);
                }
            });

            return null;
        }

        protected void processMessage(final MessageKeyPressed message, EntityPlayer player)
        {
            ItemStack stack = EntityUtils.getHeldItemOfType(player, IKeyBound.class);

            if (stack.isEmpty() == false)
            {
                ((IKeyBound) stack.getItem()).doKeyBindingAction(player, stack, message.keyPressed);
            }
        }
    }
}
