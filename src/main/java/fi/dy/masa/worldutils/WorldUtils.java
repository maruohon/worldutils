package fi.dy.masa.worldutils;

import java.io.File;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import fi.dy.masa.worldutils.command.CommandWorldUtils;
import fi.dy.masa.worldutils.network.PacketHandler;
import fi.dy.masa.worldutils.proxy.IProxy;
import fi.dy.masa.worldutils.reference.Reference;
import fi.dy.masa.worldutils.setup.Configs;
import fi.dy.masa.worldutils.setup.WorldUtilsItems;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION,
    guiFactory = "fi.dy.masa.worldutils.setup.WorldUtilsGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/worldutils/master/update.json",
    acceptableRemoteVersions = "*", acceptedMinecraftVersions = "1.11.2")
public class WorldUtils
{
    @Instance(Reference.MOD_ID)
    public static WorldUtils instance;

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
        configDirPath = new File(event.getModConfigurationDirectory(), Reference.MOD_ID).getAbsolutePath();

        WorldUtilsItems.init();
        PacketHandler.init(); // Initialize network stuff

        proxy.registerModels();
        proxy.registerKeyBindings();
        proxy.registerEventHandlers();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandWorldUtils());
    }
}
