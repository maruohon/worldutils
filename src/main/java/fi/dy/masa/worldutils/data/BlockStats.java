package fi.dy.masa.worldutils.data;

import java.io.DataInputStream;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import fi.dy.masa.worldutils.WorldUtils;
import fi.dy.masa.worldutils.data.DataDump.Alignment;
import fi.dy.masa.worldutils.data.DataDump.Format;
import fi.dy.masa.worldutils.util.FileUtils.Region;

public class BlockStats implements IWorldDataHandler
{
    protected ChunkProviderServer provider;
    private final Multimap<String, BlockInfo> blockStats = MultimapBuilder.hashKeys().arrayListValues().build();
    protected int regionCount;
    protected int chunkCountUnloaded;
    protected long totalCount;
    private boolean append;
    private long timeStart;
    private long[] counts = new long[1048576];

    public void setAppend(boolean append)
    {
        this.append = append;
    }

    @Override
    public void init(int dimension)
    {
        this.timeStart = System.currentTimeMillis();

        if (this.append == false)
        {
            this.blockStats.clear();
            this.counts = new long[1048576];
        }
    }

    @Override
    public void setChunkProvider(@Nullable ChunkProviderServer provider)
    {
        this.provider = provider;
    }

    @Override
    public int processRegion(Region region, boolean simulate)
    {
        this.regionCount++;
        return 0;
    }

    @Override
    public int processChunk(Region region, int chunkX, int chunkZ, boolean simulate)
    {
        NBTTagCompound chunkNBT = null;
        int count = 0;

        try
        {
            DataInputStream data = region.getRegionFile().getChunkDataInputStream(chunkX, chunkZ);

            if (data == null)
            {
                WorldUtils.logger.warn("BlockStats#processChunk(): Failed to get chunk data input stream for chunk [{}, {}] from file '{}'",
                        chunkX, chunkZ, region.getAbsolutePath());
                return 0;
            }

            chunkNBT = CompressedStreamTools.read(data);
            data.close();
        }
        catch (Exception e)
        {
            WorldUtils.logger.warn("BlockStats#processChunk(): Failed to read chunk NBT data for chunk [{}, {}] from file '{}'",
                    chunkX, chunkZ, region.getAbsolutePath(), e);

            return 0;
        }

        if (chunkNBT != null && chunkNBT.hasKey("Level", Constants.NBT.TAG_COMPOUND))
        {
            /*
            NBTTagCompound level = chunkNBT.getCompoundTag("Level");
            int chunkAbsX = level.getInteger("xPos");
            int chunkAbsZ = level.getInteger("zPos");

            // This needs to use absolute Chunk coordinates
            if (this.provider == null || this.provider.chunkExists(chunkAbsX, chunkAbsZ) == false)
            */
            {
                count = this.processUnloadedChunk(region, chunkNBT, chunkX, chunkZ);
                this.totalCount += count;
                this.chunkCountUnloaded++;
            }
        }

        return count;
    }

