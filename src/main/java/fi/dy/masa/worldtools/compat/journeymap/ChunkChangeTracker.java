package fi.dy.masa.worldtools.compat.journeymap;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.common.util.Constants;
import fi.dy.masa.worldtools.util.ChunkChanger.ChangeType;

public class ChunkChangeTracker
{
    private static final ChunkChangeTracker INSTANCE = new ChunkChangeTracker();
    private final Set<ChunkPos> changedChunks = new HashSet<ChunkPos>();
    private final Set<ChunkPos> importedBiomes = new HashSet<ChunkPos>();
    private String ignoredWorld = "";
    private boolean newChanges;

    private ChunkChangeTracker()
    {
    }

    public static ChunkChangeTracker instance()
    {
        return INSTANCE;
    }

    public void setIgnoredWorld(String ignore)
    {
        this.ignoredWorld = ignore;
    }

    public boolean hasNewChanges()
    {
        return this.newChanges;
    }

    /**
     * Returns a Set of chunks for the given ChangeType.
     * Also clears the hasNewChanges() status.
     */
    public Set<ChunkPos> getChangedChunksForType(ChangeType type)
    {
        this.newChanges = false;

        return type == ChangeType.CHUNK_CHANGE ? this.changedChunks : this.importedBiomes;
    }

    private void readChangesFromNBT(Set<ChunkPos> setIn, NBTTagCompound nbt, String tagName)
    {
        NBTTagList list = nbt.getTagList(tagName, Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < list.tagCount(); i++)
        {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            boolean ignored = this.ignoredWorld.equals(tag.getString("world"));
            int[] arr = tag.getIntArray("chunks");

            for (int j = 0; j < arr.length - 1; j += 2)
            {
                if (ignored == false)
                {
                    setIn.add(new ChunkPos(arr[j], arr[j + 1]));
                }
            }
        }
    }

    public void readAllChangesFromNBT(NBTTagCompound nbt)
    {
        if (nbt == null || StringUtils.isBlank(nbt.getString("user")))
        {
            return;
        }

        this.changedChunks.clear();
        this.importedBiomes.clear();

        this.readChangesFromNBT(this.changedChunks, nbt, "changes");
        this.readChangesFromNBT(this.importedBiomes, nbt, "biomes");

        this.newChanges = true;
    }

    public void addIncrementalChanges(ChangeType type, Collection<ChunkPos> chunks, String worldName)
    {
        Set<ChunkPos> set = this.getChangedChunksForType(type);

        for (ChunkPos pos : chunks)
        {
            if (this.ignoredWorld.equals(worldName))
            {
                set.remove(pos);
            }
            else
            {
                set.add(pos);
            }

            // A chunk change removes an existing biome import
            if (type == ChangeType.CHUNK_CHANGE)
            {
                this.importedBiomes.remove(pos);
            }
        }

        this.newChanges = true;
    }
}
