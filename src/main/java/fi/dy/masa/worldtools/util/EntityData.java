package fi.dy.masa.worldtools.util;

import java.util.UUID;
import net.minecraft.util.math.Vec3d;

public class EntityData implements Comparable<EntityData>
{
    public final int dimension;
    public final String id;
    public final Vec3d pos;
    public final UUID uuid;

    public EntityData(int dimension, String id, Vec3d pos, UUID uuid)
    {
        this.dimension = dimension;
        this.id = id;
        this.pos = pos;
        this.uuid = uuid;
    }

    @Override
    public int compareTo(EntityData other)
    {
        int idComp = this.id.compareTo(other.id);

        if (idComp == 0)
        {
            return this.uuid.toString().compareTo(other.uuid.toString());
        }

        return idComp;

        //return this.uuid.compareTo(other.uuid);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
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
        EntityData other = (EntityData) obj;
        if (uuid == null)
        {
            if (other.uuid != null)
                return false;
        }
        else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }
}
