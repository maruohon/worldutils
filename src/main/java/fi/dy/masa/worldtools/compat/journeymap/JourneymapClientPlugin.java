package fi.dy.masa.worldtools.compat.journeymap;

import java.util.EnumSet;
import java.util.Set;
import net.minecraft.util.math.ChunkPos;
import fi.dy.masa.worldtools.WorldTools;
import fi.dy.masa.worldtools.reference.Reference;
import fi.dy.masa.worldtools.setup.Configs;
import fi.dy.masa.worldtools.util.ChunkChanger.ChangeType;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.ClientEvent.Type;
import journeymap.client.api.model.MapPolygon;
import journeymap.client.api.model.ShapeProperties;
import journeymap.client.api.util.PolygonHelper;

@journeymap.client.api.ClientPlugin
public class JourneymapClientPlugin implements IClientPlugin
{
    private IClientAPI api;

    @Override
    public void initialize(IClientAPI jmClientApi)
    {
        this.api = jmClientApi;
        this.api.subscribe(Reference.MOD_ID, EnumSet.of(Type.DISPLAY_UPDATE, Type.MAPPING_STARTED, Type.MAPPING_STOPPED));
    }

    @Override
    public String getModId()
    {
        return Reference.MOD_ID;
    }

    @Override
    public void onEvent(ClientEvent event)
    {
        //System.out.printf("event: " + event.type + "\n");

        try
        {
            switch (event.type)
            {
                case MAPPING_STOPPED:
                    this.api.removeAll(Reference.MOD_ID);
                    break;

                case MAPPING_STARTED:
                case DISPLAY_UPDATE:
                    this.updatePolygons(event.dimension);
                    break;

                default:
            }
        }
        catch (Exception e)
        {
            WorldTools.logger.warn("Exception while updating map overlay");
        }
    }

    private void updatePolygons(int dimension) throws Exception
    {
        this.api.removeAll(Reference.MOD_ID);

        Set<ChunkPos> chunksChanged = ChunkChangeTracker.instance().getChangedChunksForType(ChangeType.CHUNK_CHANGE);
        Set<ChunkPos> chunksBiomes = ChunkChangeTracker.instance().getChangedChunksForType(ChangeType.BIOME_IMPORT);
        int colorChanged = Configs.colorChangedChunks;
        int colorBiomes = Configs.colorImportedBiomes;
        int colorBoth = Configs.colorChangedChunksAndImportedBiomes;

        ShapeProperties shapeChanged = new ShapeProperties()
                .setStrokeWidth(1.5f).setFillOpacity(0.2f)
                .setFillColor(colorChanged).setStrokeColor(colorChanged);

        ShapeProperties shapeBiomes = new ShapeProperties()
                .setStrokeWidth(1.5f).setFillOpacity(0.2f)
                .setFillColor(colorBiomes).setStrokeColor(colorBiomes);

        ShapeProperties shapeBoth = new ShapeProperties()
                .setStrokeWidth(1.5f).setFillOpacity(0.2f)
                .setFillColor(colorBoth).setStrokeColor(colorBoth);

        for (ChunkPos chunk : chunksChanged)
        {
            if (chunksBiomes.contains(chunk))
            {
                this.api.show(this.createPolygon(dimension, chunk, shapeBoth));
            }
            else
            {
                this.api.show(this.createPolygon(dimension, chunk, shapeChanged));
            }
        }

        for (ChunkPos chunk : chunksBiomes)
        {
            if (chunksChanged.contains(chunk) == false)
            {
                this.api.show(this.createPolygon(dimension, chunk, shapeBiomes));
            }
        }

    }

    private PolygonOverlay createPolygon(int dimension, ChunkPos pos, ShapeProperties shape)
    {
        String displayId = "chunk_" + pos.toString();

        MapPolygon polygon = PolygonHelper.createChunkPolygon(pos.chunkXPos, 256, pos.chunkZPos);
        PolygonOverlay overlay = new PolygonOverlay(Reference.MOD_ID, displayId, dimension, shape, polygon, null);

        /*String groupName = "Modified Chunks";
        String label = String.format("Modified Chunk [%s,%s]", pos.chunkXPos, pos.chunkZPos);
        TextProperties textProps = new TextProperties()
                .setBackgroundColor(0x000022).setBackgroundOpacity(.5f)
                .setColor(0x00ff00).setOpacity(1f)
                .setMinZoom(2).setFontShadow(true);

        overlay.setOverlayGroupName(groupName).setLabel(label).setTextProperties(textProps);*/

        return overlay;
    }
}
