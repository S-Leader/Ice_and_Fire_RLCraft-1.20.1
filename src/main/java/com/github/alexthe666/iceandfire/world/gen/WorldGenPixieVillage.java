package com.github.alexthe666.iceandfire.world.gen;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.block.BlockPixieHouse;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.entity.EntityPixie;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import com.github.alexthe666.iceandfire.world.IafWorldData;
import com.github.alexthe666.iceandfire.world.IafWorldRegistry;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class WorldGenPixieVillage extends Feature<NoneFeatureConfiguration> implements TypedFeature {
    public WorldGenPixieVillage(Codec<NoneFeatureConfiguration> configFactoryIn) {
        super(configFactoryIn);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel worldIn = context.level();
        RandomSource rand = context.random();
        BlockPos position = WorldGenChunkSafety.centeredSurfaceOrigin(worldIn, context.origin(), Heightmap.Types.WORLD_SURFACE_WG);

        if (rand.nextInt(IafConfig.spawnPixiesChance) != 0 || !IafWorldRegistry.isFarEnoughFromSpawn(worldIn, position)) {
            return false;
        }

        int maxRoads = IafConfig.pixieVillageSize + rand.nextInt(5);
        BlockPos buildPosition = position;
        int placedRoads = 0;
        while(placedRoads < maxRoads){
            int roadLength = 10 + rand.nextInt(15);
            Direction buildingDirection = Direction.from2DDataValue(rand.nextInt(4));
            BlockPos lastSafeRoadPos = buildPosition;
            for(int i = 0; i < roadLength; i++) {
                BlockPos horizontalPos = buildPosition.relative(buildingDirection, i);
                if (!WorldGenChunkSafety.isSafe(position, horizontalPos)) {
                    break;
                }
                BlockPos buildPosition2 = worldIn.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, horizontalPos).below();
                if (!WorldGenChunkSafety.isSafe(position, buildPosition2)) {
                    break;
                }
                lastSafeRoadPos = horizontalPos;
                if (worldIn.getBlockState(buildPosition2).getFluidState().isEmpty()) {
                    worldIn.setBlock(buildPosition2, Blocks.DIRT_PATH.defaultBlockState(), 2);
                } else {
                    worldIn.setBlock(buildPosition2, Blocks.SPRUCE_PLANKS.defaultBlockState(), 2);
                }
                if (rand.nextInt(8) == 0) {
                    Direction houseDir = rand.nextBoolean() ? buildingDirection.getClockWise() : buildingDirection.getCounterClockWise();
                    BlockState houseState = IafBlockRegistry.PIXIE_HOUSE_OAK.get().defaultBlockState();
                    int houseColor = rand.nextInt(6);
                    houseState = switch (houseColor) {
                        case 0 -> IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_RED.get().defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 1 -> IafBlockRegistry.PIXIE_HOUSE_MUSHROOM_BROWN.get().defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 2 -> IafBlockRegistry.PIXIE_HOUSE_OAK.get().defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 3 -> IafBlockRegistry.PIXIE_HOUSE_BIRCH.get().defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 4 -> IafBlockRegistry.PIXIE_HOUSE_SPRUCE.get().defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        case 5 -> IafBlockRegistry.PIXIE_HOUSE_DARK_OAK.get().defaultBlockState().setValue(BlockPixieHouse.FACING, houseDir.getOpposite());
                        default -> houseState;
                    };
                    EntityPixie pixie = IafEntityRegistry.PIXIE.get().create(worldIn.getLevel());
                    pixie.finalizeSpawn(worldIn, worldIn.getCurrentDifficultyAt(buildPosition2.above()), MobSpawnType.SPAWNER, null, null);
                    pixie.setPos(buildPosition2.getX(), buildPosition2.getY() + 2, buildPosition2.getZ());
                    pixie.setPersistenceRequired();
                    worldIn.addFreshEntity(pixie);

                    BlockPos housePos = buildPosition2.relative(houseDir);
                    if (WorldGenChunkSafety.isSafe(position, housePos)) {
                        worldIn.setBlock(housePos.above(), houseState, 2);
                        if (!worldIn.getBlockState(housePos).canOcclude()) {
                            worldIn.setBlock(housePos, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                            worldIn.setBlock(housePos.below(), Blocks.COARSE_DIRT.defaultBlockState(), 2);
                        }
                    }
                }
            }
            buildPosition = lastSafeRoadPos;
            placedRoads++;
        }

        return true;
    }

    @Override
    public IafWorldData.FeatureType getFeatureType() {
        return IafWorldData.FeatureType.SURFACE;
    }

    @Override
    public String getId() {
        return "pixie_village";
    }
}