    private int processUnloadedChunk(Region region, NBTTagCompound chunkNBT, int chunkX, int chunkZ)
    {
        int count = 0;
        NBTTagCompound level = chunkNBT.getCompoundTag("Level");
        NBTTagList sectionsList = level.getTagList("Sections", Constants.NBT.TAG_COMPOUND);
        boolean neid = WorldUtils.isModLoadedNEID();
        int metaShift = neid ? 16 : 12;

        for (int sec = 0; sec < sectionsList.tagCount(); sec++)
        {
            NBTTagCompound sectionTag = sectionsList.getCompoundTagAt(sec);
            byte[] blockArray = sectionTag.getByteArray("Blocks");
            NibbleArray metaNibble = new NibbleArray(sectionTag.getByteArray("Data"));

            if (sectionTag.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY))
            {
                NibbleArray addNibble1 = new NibbleArray(sectionTag.getByteArray("Add"));

                if (neid && sectionTag.hasKey("Add2", Constants.NBT.TAG_BYTE_ARRAY))
                {
                    // Added by the NotEnoughIDs mod
                    NibbleArray addNibble2 = new NibbleArray(sectionTag.getByteArray("Add"));

                    for (int i = 0; i < 4096; i++)
                    {
                        int id = (((addNibble2.getFromIndex(i) & 0xF) << 12) |
                                  ((addNibble1.getFromIndex(i) & 0xF) <<  8) |
                                    (blockArray[i] & 0xFF)) & 0xFFFF;
                        int meta = (metaNibble.getFromIndex(i) & 0xF) << metaShift;
                        this.counts[meta | id]++;
                    }
                }
                else
                {
                    for (int i = 0; i < 4096; i++)
                    {
                        int id = (((addNibble1.getFromIndex(i) & 0xF) << 8) |
                                    (blockArray[i] & 0xFF)) & 0xFFFF;
                        int meta = (metaNibble.getFromIndex(i) & 0xF) << metaShift;
                        this.counts[meta | id]++;
                    }
                }
            }
            // These are completely separate cases so that they are possibly a little bit faster
            // because they don't do the Add array checking on each iteration
            else
            {
                for (int i = 0; i < 4096; i++)
                {
                    int id = blockArray[i] & 0xFF;
                    int meta = (metaNibble.getFromIndex(i) & 0xF) << metaShift;

                    this.counts[meta | id]++;
                }
            }

            count += 4096;
        }

