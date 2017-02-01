package fi.dy.masa.worldutils.event;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ServerTickEvent;
import fi.dy.masa.worldutils.event.tasks.TaskScheduler;

public class TickHandler
{
    private static TickHandler instance;
    private long timeServerTickStart;

    public TickHandler()
    {
        instance = this;
    }

    public static TickHandler instance()
    {
        return instance;
    }

    public long getTickStartTime()
    {
        return this.timeServerTickStart;
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            this.timeServerTickStart = System.currentTimeMillis();
            return;
        }

        TaskScheduler.getInstance().runTasks();
    }
}
