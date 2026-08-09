package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Exclusive night-time return-to-roost movement.
 *
 * Combat has priority over sleeping/returning.  The home position is only the centre of the
 * roost; it is NOT assumed to remain a valid landing block forever.  Players can excavate a
 * roost, so this goal searches for a temporary safe landing position near home and can replace
 * that position while the dragon is already descending.
 */
public class DragonAIReturnToRoost extends Goal {

    private final EntityDragonBase dragon;
    private BlockPos landingTarget;
    private int landingSearchCooldown;

    public DragonAIReturnToRoost(EntityDragonBase entityIn, double movementSpeedIn) {
        this.dragon = entityIn;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!dragon.canMove()
                || !dragon.lookingForRoostAIFlag
                || (dragon.getTarget() != null && dragon.getTarget().isAlive())
                || dragon.getRestrictCenter() == null
                || !DragonUtils.isInHomeDimension(dragon)) {
            return false;
        }

        // Keep the goal alive until the dragon has actually landed.  A destroyed home floor must
        // not make the goal stop just because X/Z is already close to the original home position.
        return !dragon.onGround() || !isNearLandingTarget();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        dragon.setTackling(false);
        dragon.getNavigation().stop();
        landingTarget = null;
        landingSearchCooldown = 0;
    }

    @Override
    public void stop() {
        // Do not leave an old roost waypoint behind for DragonAIAirTarget to inherit.
        dragon.airTarget = null;
        dragon.getNavigation().stop();
        landingTarget = null;
        landingSearchCooldown = 0;
    }

    @Override
    public void tick() {
        final BlockPos home = dragon.getRestrictCenter();
        if (home == null || (dragon.getTarget() != null && dragon.getTarget().isAlive())) {
            return;
        }

        // The nest floor is mutable world state.  Revalidate while approaching/descending so a
        // player digging the selected block out cannot leave the dragon suspended forever.
        if (landingTarget == null || !isSafeLandingSpot(landingTarget) || landingSearchCooldown-- <= 0) {
            landingTarget = findSafeLandingPosition(home);
            landingSearchCooldown = 20;
        }

        if (landingTarget == null) {
            // The whole nearby roost may have been excavated (or be over a very deep ravine).
            // Never enter an endless Hover/Landing state over void.  Keep flying and periodically
            // retry the search; this also lets the dragon react immediately if a floor is rebuilt.
            dragon.setHovering(false);
            dragon.setFlying(true);
            dragon.getNavigation().stop();
            dragon.airTarget = getFallbackAirTarget(home);
            return;
        }

        final double targetX = landingTarget.getX() + 0.5D;
        final double targetZ = landingTarget.getZ() + 0.5D;
        final double dx = targetX - dragon.getX();
        final double dz = targetZ - dragon.getZ();
        final double xzDist = Math.sqrt(dx * dx + dz * dz);
        final double landingRadius = getLandingRadius();

        // Close to an ACTUAL valid floor: enter explicit descent.  We intentionally steer toward
        // the selected landing point rather than the original home X/Z, because the latter may be
        // a hole now.
        if (xzDist <= landingRadius) {
            dragon.airTarget = null;
            dragon.getNavigation().stop();
            dragon.setFlying(false);

            if (!dragon.onGround()) {
                dragon.setHovering(true);

                Vec3 motion = dragon.getDeltaMovement();
                double horizontalLen = Math.max(0.001D, xzDist);
                double steerX = dx / horizontalLen * Math.min(0.18D, xzDist * 0.04D);
                double steerZ = dz / horizontalLen * Math.min(0.18D, xzDist * 0.04D);

                // Do not dive far below the selected floor if some other movement controller
                // touches Y velocity during the same tick.  Near the floor we use a gentler sink.
                double floorY = landingTarget.getY();
                double feetY = dragon.getBoundingBox().minY;
                double verticalGap = feetY - floorY;
                double desiredSink = verticalGap > 3.0D ? -0.30D : -0.12D;
                double descendY = Math.min(motion.y - 0.12D, desiredSink);

                dragon.setDeltaMovement(
                        motion.x * 0.55D + steerX,
                        Mth.clamp(descendY, -0.45D, -0.08D),
                        motion.z * 0.55D + steerZ
                );
            } else {
                dragon.setHovering(false);
                dragon.setFlying(false);
                dragon.hoverTicks = 0;
                dragon.flyTicks = 0;
                dragon.setDeltaMovement(dragon.getDeltaMovement().x * 0.4D, 0.0D, dragon.getDeltaMovement().z * 0.4D);
            }
            return;
        }

        // Cruise toward the safe landing spot rather than toward the possibly-destroyed home
        // block.  Far away we approach high; closer in we lower the approach altitude.
        if (!dragon.isFlying()) {
            dragon.setHovering(false);
            dragon.setFlying(true);
        }

        int extraHeight = xzDist > 40.0D
                ? 15 + dragon.getRandom().nextInt(3)
                : Math.max(5, Mth.ceil(dragon.getBbHeight() * 0.5D));
        BlockPos flightTarget = landingTarget.above(extraHeight);

        final int maxFlightY = DragonUtils.getMaximumFlightHeightForPos(dragon.level(), landingTarget);
        if (flightTarget.getY() > maxFlightY) {
            flightTarget = new BlockPos(flightTarget.getX(), maxFlightY, flightTarget.getZ());
        }
        while (!dragon.level().isEmptyBlock(flightTarget) && flightTarget.getY() < maxFlightY) {
            flightTarget = flightTarget.above();
        }

        dragon.airTarget = flightTarget;
        dragon.getNavigation().stop();
    }

    /**
     * Searches outwards from the stored roost centre.  The home itself is tried first, then rings
     * around it.  The scan is intentionally local: destroying a nest should make the dragon land
     * beside it, not relocate its home hundreds of blocks away.
     */
    private BlockPos findSafeLandingPosition(BlockPos home) {
        final int searchRadius = Mth.clamp(Mth.ceil(dragon.getBbWidth() * 3.0F), 12, 32);
        final int step = Mth.clamp(Mth.ceil(dragon.getBbWidth() * 0.5F), 2, 4);

        BlockPos direct = findLandingInColumn(home.getX(), home.getZ(), home.getY());
        if (direct != null) {
            return direct;
        }

        for (int radius = step; radius <= searchRadius; radius += step) {
            for (int offset = -radius; offset <= radius; offset += step) {
                BlockPos result = findLandingInColumn(home.getX() + offset, home.getZ() - radius, home.getY());
                if (result != null) {
                    return result;
                }
                result = findLandingInColumn(home.getX() + offset, home.getZ() + radius, home.getY());
                if (result != null) {
                    return result;
                }
            }
            for (int offset = -radius + step; offset <= radius - step; offset += step) {
                BlockPos result = findLandingInColumn(home.getX() - radius, home.getZ() + offset, home.getY());
                if (result != null) {
                    return result;
                }
                result = findLandingInColumn(home.getX() + radius, home.getZ() + offset, home.getY());
                if (result != null) {
                    return result;
                }
            }
        }

        // Last local fallback: if the original nest is a deep vertical shaft, try finding a floor
        // below the dragon's current column before giving up and continuing to fly.
        return findLandingBelowCurrentPosition();
    }

    private BlockPos findLandingInColumn(int x, int z, int preferredY) {
        final int minY = dragon.level().getMinBuildHeight() + 1;
        final int maxY = dragon.level().getMaxBuildHeight() - 2;
        final int down = Math.max(24, Mth.ceil(dragon.getBbHeight() * 3.0F));

        // Prefer a floor at or below the original nest height.  This keeps underground cave dragons
        // underground instead of selecting the terrain surface above their cave roof.
        int startY = Mth.clamp(preferredY + 2, minY, maxY);
        int endY = Math.max(minY, preferredY - down);
        for (int y = startY; y >= endY; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isSafeLandingSpot(candidate)) {
                return candidate;
            }
        }

        // Nearby higher terrain is also valid (e.g. the centre of a surface roost was dug out but
        // its rim is intact), but do not climb arbitrarily far above an underground home.
        int upperEnd = Math.min(maxY, preferredY + 12);
        for (int y = startY + 1; y <= upperEnd; y++) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isSafeLandingSpot(candidate)) {
                return candidate;
            }
        }

        // Heightmap is a cheap final check for normal surface roosts.  Ignore a surface that is too
        // far vertically from home so an underground dragon is not redirected onto the mountain top.
        BlockPos surface = dragon.level().getHeightmapPos(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, preferredY, z)
        );
        if (Math.abs(surface.getY() - preferredY) <= 24 && isSafeLandingSpot(surface)) {
            return surface;
        }
        return null;
    }

    private BlockPos findLandingBelowCurrentPosition() {
        final int x = dragon.getBlockX();
        final int z = dragon.getBlockZ();
        final int minY = dragon.level().getMinBuildHeight() + 1;
        final int startY = Math.min(dragon.level().getMaxBuildHeight() - 2, Mth.floor(dragon.getBoundingBox().minY));
        final int maxScan = Math.max(32, Mth.ceil(dragon.getBbHeight() * 5.0F));
        final int endY = Math.max(minY, startY - maxScan);

        for (int y = startY; y >= endY; y--) {
            BlockPos candidate = new BlockPos(x, y, z);
            if (isSafeLandingSpot(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isSafeLandingSpot(BlockPos pos) {
        if (pos == null
                || pos.getY() <= dragon.level().getMinBuildHeight()
                || pos.getY() >= dragon.level().getMaxBuildHeight() - Mth.ceil(dragon.getBbHeight()) - 1) {
            return false;
        }

        BlockPos supportPos = pos.below();
        BlockState support = dragon.level().getBlockState(supportPos);
        if (support.isAir()
                || !support.getFluidState().isEmpty()
                || !support.isFaceSturdy(dragon.level(), supportPos, Direction.UP)
                || !dragon.level().getFluidState(pos).isEmpty()) {
            return false;
        }

        // Validate the whole dragon body, not only one air block above the floor.  This prevents a
        // stage-5 dragon from choosing a tiny pocket/ledge that its full collision box cannot fit.
        double targetX = pos.getX() + 0.5D;
        double targetZ = pos.getZ() + 0.5D;
        AABB landingBox = dragon.getBoundingBox().move(
                targetX - dragon.getX(),
                pos.getY() - dragon.getBoundingBox().minY,
                targetZ - dragon.getZ()
        ).deflate(0.05D);
        return dragon.level().noCollision(dragon, landingBox);
    }

    private BlockPos getFallbackAirTarget(BlockPos home) {
        int maxFlightY = DragonUtils.getMaximumFlightHeightForPos(dragon.level(), home);
        int baseY = Math.min(maxFlightY, Math.max(home.getY() + 10, Mth.floor(dragon.getY())));

        // Slowly alternate around the home instead of hovering on one exact coordinate forever.
        int phase = (dragon.tickCount / 40) & 3;
        int radius = Math.max(8, Mth.ceil(dragon.getBbWidth() * 1.5F));
        int dx = phase == 0 ? radius : phase == 2 ? -radius : 0;
        int dz = phase == 1 ? radius : phase == 3 ? -radius : 0;
        BlockPos target = new BlockPos(home.getX() + dx, baseY, home.getZ() + dz);

        while (!dragon.level().isEmptyBlock(target) && target.getY() < maxFlightY) {
            target = target.above();
        }
        return target;
    }

    private double getLandingRadius() {
        return Math.max(4.0D, dragon.getBbWidth() * 0.9D);
    }

    private boolean isNearLandingTarget() {
        if (landingTarget == null) {
            return false;
        }
        double dx = dragon.getX() - (landingTarget.getX() + 0.5D);
        double dz = dragon.getZ() - (landingTarget.getZ() + 0.5D);
        double radius = getLandingRadius();
        return dx * dx + dz * dz <= radius * radius;
    }
}
