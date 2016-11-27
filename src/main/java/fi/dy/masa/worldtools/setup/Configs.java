package fi.dy.masa.worldtools.setup;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import fi.dy.masa.worldtools.compat.journeymap.ChunkChangeTracker;
import fi.dy.masa.worldtools.reference.Reference;

public class Configs
{
    public static final String CATEGORY_GENERIC = "Generic";
    public static final String CATEGORY_CLIENT = "Client";
    public static File configurationFile;
    public static Configuration config;

    public static boolean disableChunkWand;
    public static String chunkSwappingIgnoreWorld;
    public static int colorChangedChunks;
    public static int colorImportedBiomes;
    public static int colorChangedChunksAndImportedBiomes;

    @SubscribeEvent
    public void onConfigChangedEvent(OnConfigChangedEvent event)
    {
        if (Reference.MOD_ID.equals(event.getModID()) == true)
        {
            loadConfigs(config);
        }
    }

    public static void loadConfigsFromFile(File configFile)
    {
        configurationFile = configFile;
        config = new Configuration(configFile, null, true);
        config.load();

        loadConfigs(config);
    }

    public static void loadConfigs(Configuration conf)
    {
        Property prop;
        String category = CATEGORY_GENERIC;

        prop = conf.get(category, "disableChunkWand", true);
        prop.setComment("Whether to enable the Chunk Wand item");
        disableChunkWand = prop.getBoolean();

        category = CATEGORY_CLIENT;

        prop = conf.get(category, "chunkSwappingIgnoreWorld", "");
        prop.setComment("The exact name of the world that is the same as the current world, and will thus be displayed as \"no-changes\" on the map overlay");
        chunkSwappingIgnoreWorld = prop.getString();
        ChunkChangeTracker.instance().setIgnoredWorld(chunkSwappingIgnoreWorld);

        prop = conf.get(category, "colorChangedChunks", "0xFD9500");
        prop.setComment("Overlay color for chunks that have been changed to a different version (default: 0xFD9500 = 16618752)");
        colorChangedChunks = getColor(prop.getString(), 0xFD9500);

        prop = conf.get(category, "colorImportedBiomes", "0x00FFF6");
        prop.setComment("Overlay color for chunks that have had their biomes imported (default: 0x00FFF6 = 65526)");
        colorImportedBiomes = getColor(prop.getString(), 0x00FFF6);

        prop = conf.get(category, "colorChangedChunksAndImportedBiomes", "0xFF22FF");
        prop.setComment("Overlay color for chunks that have had their biomes imported (default: 0xFF22FF = 16720639)");
        colorChangedChunksAndImportedBiomes = getColor(prop.getString(), 0xFF22FF);

        if (conf.hasChanged() == true)
        {
            conf.save();
        }
    }

    private static int getColor(String colorStr, int defaultColor)
    {
        Pattern pattern = Pattern.compile("0x([0-9A-F]{1,8})");
        Matcher matcher = pattern.matcher(colorStr);

        if (matcher.matches())
        {
            try { return Integer.parseInt(matcher.group(1), 16); }
            catch (NumberFormatException e) { return defaultColor; }
        }

        try { return Integer.parseInt(colorStr, 10); }
        catch (NumberFormatException e) { return defaultColor; }
    }
}
