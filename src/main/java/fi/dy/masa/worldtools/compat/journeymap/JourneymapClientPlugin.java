package fi.dy.masa.worldtools.compat.journeymap;

import java.util.EnumSet;
import fi.dy.masa.worldtools.WorldTools;
import fi.dy.masa.worldtools.reference.Reference;
import fi.dy.masa.worldtools.setup.Configs;
import fi.dy.masa.worldtools.util.ChunkChanger.ChangeType;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.DisplayType;
import journeymap.client.api.display.PolygonOverlay;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.event.ClientEvent.Type;
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
        this.api.subscribe(Reference.MOD_ID, EnumSet.of(Type.DISPLAY_UPDATE, Type.MAPPING_STARTED));
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

        if (event.type == Type.DISPLAY_UPDATE)
        {
            try
            {
                this.api.removeAll(Reference.MOD_ID, DisplayType.Polygon);

                int c = Configs.colorChangedChunks;
                ShapeProperties shape = new ShapeProperties().setFillColor(c).setStrokeColor(c).setStrokeWidth(2f).setFillOpacity(0.3f);

                for (long chunk : ChunkChangeTracker.instance().getChangedChunksForType(ChangeType.CHUNK_CHANGE))
                {
                    this.api.show(new PolygonOverlay(Reference.MOD_ID, "c-" + chunk, event.dimension, shape,
                        PolygonHelper.createChunkPolygon((int)(chunk & 0xFFFFFFFF), 0, (int)(chunk >>> 32) & 0xFFFFFFFF), null));
                }

                c = Configs.colorImportedBiomes;
                shape = new ShapeProperties().setFillColor(c).setStrokeColor(c).setStrokeWidth(2f).setFillOpacity(0.3f);

                for (long chunk : ChunkChangeTracker.instance().getChangedChunksForType(ChangeType.BIOME_IMPORT))
                {
                    this.api.show(new PolygonOverlay(Reference.MOD_ID, "c-" + chunk, event.dimension, shape,
                        PolygonHelper.createChunkPolygon((int)(chunk & 0xFFFFFFFF), 0, (int)(chunk >>> 32) & 0xFFFFFFFF), null));
                }
            }
            catch (Exception e)
            {
                WorldTools.logger.warn("Exception while updating map overlay");
            }
        }
    }
}
