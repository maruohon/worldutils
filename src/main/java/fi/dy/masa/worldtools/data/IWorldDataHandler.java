package fi.dy.masa.worldtools.data;

import javax.annotation.Nullable;
import net.minecraft.command.ICommandSender;
import net.minecraft.world.gen.ChunkProviderServer;
import fi.dy.masa.worldtools.util.FileUtils.Region;

public interface IWorldDataHandler
{
    /**
     * Called before processing world data begins.
     */
    public void init();

    /**
     * Sets the chunk provider for the world being processed.
     * @param provider
     */
    public void setChunkProvider(@Nullable ChunkProviderServer provider);

    /**
     * Process data from the provided region file.
     * @param region The current region file being processed.
     * @param simulate If true, only simulate what would happen
     * @return The number of data entries processed. Implementation-specific. <b>Note:</b>
     * If the returned value is not 0, then this method is assumed to have taken care of
     * all processing for this region and external processing of this region should not happen.
     */
    public int processRegion(Region region, boolean simulate);

    /**
     * Process data for the given chunk within the given region.
     * @param region The current region file being processed.
     * @param chunkX The chunk X position of the current chunk within this region file.
     * @param chunkX The chunk Z position of the current chunk within this region file.
     * @param simulate If true, only simulate what would happen
     * @return The number of data entries processed. Implementation-specific.
     */
    public int processChunk(Region region, int chunkX, int chunkZ, boolean simulate);

    /**
     * Called after all region files have been processed.
     * @param simulate If true, only simulate what would happen
     * @return
     */
    public void finish(ICommandSender sender, boolean simulate);
}
