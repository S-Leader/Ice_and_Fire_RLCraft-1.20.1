package com.github.alexthe666.iceandfire.world.gen;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Keeps legacy Feature-based Ice and Fire worldgen inside the 3x3 chunk area
 * available while the FEATURES generation step is running.
 */
public final class WorldGenChunkSafety {
    private WorldGenChunkSafety() {
    }

    public static boolean isSafe(BlockPos featureOrigin, BlockPos target) {
        ChunkPos originChunk = new ChunkPos(featureOrigin);
        ChunkPos targetChunk = new ChunkPos(target);
        return Math.abs(originChunk.x - targetChunk.x) <= 1
            && Math.abs(originChunk.z - targetChunk.z) <= 1;
    }

    public static boolean isBoxSafe(BlockPos featureOrigin, BlockPos center, int horizontalRadius) {
        return isSafe(featureOrigin, center.offset(horizontalRadius, 0, horizontalRadius))
            && isSafe(featureOrigin, center.offset(horizontalRadius, 0, -horizontalRadius))
            && isSafe(featureOrigin, center.offset(-horizontalRadius, 0, horizontalRadius))
            && isSafe(featureOrigin, center.offset(-horizontalRadius, 0, -horizontalRadius));
    }

    public static BlockPos centeredSurfaceOrigin(WorldGenLevel level, BlockPos original, Heightmap.Types heightmap) {
        int x = ((original.getX() >> 4) << 4) + 8;
        int z = ((original.getZ() >> 4) << 4) + 8;
        return level.getHeightmapPos(heightmap, new BlockPos(x, original.getY(), z));
    }
}
