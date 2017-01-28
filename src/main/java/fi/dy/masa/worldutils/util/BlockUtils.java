package fi.dy.masa.worldutils.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import fi.dy.masa.worldutils.WorldUtils;

public class BlockUtils
{
    public enum DataType
    {
        ID,
        ID_META,
        NAME,
        NAME_META,
        NAME_PROPS;
    }

    public static class BlockData
    {
        private DataType type;
        private int id;
        private int meta;
        private String name;
        private String props;

        public BlockData(int id)
        {
            this.type = DataType.ID;
            this.id = id;
            this.meta = 0;
        }

        public BlockData(int id, int meta)
        {
            this.type = DataType.ID_META;
            this.id = id;
            this.meta = meta;
        }

        public BlockData(String name)
        {
            this.type = DataType.NAME;
            this.name = name;
            this.meta = 0;
            this.getNumericValues();
        }

        public BlockData(String name, int meta)
        {
            this.type = DataType.NAME_META;
            this.name = name;
            this.meta = meta;
            this.getNumericValues();
        }

        public BlockData(String name, String props)
        {
            this.type = DataType.NAME_PROPS;
            this.name = name;
            this.props = props;
            this.getNumericValues();
        }

        public DataType getType()
        {
            return this.type;
        }

        public String getName()
        {
            return this.name;
        }

        public int getId()
        {
            return this.id & 0xFFF;
        }

        public int getMeta()
        {
            return this.meta & 0xF;
        }

        public boolean ignoreMeta()
        {
            return this.type == DataType.ID || this.type == DataType.NAME;
        }

        public boolean isValid()
        {
            switch (this.type)
            {
                case ID:
                    return this.id >= 0 && this.id < 4096;
                case ID_META:
                    return this.id >= 0 && this.id < 4096 && this.meta >= 0 && this.meta < 16;
                case NAME:
                    return Block.REGISTRY.getObject(new ResourceLocation(this.name)) != null;
                case NAME_META:
                    return Block.REGISTRY.getObject(new ResourceLocation(this.name)) != null && this.meta >= 0 && this.meta < 16;
                case NAME_PROPS:
                    Block block = Block.REGISTRY.getObject(new ResourceLocation(this.name));

                    if (block == null)
                    {
                        return false;
                    }

                    String str = this.name + "[" + this.props + "]";

                    for (IBlockState state : block.getBlockState().getValidStates())
                    {
                        if (state.toString().equals(str))
                        {
                            return true;
                        }
                    }

                    return false;
            }

            return false;
        }

        private void getNumericValues()
        {
            if (this.isValid())
            {
                switch (this.type)
                {
                    case NAME:
                        this.id = Block.getIdFromBlock(Block.REGISTRY.getObject(new ResourceLocation(this.name)));
                        break;
                    case NAME_META:
                        this.id = Block.getIdFromBlock(Block.REGISTRY.getObject(new ResourceLocation(this.name)));
                        break;
                    case NAME_PROPS:
                        Block block = Block.REGISTRY.getObject(new ResourceLocation(this.name));
                        String str = this.name + "[" + this.props + "]";

                        for (IBlockState state : block.getBlockState().getValidStates())
                        {
                            if (state.toString().equals(str))
                            {
                                int stateId = Block.getStateId(state);
                                this.id = stateId & 0xFFF;
                                this.meta = (stateId >> 12) & 0xF;
                                return;
                            }
                        }
                        break;
                    default:
                }
            }
        }

        public int getBlockstateId()
        {
            if (this.isValid())
            {
                switch (this.type)
                {
                    case ID:
                        return this.getId();
                    case ID_META:
                        return this.getId() | (this.getMeta() << 12);
                    case NAME:
                        return Block.getIdFromBlock(Block.REGISTRY.getObject(new ResourceLocation(this.name)));
                    case NAME_META:
                        return Block.getIdFromBlock(Block.REGISTRY.getObject(new ResourceLocation(this.name))) | this.getMeta() << 12;
                    case NAME_PROPS:
                        Block block = Block.REGISTRY.getObject(new ResourceLocation(this.name));
                        String str = this.toString();

                        for (IBlockState state : block.getBlockState().getValidStates())
                        {
                            if (state.toString().equals(str))
                            {
                                return Block.getStateId(state);
                            }
                        }

                        return 0;
                }
            }

            return 0;
        }

