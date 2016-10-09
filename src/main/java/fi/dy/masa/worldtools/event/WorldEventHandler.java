package fi.dy.masa.worldtools.event;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldtools.util.ChunkChanger;

public class WorldEventHandler
{
    @SubscribeEvent
    public static void onWorldSaveEvent(WorldEvent.Save event)
    {
        ChunkChanger.instance().writeToDisk(event.getWorld());
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event)
    {
        ChunkChanger.instance().readFromDisk(event.getWorld());
    }
}