        return count;
    }

    @Override
    public void finish(ICommandSender sender, boolean simulate)
    {
        String timeStr = String.format("%.3f", (float) (System.currentTimeMillis() - this.timeStart) / 1000F);
        WorldUtils.logger.info("Counted a total of {} blocks in {} chunks in %s seconds, touching {} region files",
                this.totalCount, this.chunkCountUnloaded, timeStr, this.regionCount);

        sender.sendMessage(new TextComponentTranslation("Counted a total of %s blocks in %s chunks in %s seconds, touching %s region files",
                Long.valueOf(this.totalCount), Integer.valueOf(this.chunkCountUnloaded), timeStr, Integer.valueOf(this.regionCount)));

        this.addParsedData(this.counts);
    }

    private void addParsedData(long[] counts)
    {
        if (this.append == false)
        {
            this.blockStats.clear();
        }

        final int size = counts.length;
        boolean neid = WorldUtils.isModLoadedNEID();
        final int mask = neid ? 0xFFFF : 0xFFF;
        //final int metaShift = neid ? 16 : 12;

        for (int i = 0; i < size; i++)
        {
            if (counts[i] != 0)
            {
                try
                {
                    Block block = Block.getBlockById(i & mask);

                    if (block == null)
                    {
                        WorldUtils.logger.warn("BlockStats: Invalid block for index {}", i);
                        continue;
                    }

                    //@SuppressWarnings("deprecation")
                    //IBlockState state = block.getStateFromMeta((i >> metaShift) & 0xF);
                    IBlockState state = Block.getStateById(i);
                    ResourceLocation key = ForgeRegistries.BLOCKS.getKey(block);
                    String registryName = key != null ? key.toString() : "null";
                    int id = Block.getIdFromBlock(block);
                    int meta = block.getMetaFromState(state);
                    ItemStack stack = new ItemStack(block, 1, block.damageDropped(state));
                    String displayName = stack.isEmpty() == false ? stack.getDisplayName() : registryName;

                    if (key == null)
                    {
                        WorldUtils.logger.warn("Non-registered block: class = {}, id = {}, meta = {}, state 0 {}",
                                block.getClass().getName(), id, meta, state);
                    }

                    if (this.append)
                    {
                        boolean appended = false;

                        for (BlockInfo old : this.blockStats.get(registryName))
                        {
                            if (old.id == id && old.meta == meta)
                            {
                                old.addToCount(counts[i]);
                                appended = true;
                                break;
                            }
                        }

                        if (appended == false)
                        {
                            this.blockStats.put(registryName, new BlockInfo(registryName, displayName, id, meta, counts[i]));
                        }
                    }
                    else
                    {
                        this.blockStats.put(registryName, new BlockInfo(registryName, displayName, id, meta, counts[i]));
                    }
                }
                catch (Exception e)
                {
                    WorldUtils.logger.error("Caught an exception while getting block names", e);
                }
            }
        }
    }

    private void addFilteredData(DataDump dump, List<String> filters)
    {
        for (String filter : filters)
        {
            int firstSemi = filter.indexOf(":");

            if (firstSemi == -1)
            {
                filter = "minecraft:" + filter;
            }

            int lastSemi = filter.lastIndexOf(":");

            // At least two ':' characters found; assume the first separates the modid and block name,
            // and the second separates the block name and meta.
            if (lastSemi != firstSemi && lastSemi < (filter.length() - 1))
            {
                try
                {
                    int meta = Integer.parseInt(filter.substring(lastSemi + 1, filter.length()));

                    for (BlockInfo info : this.blockStats.get(filter))
                    {
                        if (info.meta == meta)
                        {
                            dump.addData(info.name, String.valueOf(info.id), String.valueOf(info.meta), info.displayName, String.valueOf(info.count));
                            break;
                        }
                    }
                }
                catch (NumberFormatException e)
                {
                    WorldUtils.logger.error("Caught an exception while parsing block meta value from user input", e);
                }
            }
            else
            {
                for (BlockInfo info : this.blockStats.get(filter))
                {
                    dump.addData(info.name, String.valueOf(info.id), String.valueOf(info.meta), info.displayName, String.valueOf(info.count));
                }
            }
        }
    }

    public List<String> queryAll(Format format)
    {
        return this.query(format, null);
    }

    public List<String> query(Format format, @Nullable List<String> filters)
    {
        DataDump dump = new DataDump(5, format);

        if (filters != null)
        {
            this.addFilteredData(dump, filters);
        }
        else
        {
            for (BlockInfo info : this.blockStats.values())
            {
                dump.addData(info.name, String.valueOf(info.id), String.valueOf(info.meta), info.displayName, String.valueOf(info.count));
            }
        }

        dump.addTitle("Registry name", "ID", "meta", "Display name", "Count");
        dump.addHeader("NOTE: The Block ID is for very specific low-level purposes only!");
        dump.addHeader("It WILL be different in every world since Minecraft 1.7,");
        dump.addHeader("because they are dynamically allocated by the game!");

        dump.setColumnProperties(1, Alignment.RIGHT, true); // Block ID
        dump.setColumnProperties(2, Alignment.RIGHT, true); // meta
        dump.setColumnProperties(4, Alignment.RIGHT, true); // count

        dump.setUseColumnSeparator(true);

        return dump.getLines();
    }

    private static class BlockInfo implements Comparable<BlockInfo>
    {
        public final String name;
        public final String displayName;
        public final int id;
        public final int meta;
        public long count;

        public BlockInfo(String name, String displayName, int id, int meta, long count)
        {
            this.name = name;
            this.displayName = displayName;
            this.id = id;
            this.meta = meta;
            this.count = count;
        }

        public void addToCount(long amount)
        {
            this.count += amount;
        }

        public int compareTo(BlockInfo other)
        {
            if (other == null)
            {
                throw new NullPointerException();
            }

            if (this.id != other.id)
            {
                return this.id - other.id;
            }

            if (this.meta != other.meta)
            {
                return this.meta - other.meta;
            }

            return 0;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + id;
            result = prime * result + meta;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BlockInfo other = (BlockInfo) obj;
            if (id != other.id)
                return false;
            if (meta != other.meta)
                return false;
            return true;
        }
    }
}
