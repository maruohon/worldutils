package fi.dy.masa.worldtools;

import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import fi.dy.masa.worldtools.command.CommandWorldTools;
import fi.dy.masa.worldtools.network.PacketHandler;
import fi.dy.masa.worldtools.proxy.IProxy;
import fi.dy.masa.worldtools.reference.Reference;
import fi.dy.masa.worldtools.setup.Configs;
import fi.dy.masa.worldtools.setup.WorldToolsItems;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
    guiFactory = "fi.dy.masa.worldtools.setup.WorldToolsGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/worldtools/master/update.json",
    acceptedMinecraftVersions = "1.10.2")
public class WorldTools
{
    @Instance(Reference.MOD_ID)
    public static WorldTools instance;

    @SidedProxy(clientSide = Reference.PROXY_CLASS_CLIENT, serverSide = Reference.PROXY_CLASS_SERVER)
    public static IProxy proxy;
    public static Logger logger;
    public static String configDirPath;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        instance = this;
        logger = event.getModLog();
        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        configDirPath = event.getModConfigurationDirectory().getAbsolutePath().concat("/" + Reference.MOD_ID);

        WorldToolsItems.init();
        PacketHandler.init(); // Initialize network stuff

        proxy.registerModels();
        proxy.registerKeyBindings();
        proxy.registerEventHandlers();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandWorldTools());
    }
}
