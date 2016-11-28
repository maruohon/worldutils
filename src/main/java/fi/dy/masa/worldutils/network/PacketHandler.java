package fi.dy.masa.worldutils.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import fi.dy.masa.worldutils.reference.Reference;

public class PacketHandler
{
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID.toLowerCase());

    public static void init()
    {
        INSTANCE.registerMessage(MessageKeyPressed.Handler.class,   MessageKeyPressed.class,    0, Side.SERVER);
        INSTANCE.registerMessage(MessageChunkChanges.Handler.class, MessageChunkChanges.class,  1, Side.CLIENT);
    }
}
