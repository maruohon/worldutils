package fi.dy.masa.worldtools.reference;

public class ReferenceNames
{
    public static final String NAME_ITEM_CHUNK_WAND             = "chunkwand";

    public static String getPrefixedName(String name)
    {
        return Reference.MOD_ID + "_" + name;
    }

    public static String getDotPrefixedName(String name)
    {
        return Reference.MOD_ID + "." + name;
    }
}
