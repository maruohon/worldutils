package fi.dy.masa.worldtools;

import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import fi.dy.masa.worldtools.network.PacketHandler;
import fi.dy.masa.worldtools.proxy.IProxy;
import fi.dy.masa.worldtools.reference.Reference;
import fi.dy.masa.worldtools.setup.WorldToolsItems;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
updateJSON = "https://raw.githubusercontent.com/maruohon/worldtools/master/update.json",
acceptedMinecraftVersions = "1.10.2")
public class WorldTools
{
    @Instance(Reference.MOD_ID)
    public static WorldTools instance;

    @SidedProxy(clientSide = Reference.PROXY_CLASS_CLIENT, serverSide = Reference.PROXY_CLASS_SERVER)
    public static IProxy proxy;
    public static Logger logger;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        instance = this;
        logger = event.getModLog();
        //ConfigReader.loadConfigsAll(event.getSuggestedConfigurationFile());

        WorldToolsItems.init();
        PacketHandler.init(); // Initialize network stuff

        proxy.registerModels();
        proxy.registerKeyBindings();
        proxy.registerEventHandlers();
    }
}
