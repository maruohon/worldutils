package fi.dy.masa.worldtools.compat.journeymap;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldtools.util.ChunkChanger.ChangeType;
import fi.dy.masa.worldtools.util.ChunkChanger.ChunkChanges;

public class ChunkChangeTracker
{
    private static final ChunkChangeTracker INSTANCE = new ChunkChangeTracker();
    private final Map<Long, ChunkChanges> changes = new HashMap<Long, ChunkChanges>();
    private final Set<Long> changedChunks = new HashSet<Long>();
    private final Set<Long> importedBiomes = new HashSet<Long>();
    private String ignoreWorld;
    private boolean dirty;

    private ChunkChangeTracker()
    {
    }

    public static ChunkChangeTracker instance()
    {
        return INSTANCE;
    }

    public void setIgnoredWorld(String ignore)
    {
        this.ignoreWorld = ignore;
    }

    public void readAllChangesFromNBT(NBTTagCompound nbt)
    {
        if (nbt == null || StringUtils.isBlank(nbt.getString("user")))
        {
            return;
        }

        this.changes.clear();

        NBTTagList list = nbt.getTagList("changes", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < list.tagCount(); i++)
        {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            String world = tag.getString("world");
            ChangeType type = ChangeType.fromId(tag.getByte("type"));
            int[] arr = tag.getIntArray("chunks");

            for (int j = 0; j < arr.length - 1; j += 2)
            {
                long loc = ((long) arr[j + 1]) << 32 | (long) arr[j];
                this.changes.put(loc, new ChunkChanges(type, world));
            }
        }

        this.dirty = true;
    }

    public void addIncrementalChanges(ChangeType type, Collection<ChunkPos> chunks, String worldName)
    {
        for (ChunkPos pos : chunks)
        {
            this.changes.put(ChunkPos.asLong(pos.chunkXPos, pos.chunkZPos), new ChunkChanges(type, worldName));
        }

        this.dirty = true;
    }

    private void parseData()
    {
        if (this.dirty)
        {
            this.changedChunks.clear();
            this.importedBiomes.clear();

            for (Map.Entry<Long, ChunkChanges> entry : this.changes.entrySet())
            {
                ChunkChanges changes = entry.getValue();

                if (changes.worldName.equals(this.ignoreWorld) == false)
                {
                    if (changes.type == ChangeType.CHUNK_CHANGE)
                    {
                        this.changedChunks.add(entry.getKey());
                    }
                    else
                    {
                        this.importedBiomes.add(entry.getKey());
                    }
                }
            }

            this.dirty = false;
        }
    }

    public Set<Long> getChangedChunksForType(ChangeType type)
    {
        this.parseData();

        if (type == ChangeType.CHUNK_CHANGE)
        {
            return this.changedChunks;
        }

        return this.importedBiomes;
    }
}
