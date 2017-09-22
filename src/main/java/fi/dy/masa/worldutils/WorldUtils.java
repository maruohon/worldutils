package fi.dy.masa.worldutils;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import fi.dy.masa.worldutils.command.CommandWorldUtils;
import fi.dy.masa.worldutils.config.Configs;
import fi.dy.masa.worldutils.network.PacketHandler;
import fi.dy.masa.worldutils.proxy.IProxy;
import fi.dy.masa.worldutils.reference.Reference;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.MOD_VERSION, certificateFingerprint = Reference.FINGERPRINT,
    guiFactory = "fi.dy.masa.worldutils.config.WorldUtilsGuiFactory",
    updateJSON = "https://raw.githubusercontent.com/maruohon/worldutils/master/update.json",
    acceptableRemoteVersions = "*",
    acceptedMinecraftVersions = "1.12",
    dependencies = "required-after:forge@[14.21.0.2373,);")
public class WorldUtils
{
    @Instance(Reference.MOD_ID)
    public static WorldUtils instance;

    @SidedProxy(clientSide = Reference.PROXY_CLASS_CLIENT, serverSide = Reference.PROXY_CLASS_SERVER)
    public static IProxy proxy;

    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);
    public static String configDirPath;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        Configs.loadConfigsFromFile(event.getSuggestedConfigurationFile());
        configDirPath = new File(event.getModConfigurationDirectory(), Reference.MOD_ID).getAbsolutePath();

        PacketHandler.init(); // Initialize network stuff

        proxy.registerKeyBindings();
        proxy.registerEventHandlers();
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event)
    {
        event.registerServerCommand(new CommandWorldUtils());
    }

    @Mod.EventHandler
    public void onFingerPrintViolation(FMLFingerprintViolationEvent event)
    {
        // Not running in a dev environment
        if (event.isDirectory() == false)
        {
            logger.warn("*********************************************************************************************");
            logger.warn("*****                                    WARNING                                        *****");
            logger.warn("*****                                                                                   *****");
            logger.warn("*****   The signature of the mod file '{}' does not match the expected fingerprint!     *****", event.getSource().getName());
            logger.warn("*****   This might mean that the mod file has been tampered with!                       *****");
            logger.warn("*****   If you did not download the mod {} directly from Curse/CurseForge,       *****", Reference.MOD_NAME);
            logger.warn("*****   or using one of the well known launchers, and you did not                       *****");
            logger.warn("*****   modify the mod file at all yourself, then it's possible,                        *****");
            logger.warn("*****   that it may contain malware or other unwanted things!                           *****");
            logger.warn("*********************************************************************************************");
        }
    }
}
