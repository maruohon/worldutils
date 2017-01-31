package fi.dy.masa.worldutils.util;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockSand;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockWoodSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

public class VanillaBlocks
{
    public enum VanillaVersion
    {
        VANILLA_1_5,
        VANILLA_1_6,
        VANILLA_1_7,
        VANILLA_1_8,
        VANILLA_1_9,
        VANILLA_1_10,
        VANILLA_1_11;

        @Nullable
        public static VanillaVersion fromVersion(String version)
        {
            version = "VANILLA_" + version.replace(".", "_");

            for (VanillaVersion v : values())
            {
                if (v.name().equals(version))
                {
                    return v;
                }
            }

            return null;
        }
    }

    public static void addVanillaBlocks_pre_1_6(List<IBlockState> blockStateList)
    {
        blockStateList.addAll(Blocks.ACTIVATOR_RAIL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.AIR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.ANVIL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BEACON.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BED.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BEDROCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BOOKSHELF.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BREWING_STAND.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BRICK_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BRICK_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BROWN_MUSHROOM.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.BROWN_MUSHROOM_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CACTUS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CAKE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CARROTS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CAULDRON.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CHEST.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CLAY.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.COAL_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.COBBLESTONE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.COBBLESTONE_WALL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.COCOA.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.COMMAND_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CRAFTING_TABLE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DAYLIGHT_DETECTOR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DAYLIGHT_DETECTOR_INVERTED.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DEADBUSH.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DETECTOR_RAIL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DIAMOND_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DIAMOND_ORE.getBlockState().getValidStates());
        blockStateList.add(Blocks.DIRT.getDefaultState());
        blockStateList.addAll(Blocks.DISPENSER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DOUBLE_STONE_SLAB.getBlockState().getValidStates());

        blockStateList.add(Blocks.DOUBLE_WOODEN_SLAB.getDefaultState().withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.OAK));
        blockStateList.add(Blocks.DOUBLE_WOODEN_SLAB.getDefaultState().withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.SPRUCE));
        blockStateList.add(Blocks.DOUBLE_WOODEN_SLAB.getDefaultState().withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.BIRCH));
        blockStateList.add(Blocks.DOUBLE_WOODEN_SLAB.getDefaultState().withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.JUNGLE));

        blockStateList.addAll(Blocks.DRAGON_EGG.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DROPPER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.EMERALD_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.EMERALD_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.ENCHANTING_TABLE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.END_PORTAL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.END_PORTAL_FRAME.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.END_STONE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.ENDER_CHEST.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.FARMLAND.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.FIRE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.FLOWER_POT.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.FLOWING_LAVA.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.FLOWING_WATER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.FURNACE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GLASS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GLASS_PANE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GLOWSTONE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GOLD_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GOLD_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GOLDEN_RAIL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GRASS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GRAVEL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.HOPPER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.ICE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.IRON_BARS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.IRON_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.IRON_DOOR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.IRON_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.JUKEBOX.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LADDER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LAPIS_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LAPIS_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LAVA.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LEAVES.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LEVER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LIT_FURNACE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LIT_PUMPKIN.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LIT_REDSTONE_LAMP.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LIT_REDSTONE_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LOG.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.MELON_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.MELON_STEM.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.MOB_SPAWNER.getBlockState().getValidStates());

        blockStateList.add(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONE));
        blockStateList.add(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.COBBLESTONE));
        blockStateList.add(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.STONEBRICK));

        blockStateList.addAll(Blocks.MOSSY_COBBLESTONE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.MYCELIUM.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.NETHER_BRICK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.NETHER_BRICK_FENCE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.NETHER_BRICK_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.NETHER_WART.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.NETHERRACK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.NOTEBLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.OAK_DOOR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.OAK_FENCE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.OAK_FENCE_GATE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.OAK_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.OBSIDIAN.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PISTON.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PISTON_EXTENSION.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PISTON_HEAD.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PLANKS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PORTAL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.POTATOES.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.POWERED_COMPARATOR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.POWERED_REPEATER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PUMPKIN.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PUMPKIN_STEM.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.QUARTZ_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.QUARTZ_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.QUARTZ_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.RAIL.getBlockState().getValidStates());
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState());
        blockStateList.addAll(Blocks.RED_MUSHROOM.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.RED_MUSHROOM_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.REDSTONE_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.REDSTONE_LAMP.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.REDSTONE_ORE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.REDSTONE_TORCH.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.REDSTONE_WIRE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.REEDS.getBlockState().getValidStates());
        blockStateList.add(Blocks.SAND.getDefaultState());
        blockStateList.addAll(Blocks.SANDSTONE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.SANDSTONE_STAIRS.getBlockState().getValidStates());

        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 0).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.OAK));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 1).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.OAK));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 0).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.SPRUCE));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 1).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.SPRUCE));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 0).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.BIRCH));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 1).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.BIRCH));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 0).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.JUNGLE));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 1).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.JUNGLE));

        blockStateList.addAll(Blocks.SKULL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.SNOW.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.SNOW_LAYER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.SOUL_SAND.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.SPONGE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STANDING_SIGN.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STICKY_PISTON.getBlockState().getValidStates());
        blockStateList.add(Blocks.STONE.getDefaultState());
        blockStateList.addAll(Blocks.STONE_BRICK_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STONE_BUTTON.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STONE_PRESSURE_PLATE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STONE_SLAB.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STONE_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STONEBRICK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.TALLGRASS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.TNT.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.TORCH.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.TRAPDOOR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.TRAPPED_CHEST.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.TRIPWIRE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.TRIPWIRE_HOOK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.UNLIT_REDSTONE_TORCH.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.UNPOWERED_COMPARATOR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.UNPOWERED_REPEATER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.VINE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.WALL_SIGN.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.WATER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.WATERLILY.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.WEB.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.WHEAT.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.WOODEN_BUTTON.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.WOODEN_PRESSURE_PLATE.getBlockState().getValidStates());

        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.OAK));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.OAK));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.SPRUCE));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.SPRUCE));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.BIRCH));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.BIRCH));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.JUNGLE));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.JUNGLE));

        blockStateList.addAll(Blocks.WOOL.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.YELLOW_FLOWER.getBlockState().getValidStates());
    }

    public static void addNewVanillaBlockStatesIn_1_6(List<IBlockState> blockStateList)
    {
        blockStateList.addAll(Blocks.CARPET.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.COAL_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.HAY_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.HARDENED_CLAY.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STAINED_HARDENED_CLAY.getBlockState().getValidStates());
    }

    public static void addNewVanillaBlockStatesIn_1_7(List<IBlockState> blockStateList)
    {
        // coarse dirt was added in 1.8, but "grassless dirt" in 1.7, and they use the same metadata 1
        blockStateList.add(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT));
        blockStateList.add(Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.PODZOL));
        blockStateList.addAll(Blocks.DOUBLE_PLANT.getBlockState().getValidStates());
        blockStateList.add(Blocks.DOUBLE_WOODEN_SLAB.getDefaultState().withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.ACACIA));
        blockStateList.add(Blocks.DOUBLE_WOODEN_SLAB.getDefaultState().withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.DARK_OAK));
        blockStateList.addAll(Blocks.LEAVES2.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.LOG2.getBlockState().getValidStates());
        blockStateList.add(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.CHISELED_STONEBRICK));
        blockStateList.add(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.CRACKED_STONEBRICK));
        blockStateList.add(Blocks.MONSTER_EGG.getDefaultState().withProperty(BlockSilverfish.VARIANT, BlockSilverfish.EnumType.MOSSY_STONEBRICK));
        blockStateList.addAll(Blocks.PACKED_ICE.getBlockState().getValidStates());
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.ALLIUM));
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.BLUE_ORCHID));
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.HOUSTONIA));
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.ORANGE_TULIP));
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.OXEYE_DAISY));
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.PINK_TULIP));
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.RED_TULIP));
        blockStateList.add(Blocks.RED_FLOWER.getDefaultState().withProperty(Blocks.RED_FLOWER.getTypeProperty(), BlockFlower.EnumFlowerType.WHITE_TULIP));
        blockStateList.add(Blocks.SAND.getDefaultState().withProperty(BlockSand.VARIANT, BlockSand.EnumType.RED_SAND));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 0).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.ACACIA));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 1).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.ACACIA));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 0).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.DARK_OAK));
        blockStateList.add(Blocks.SAPLING.getDefaultState().withProperty(BlockSapling.STAGE, 1).withProperty(BlockSapling.TYPE, BlockPlanks.EnumType.DARK_OAK));
        blockStateList.addAll(Blocks.STAINED_GLASS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STAINED_GLASS_PANE.getBlockState().getValidStates());
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.ACACIA));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.ACACIA));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.DARK_OAK));
        blockStateList.add(Blocks.WOODEN_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM).withProperty(BlockWoodSlab.VARIANT, BlockPlanks.EnumType.DARK_OAK));
    }

    public static void addNewVanillaBlockStatesIn_1_8(List<IBlockState> blockStateList)
    {
        blockStateList.addAll(Blocks.BARRIER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.DOUBLE_STONE_SLAB2.getBlockState().getValidStates()); // Red Sandstone Slab
        blockStateList.addAll(Blocks.IRON_TRAPDOOR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PRISMARINE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.RED_SANDSTONE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.RED_SANDSTONE_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.SEA_LANTERN.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.SLIME_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STANDING_BANNER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STONE_SLAB2.getBlockState().getValidStates()); // Red Sandstone Slab
        blockStateList.addAll(Blocks.WALL_BANNER.getBlockState().getValidStates());

        blockStateList.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE));
        blockStateList.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE_SMOOTH));
        blockStateList.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE));
        blockStateList.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE_SMOOTH));
        blockStateList.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE));
        blockStateList.add(Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE_SMOOTH));

        blockStateList.addAll(Blocks.ACACIA_DOOR.getBlockState().getValidStates());
        blockStateList.add(Blocks.ACACIA_FENCE.getDefaultState());
        blockStateList.addAll(Blocks.ACACIA_FENCE_GATE.getBlockState().getValidStates());

        blockStateList.addAll(Blocks.BIRCH_DOOR.getBlockState().getValidStates());
        blockStateList.add(Blocks.BIRCH_FENCE.getDefaultState());
        blockStateList.addAll(Blocks.BIRCH_FENCE_GATE.getBlockState().getValidStates());

        blockStateList.addAll(Blocks.DARK_OAK_DOOR.getBlockState().getValidStates());
        blockStateList.add(Blocks.DARK_OAK_FENCE.getDefaultState());
        blockStateList.addAll(Blocks.DARK_OAK_FENCE_GATE.getBlockState().getValidStates());

        blockStateList.addAll(Blocks.JUNGLE_DOOR.getBlockState().getValidStates());
        blockStateList.add(Blocks.JUNGLE_FENCE.getDefaultState());
        blockStateList.addAll(Blocks.JUNGLE_FENCE_GATE.getBlockState().getValidStates());

        blockStateList.addAll(Blocks.SPRUCE_DOOR.getBlockState().getValidStates());
        blockStateList.add(Blocks.SPRUCE_FENCE.getDefaultState());
        blockStateList.addAll(Blocks.SPRUCE_FENCE_GATE.getBlockState().getValidStates());
    }

    public static void addNewVanillaBlockStatesIn_1_9(List<IBlockState> blockStateList)
    {
        blockStateList.addAll(Blocks.BEETROOTS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CHAIN_COMMAND_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CHORUS_FLOWER.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.CHORUS_PLANT.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.END_GATEWAY.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.END_ROD.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.END_BRICKS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.FROSTED_ICE.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.GRASS_PATH.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PURPUR_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PURPUR_PILLAR.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PURPUR_SLAB.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PURPUR_DOUBLE_SLAB.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.PURPUR_STAIRS.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.REPEATING_COMMAND_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.STRUCTURE_BLOCK.getBlockState().getValidStates());
    }

    public static void addNewVanillaBlockStatesIn_1_10(List<IBlockState> blockStateList)
    {
        blockStateList.addAll(Blocks.BONE_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.MAGMA.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.NETHER_WART_BLOCK.getBlockState().getValidStates());
        blockStateList.addAll(Blocks.RED_NETHER_BRICK.getBlockState().getValidStates());
    }

    public static void addNewVanillaBlockStatesIn_1_11(List<IBlockState> blockStateList)
    {
        //blockStateList.add(Blocks.OBSERVER.getDefaultState());

        /*for (EnumDyeColor color : EnumDyeColor.values())
        {
            list_1_11.add("minecraft:" + color.getName() + "_shulker_box");
        }*/
    }

    /**
     * Returns a list of all valid vanilla blockstates that existed in the requested game version.
     * The list includes only states that serialize into different metadata values, so it doesn't include
     * states that are run-time calculated in getActualState(). Note however that this might not strictly
     * be the case for all blocks, some blocks may also include some run-time-only states...
     * @param version
     * @return a list of all states that serialize into different metadata values on disk
     */
    public static List<IBlockState> getSerializableVanillaBlockStatesInVersion(VanillaVersion version)
    {
        List<IBlockState> list = new ArrayList<IBlockState>();

        // NOTE: No break statements!!
        switch (version)
        {
            case VANILLA_1_11:
                addNewVanillaBlockStatesIn_1_11(list);

            case VANILLA_1_10:
                addNewVanillaBlockStatesIn_1_10(list);

            case VANILLA_1_9:
                addNewVanillaBlockStatesIn_1_9(list);

            case VANILLA_1_8:
                addNewVanillaBlockStatesIn_1_8(list);

            case VANILLA_1_7:
                addNewVanillaBlockStatesIn_1_7(list);

            case VANILLA_1_6:
                addNewVanillaBlockStatesIn_1_6(list);

            default:
                addVanillaBlocks_pre_1_6(list);
        }

        return list;
    }
}