        @Override
        public String toString()
        {
            if (this.type == DataType.ID)
            {
                return "BlockData:{type=" + this.type + ",id=" + this.id + "}";
            }
            else if (this.type == DataType.ID_META)
            {
                return "BlockData:{type=" + this.type + ",id=" + this.id + ",meta=" + this.meta + "}";
            }
            else if (this.type == DataType.NAME)
            {
                return "BlockData:{type=" + this.type + ",name=" + this.name + ",id=" + this.id + "}";
            }
            else if (this.type == DataType.NAME_META)
            {
                return "BlockData:{type=" + this.type + ",name=" + this.name + ",meta=" + this.meta + ",id=" + this.id + "}";
            }
            else if (this.type == DataType.NAME_PROPS)
            {
                return "BlockData:{type=" + this.type + ",name=" + this.name + ",props=[" + this.props + "],id=" + this.id + ",meta=" + this.meta + "}";
            }
            else
            {
                return "BlockData:{type=INVALID}";
            }
        }
    }

    public static BlockData parseBlockTypeFromString(String str)
    {
        try
        {
            Pattern patternId        = Pattern.compile("(?<id>[0-9]+)");
            Pattern patternIdMeta    = Pattern.compile("(?<id>[0-9]+)[@:]{1}(?<meta>[0-9]+)");
            Pattern patternName      = Pattern.compile("(?<name>[a-z0-9_]+:[a-z0-9\\._]+)");
            Pattern patternNameMeta  = Pattern.compile("(?<name>[a-z0-9_]+:[a-z0-9\\._]+)[@:]{1}(?<meta>[0-9]+)");
            Pattern patternNameProps = Pattern.compile("(?<name>[a-z0-9_]+:[a-z0-9\\._]+)\\[(?<props>[a-z0-9_]+=[a-z0-9_]+(,[a-z0-9_]+=[a-z0-9_]+)*)\\]");

            Matcher matcherId = patternId.matcher(str);
            if (matcherId.matches())
            {
                System.out.printf("Type.ID - id: %d\n", Integer.parseInt(matcherId.group("id")));
                return new BlockData(Integer.parseInt(matcherId.group("id")));
            }

            Matcher matcherIdMeta = patternIdMeta.matcher(str);
            if (matcherIdMeta.matches())
            {
                // id@meta
                System.out.printf("Type.ID_META - id: %d, meta: %d\n",
                        Integer.parseInt(matcherIdMeta.group("id")), Integer.parseInt(matcherIdMeta.group("meta")));
                return new BlockData(Integer.parseInt(matcherIdMeta.group("id")), Integer.parseInt(matcherIdMeta.group("meta")));
            }

            Matcher matcherName = patternName.matcher(str);
            if (matcherName.matches())
            {
                System.out.printf("Type.NAME - name: %s\n", matcherName.group("name"));
                return new BlockData(matcherName.group("name"));
            }

            Matcher matcherNameMeta = patternNameMeta.matcher(str);
            if (matcherNameMeta.matches())
            {
                // name@meta
                System.out.printf("Type.NAME_META - name: %s, meta: %d\n",
                        matcherNameMeta.group("name"), Integer.parseInt(matcherNameMeta.group("meta")));
                return new BlockData(matcherNameMeta.group("name"), Integer.parseInt(matcherNameMeta.group("meta")));
            }

            Matcher matcherNameProps = patternNameProps.matcher(str);
            if (matcherNameProps.matches())
            {
                // name[props]
                String name = matcherNameProps.group("name");
                String propStr = matcherNameProps.group("props");
                //String propStr = str.substring(str.indexOf("[") + 1, str.length() - 1);
                String[] propParts = propStr.split(",");
                Pattern patternProp = Pattern.compile("([a-zA-Z0-9\\._-]+)=([a-zA-Z0-9\\._-]+)");
                List<String> props = new ArrayList<String>();

                for (int i = 0; i < propParts.length; i++)
                {
                    Matcher matcherProp = patternProp.matcher(propParts[i]);

                    if (matcherProp.matches())
                    {
                        props.add(propParts[i]);
                    }
                    else
                    {
                        WorldUtils.logger.warn("Invalid block property '{}'", propParts[i]);
                    }
                }

                Collections.sort(props); // the properties need to be in alphabetical order

                System.out.printf("Type.NAME_PROPS - name: %s, props: %s (propStr: %s)\n", name, String.join(",", props), propStr);
                return new BlockData(name, String.join(",", props));
            }
        }
        catch (PatternSyntaxException e)
        {
            WorldUtils.logger.warn("Invalid regex pattern in parseBlockTypeFromString()", e);
        }
        catch (Exception e)
        {
            WorldUtils.logger.warn("Failed to parse block type in parseBlockTypeFromString()", e);
        }

        return null;
    }

    public static List<String> getAllBlockNames()
    {
        List<String> names = new ArrayList<String>();

        for (ResourceLocation rl : Block.REGISTRY.getKeys())
        {
            names.add(rl.toString());
        }

        return names;
    }
}
