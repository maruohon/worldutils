package fi.dy.masa.worldtools.network;

import java.util.ArrayList;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.worldtools.WorldTools;
import fi.dy.masa.worldtools.compat.journeymap.ChunkChangeTracker;
import fi.dy.masa.worldtools.util.ChunkChanger.ChangeType;
import io.netty.buffer.ByteBuf;

public class MessageChunkChanges implements IMessage
{
    private byte type;
    private int numChunks;
    private String worldName;
    private Collection<ChunkPos> chunks;
    private NBTTagCompound nbt;

    public MessageChunkChanges()
    {
    }

    public MessageChunkChanges(ChangeType type, Collection<ChunkPos> chunks, String worldName)
    {
        this.type = (byte) type.ordinal();
        this.numChunks = chunks.size();
        this.worldName = worldName;
        this.chunks = chunks;
    }

    public MessageChunkChanges(NBTTagCompound nbt)
    {
        this.type = (byte) 0xFF;
        this.nbt = nbt;
    }

    @Override
    public void fromBytes(ByteBuf buf)
    {
        this.type = buf.readByte();

        // Full change set in NBT form
        if (this.type == (byte) 0xFF)
        {
            this.nbt = ByteBufUtils.readTag(buf);
        }
        else
        {
            this.worldName = ByteBufUtils.readUTF8String(buf);
            this.numChunks = buf.readInt();
            this.chunks = new ArrayList<ChunkPos>();

            for (int i = 0; i < this.numChunks; i++)
            {
                this.chunks.add(new ChunkPos(buf.readInt(), buf.readInt()));
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf)
    {
        buf.writeByte(this.type);

        // Full change set in NBT form
        if (this.type == (byte) 0xFF)
        {
            ByteBufUtils.writeTag(buf, this.nbt);
        }
        else
        {
            ByteBufUtils.writeUTF8String(buf, this.worldName);
            buf.writeInt(this.numChunks);

            for (ChunkPos pos : this.chunks)
            {
                buf.writeInt(pos.chunkXPos);
                buf.writeInt(pos.chunkZPos);
            }
        }
    }

    public static class Handler implements IMessageHandler<MessageChunkChanges, IMessage>
    {
        @Override
        public IMessage onMessage(final MessageChunkChanges message, MessageContext ctx)
        {
            if (ctx.side != Side.CLIENT)
            {
                WorldTools.logger.error("Wrong side in MessageChunkChanges: " + ctx.side);
                return null;
            }

            Minecraft mc = FMLClientHandler.instance().getClient();
            final EntityPlayer player = WorldTools.proxy.getPlayerFromMessageContext(ctx);
            if (mc == null || player == null)
            {
                WorldTools.logger.error("Minecraft or player was null in MessageSyncSlot");
                return null;
            }

            mc.addScheduledTask(new Runnable()
            {
                public void run()
                {
                    processMessage(message, player);
                }
            });

            return null;
        }

        protected void processMessage(final MessageChunkChanges message, EntityPlayer player)
        {
            if (message.type == (byte) 0xFF)
            {
                //WorldTools.logger.info("Message to load ALL chunks");
                ChunkChangeTracker.instance().readAllChangesFromNBT(message.nbt);
            }
            else
            {
                //WorldTools.logger.info("Message to load incremental chunks");
                ChunkChangeTracker.instance().addIncrementalChanges(ChangeType.fromId(message.type), message.chunks, message.worldName);
            }
        }
    }
}
