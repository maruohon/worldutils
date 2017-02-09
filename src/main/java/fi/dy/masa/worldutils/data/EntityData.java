package fi.dy.masa.worldutils.data;

import java.util.UUID;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

public class EntityData implements Comparable<EntityData>
{
    private final int dimension;
    private final String id;
    private final Vec3d pos;
    private final ChunkPos chunkPos;
    private final UUID uuid;

    public EntityData(int dimension, String id, Vec3d pos, ChunkPos chunkPos, UUID uuid)
    {
        this.dimension = dimension;
        this.id = id;
        this.pos = pos;
        this.chunkPos = chunkPos;
        this.uuid = uuid;
    }

    public int getDimension()
    {
        return this.dimension;
    }

    public String getId()
    {
        return this.id;
    }

    public Vec3d getPosition()
    {
        return this.pos;
    }

    public ChunkPos getChunkPosition()
    {
        return this.chunkPos;
    }

    public UUID getUUID()
    {
        return this.uuid;
    }

    @Override
    public int compareTo(EntityData other)
    {
        int idComp = this.getId().compareTo(other.getId());

        if (idComp == 0)
        {
            return this.getUUID().toString().compareTo(other.getUUID().toString());
        }

        return idComp;

        //return this.uuid.compareTo(other.uuid);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.getId() == null) ? 0 : this.getId().hashCode());
        result = prime * result + ((this.getUUID() == null) ? 0 : this.getUUID().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) { return true; }
        if (obj == null) { return false; }
        if (getClass() != obj.getClass()) { return false; }

        EntityData other = (EntityData) obj;
        if (this.getId() == null)
        {
            if (other.getId() != null) { return false; }
        }
        else if (! this.getId().equals(other.getId())) { return false; }

        if (this.getUUID() == null)
        {
            if (other.getUUID() != null) { return false; }
        }
        else if (! this.getUUID().equals(other.getUUID())) { return false; }

        return true;
    }

    public static class MutableEntityData
    {
        protected int dimension;
        protected String id;
        protected Vec3d pos;
        protected ChunkPos chunkPos;
        protected UUID uuid;

        public MutableEntityData()
        {
        }

        public MutableEntityData(String id, UUID uuid)
        {
            this.id = id;
            this.uuid = uuid;
        }

        public int getDimension()
        {
            return this.dimension;
        }

        public String getId()
        {
            return this.id;
        }

        public Vec3d getPosiiton()
        {
            return this.pos;
        }

        public ChunkPos getChunkPosition()
        {
            return this.chunkPos;
        }

        public UUID getUUID()
        {
            return this.uuid;
        }

        public MutableEntityData setDimension(int dimension)
        {
            this.dimension = dimension;
            return this;
        }

        public MutableEntityData setId(String id)
        {
            this.id = id;
            return this;
        }

        public MutableEntityData setPosiiton(Vec3d pos)
        {
            this.pos = pos;
            return this;
        }

        public MutableEntityData setChunkPosition(ChunkPos pos)
        {
            this.chunkPos = pos;
            return this;
        }

        public MutableEntityData setUUID(UUID uuid)
        {
            this.uuid = uuid;
            return this;
        }
    }
}
