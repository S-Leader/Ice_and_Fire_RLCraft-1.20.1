package com.github.alexthe666.iceandfire.entity.util;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.datagen.tags.IafBlockTags;
import com.github.alexthe666.iceandfire.entity.*;
import com.github.alexthe666.iceandfire.misc.IafTagRegistry;
import com.google.common.base.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class DragonUtils {
    // 护航飞行高度：比巡航低，便于跟随主人
    private static int getEscortFlightHeight() {
        return Math.max(8, IafConfig.dragonFlightHeight * 2 / 3);
    }

    // 骏鹰体型小，巡航高度取龙的一半
    private static int getHippogryphFlightHeight() {
        return Math.max(8, IafConfig.dragonFlightHeight / 2);
    }

    /**
     * 空战追踪选点：近距离(≤32格)直扑目标实时位置；
     * 远距离绕目标随机盘旋点逐步接近，仅在无点或到达后重选，避免每tick换点导致乱飞。
     */
    public static BlockPos updateAirAttackTarget(EntityDragonBase dragon, LivingEntity target, @Nullable BlockPos current) {
        // 32格内直扑，交由战斗逻辑俯冲/吐息
        if (dragon.distanceToSqr(target.getX(), dragon.getY(), target.getZ()) <= 1024) {
            return target.blockPosition();
        }
        // 到达当前盘旋点（或无点）时，在目标周围16~31格、上方16格处选新点
        if (current == null || dragon.distanceToSqr(Vec3.atCenterOf(current)) < 16) {
            double angle = Math.PI * 2 * dragon.getRandom().nextDouble();
            double dist = 16 + dragon.getRandom().nextInt(16);
            return BlockPos.containing(
                target.getX() + Math.sin(angle) * dist,
                target.getY() + 16,
                target.getZ() + Math.cos(angle) * dist);
        }
        return current;
    }

    // 获取位置的最大飞行高度（修正公式，基于地面高度+相对偏移，且不超过绝对上限）
    public static int getMaximumFlightHeightForPos(Level world, BlockPos pos) {
        int maxFlight = IafConfig.maxDragonFlight;
        int groundHeight = world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        // 巡航高度上方留16格余量，但不超过配置的绝对上限
        int relativeMaxHeight = groundHeight + IafConfig.dragonFlightHeight + 16;
        return Math.min(maxFlight, relativeMaxHeight);
    }

    public static BlockPos getBlockInViewEscort(EntityDragonBase dragon) {
        BlockPos escortPos;
        LivingEntity owner = dragon.getOwner();
        if (owner != null) {
            Vec3 ownerVel = new Vec3(owner.getX() - owner.xo, owner.getY() - owner.yo, owner.getZ() - owner.zo);
            double projectedX = owner.getX() + ownerVel.x * 15;
            double projectedY = owner.getY() + ownerVel.y * 15;
            double projectedZ = owner.getZ() + ownerVel.z * 15;
            escortPos = BlockPos.containing(projectedX, projectedY, projectedZ);
        } else {
            escortPos = dragon.getEscortPosition();
        }

        BlockPos ground = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, escortPos);
        int maxHeight = getMaximumFlightHeightForPos(dragon.level(), escortPos);
        
        int wanderDist = Math.max(5, IafConfig.dragonWanderFromHomeDistance / 2);
        for (int i = 0; i < 10; i++) {
            // 护航飞行高度固定，避免随机高度导致上下颠簸
            int targetY = Math.min(maxHeight, ground.getY() + getEscortFlightHeight());
            BlockPos pos = new BlockPos(
                escortPos.getX() + dragon.getRandom().nextInt(wanderDist) - wanderDist / 2,
                targetY,
                (escortPos.getZ() + dragon.getRandom().nextInt(wanderDist) - wanderDist / 2));
            if (dragon.distanceToSqr(Vec3.atCenterOf(pos)) > 6 && !dragon.isTargetBlocked(Vec3.atCenterOf(pos))) {
                return pos;
            }
        }
        return null;
    }

    public static BlockPos getWaterBlockInViewEscort(EntityDragonBase dragon) {
        // In water escort
        BlockPos inWaterEscortPos = dragon.getEscortPosition();
        // We don't need to get too close
        if (Math.abs(dragon.getX() - inWaterEscortPos.getX()) < dragon.getBoundingBox().getXsize()
                && Math.abs(dragon.getZ() - inWaterEscortPos.getZ()) < dragon.getBoundingBox().getZsize()) {
            return dragon.blockPosition();
        }
        // Takes off if the escort position is no longer in water, mainly for using elytra to fly out of the water
        if (inWaterEscortPos.getY() - dragon.getY() > 8 + dragon.getYNavSize() && !dragon.level().getFluidState(inWaterEscortPos.below()).is(FluidTags.WATER)) {
            dragon.setHovering(true);
        }
        // Swim directly to the escort position
        return inWaterEscortPos;
    }

    public static BlockPos getBlockInView(EntityDragonBase dragon) {
        // 1.12.2 RLI&F semantics: a dragon with a home position does not run a separate
        // "return to roost" goal. Its ordinary aerial wandering is simply centered on home.
        // This naturally brings a roaming dragon back toward the nest without fighting combat AI.
        float radius = 0.75F * (0.7F * dragon.getRenderSize() / 3) * -7
                - dragon.getRandom().nextInt(Math.max(1, dragon.getDragonStage() * 6));
        float neg = dragon.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = dragon.yBodyRot;
        int maximumFlightHeight = getMaximumFlightHeightForPos(dragon.level(), dragon.blockPosition());

        if (dragon.hasHomePosition && dragon.homePos != null) {
            BlockPos dragonPos = dragon.blockPosition();
            BlockPos ground = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dragonPos);
            int distFromGround = Mth.floor(dragon.getY()) - ground.getY();
            int wander = Math.max(1, IafConfig.dragonWanderFromHomeDistance);

            for (int i = 0; i < 10; i++) {
                BlockPos home = dragon.homePos.getPosition();
                int targetY = distFromGround > 16
                        ? (int) Math.min(maximumFlightHeight, dragon.getY() + dragon.getRandom().nextInt(17) - 8)
                        : Mth.floor(dragon.getY()) + dragon.getRandom().nextInt(16) + 1;
                targetY = Math.min(targetY, maximumFlightHeight);

                BlockPos pos = new BlockPos(
                        home.getX() + dragon.getRandom().nextInt(wander) - wander,
                        targetY,
                        home.getZ() + dragon.getRandom().nextInt(wander * 2) - wander
                );
                if (!dragon.isTargetBlocked(Vec3.atCenterOf(pos))
                        && dragon.distanceToSqr(Vec3.atCenterOf(pos)) > 6) {
                    return pos;
                }
            }
        }

        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (dragon.getRandom().nextFloat() * neg);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = BlockPos.containing(dragon.getX() + extraX, 0, dragon.getZ() + extraZ);
        BlockPos ground = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, radialPos);
        int distFromGround = Mth.floor(dragon.getY()) - ground.getY();
        int targetY = distFromGround > 16
                ? (int) Math.min(getMaximumFlightHeightForPos(dragon.level(), radialPos), dragon.getY() + dragon.getRandom().nextInt(17) - 8)
                : Mth.floor(dragon.getY()) + dragon.getRandom().nextInt(16) + 1;
        BlockPos newPos = new BlockPos(radialPos.getX(), targetY, radialPos.getZ());
        BlockPos pos = dragon.doesWantToLand() ? ground : newPos;
        if (dragon.distanceToSqr(Vec3.atCenterOf(newPos)) > 6
                && !dragon.isTargetBlocked(Vec3.atCenterOf(newPos))) {
            return pos;
        }
        return null;
    }

    public static BlockPos getWaterBlockInView(EntityDragonBase dragon) {
        float radius = 0.75F * (0.7F * dragon.getRenderSize() / 3) * -7 - dragon.getRandom().nextInt(dragon.getDragonStage() * 6);
        float neg = dragon.getRandom().nextBoolean() ? 1 : -1;
        float angle = (0.01745329251F * dragon.yBodyRot) + 3.15F + (dragon.getRandom().nextFloat() * neg);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = BlockPos.containing(dragon.getX() + extraX, 0, dragon.getZ() + extraZ);
        BlockPos ground = dragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, radialPos);
        int maxHeight = getMaximumFlightHeightForPos(dragon.level(), radialPos);
        // 巡航高度固定为配置值，避免随机高度导致上下颠簸
        int targetY = Math.min(maxHeight, ground.getY() + IafConfig.dragonFlightHeight);
        BlockPos newPos = new BlockPos(radialPos.getX(), targetY, radialPos.getZ());
        BlockPos pos = dragon.doesWantToLand() ? ground : newPos;
        BlockPos surface = dragon.level().getFluidState(newPos.below(2)).is(FluidTags.WATER) ? newPos.below(dragon.getRandom().nextInt(10) + 1) : newPos;
        
        if (dragon.getRandom().nextInt(5) == 0) {
            // 偶尔跃出水面：高度固定，去除随机
            return surface.above(15);
        }

        if (dragon.distanceToSqr(Vec3.atCenterOf(surface)) > 6 && dragon.level().getFluidState(surface).is(FluidTags.WATER)) {
            return surface;
        }
        return null;
    }

    public static LivingEntity riderLookingAtEntity(LivingEntity dragon, LivingEntity rider, double dist) {
        Vec3 Vector3d = rider.getEyePosition(1.0F);
        Vec3 Vector3d1 = rider.getViewVector(1.0F);
        Vec3 Vector3d2 = Vector3d.add(Vector3d1.x * dist, Vector3d1.y * dist, Vector3d1.z * dist);
        double d1 = dist;
        Entity pointedEntity = null;
        List<Entity> list = rider.level().getEntities(rider, rider.getBoundingBox().expandTowards(Vector3d1.x * dist, Vector3d1.y * dist, Vector3d1.z * dist).inflate(1.0D, 1.0D, 1.0D), new Predicate<Entity>() {
            @Override
            public boolean apply(@Nullable Entity entity) {
                if (onSameTeam(dragon, entity)) {
                    return false;
                }
                return entity != null && entity.isPickable() && entity instanceof LivingEntity && !entity.is(dragon) && !entity.isAlliedTo(dragon) && (!(entity instanceof IDeadMob) || !((IDeadMob) entity).isMobDead());
            }
        });
        double d2 = d1;
        for (int j = 0; j < list.size(); ++j) {
            Entity entity1 = list.get(j);
            AABB axisalignedbb = entity1.getBoundingBox().inflate((double) entity1.getPickRadius() + 2F);
            Vec3 raytraceresult = axisalignedbb.clip(Vector3d, Vector3d2).orElse(net.minecraft.world.phys.Vec3.ZERO);

            if (axisalignedbb.contains(Vector3d)) {
                if (d2 >= 0.0D) {
                    pointedEntity = entity1;
                    d2 = 0.0D;
                }
            } else if (raytraceresult != null) {
                double d3 = Vector3d.distanceTo(raytraceresult);

                if (d3 < d2 || d2 == 0.0D) {
                    if (entity1.getRootVehicle() == rider.getRootVehicle() && !rider.canRiderInteract()) {
                        if (d2 == 0.0D) {
                            pointedEntity = entity1;
                        }
                    } else {
                        pointedEntity = entity1;
                        d2 = d3;
                    }
                }
            }
        }
        return (LivingEntity) pointedEntity;
    }

    public static BlockPos getBlockInViewHippogryph(EntityHippogryph hippo, float yawAddition) {
        float radius = 0.75F * (0.7F * 8) * -3 - hippo.getRandom().nextInt(48);
        float neg = hippo.getRandom().nextBoolean() ? 1 : -1;
        float angle = (0.01745329251F * (hippo.yBodyRot + yawAddition)) + 3.15F + (hippo.getRandom().nextFloat() * neg);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        if (hippo.hasHomePosition && hippo.homePos != null) {
            BlockPos dragonPos = hippo.blockPosition();
            BlockPos ground = hippo.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dragonPos);
            int maxHeight = getMaximumFlightHeightForPos(hippo.level(), dragonPos);
            for (int i = 0; i < 10; i++) {
                // 骏鹰巡航高度：地面+8到地面+32
                int targetY = Math.min(maxHeight, ground.getY() + getHippogryphFlightHeight());
                BlockPos pos = BlockPos.containing(
                    hippo.homePos.getX() + hippo.getRandom().nextInt(IafConfig.dragonWanderFromHomeDistance) - IafConfig.dragonWanderFromHomeDistance,
                    targetY,
                    hippo.homePos.getZ() + hippo.getRandom().nextInt(IafConfig.dragonWanderFromHomeDistance * 2) - IafConfig.dragonWanderFromHomeDistance);
                if (hippo.distanceToSqr(Vec3.atCenterOf(pos)) > 6 && !hippo.isTargetBlocked(Vec3.atCenterOf(pos))) {
                    return pos;
                }
            }
        }
        BlockPos radialPos = BlockPos.containing(hippo.getX() + extraX, 0, hippo.getZ() + extraZ);
        BlockPos ground = hippo.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, radialPos);
        int maxHeight = getMaximumFlightHeightForPos(hippo.level(), radialPos);
        // 骏鹰巡航高度：地面+8到地面+32，不超过最大飞行高度
        int targetY = Math.min(maxHeight, ground.getY() + getHippogryphFlightHeight());
        BlockPos newPos = new BlockPos(radialPos.getX(), targetY, radialPos.getZ());
        BlockPos pos = hippo.doesWantToLand() ? ground : newPos;
        if (!hippo.isTargetBlocked(Vec3.atCenterOf(newPos)) && hippo.distanceToSqr(Vec3.atCenterOf(newPos)) > 6) {
            return newPos;
        }
        return null;
    }

    public static BlockPos getBlockInViewStymphalian(EntityStymphalianBird bird) {
        float radius = 0.75F * (0.7F * 6) * -3 - bird.getRandom().nextInt(24);
        float neg = bird.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = bird.flock != null && !bird.flock.isLeader(bird) ? getStymphalianFlockDirection(bird) : bird.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (bird.getRandom().nextFloat() * neg);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = getStymphalianFearPos(bird, BlockPos.containing(bird.getX() + extraX, 0, bird.getZ() + extraZ));
        BlockPos ground = bird.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, radialPos);
        int distFromGround = (int) bird.getY() - ground.getY();
        int flightHeight = Math.min(IafConfig.stymphalianBirdFlightHeight, ground.getY() + bird.getRandom().nextInt(16));
        BlockPos newPos = radialPos.above(distFromGround > 16 ? flightHeight : (int) bird.getY() + bird.getRandom().nextInt(16) + 1);
        // FIXME :: Unused
//        BlockPos pos = bird.doesWantToLand() ? ground : newPos;
        if (bird.distanceToSqr(Vec3.atCenterOf(newPos)) > 6 && !bird.isTargetBlocked(Vec3.atCenterOf(newPos))) {
            return newPos;
        }
        return null;
    }

    private static BlockPos getStymphalianFearPos(EntityStymphalianBird bird, BlockPos fallback) {
        if (bird.getVictor() != null && bird.getVictor() instanceof PathfinderMob) {
            Vec3 Vector3d = DefaultRandomPos.getPosAway((PathfinderMob) bird.getVictor(), 16, IafConfig.stymphalianBirdFlightHeight, new Vec3(bird.getVictor().getX(), bird.getVictor().getY(), bird.getVictor().getZ()));
            if (Vector3d != null) {
                BlockPos pos = BlockPos.containing(Vector3d);
                return new BlockPos(pos.getX(), 0, pos.getZ());
            }
        }
        return fallback;
    }

    private static float getStymphalianFlockDirection(EntityStymphalianBird bird) {
        EntityStymphalianBird leader = bird.flock.getLeader();
        if (bird.distanceToSqr(leader) > 2) {
            double d0 = leader.getX() - bird.getX();
            double d2 = leader.getZ() - bird.getZ();
            float f = (float) (Mth.atan2(d2, d0) * (180D / Math.PI)) - 90.0F;
            float degrees = Mth.wrapDegrees(f - bird.getYRot());

            return bird.getYRot() + degrees;
        } else {
            return leader.yBodyRot;
        }
    }

    public static BlockPos getBlockInTargetsViewCockatrice(EntityCockatrice cockatrice, LivingEntity target) {
        float radius = 10 + cockatrice.getRandom().nextInt(10);
        float angle = (0.01745329251F * target.yHeadRot);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = BlockPos.containing(target.getX() + extraX, 0, target.getZ() + extraZ);
        BlockPos ground = target.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, radialPos);
        if (cockatrice.distanceToSqr(Vec3.atCenterOf(ground)) > 30 && !cockatrice.isTargetBlocked(Vec3.atCenterOf(ground))) {
            return ground;
        }
        return target.blockPosition();
    }


    public static BlockPos getBlockInTargetsViewGhost(EntityGhost ghost, LivingEntity target) {
        float radius = 4 + ghost.getRandom().nextInt(5);
        float angle = (0.01745329251F * (target.yHeadRot + 90F + ghost.getRandom().nextInt(180)));
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = BlockPos.containing(target.getX() + extraX, target.getY(), target.getZ() + extraZ);
        BlockPos ground = radialPos;
        if (ghost.distanceToSqr(Vec3.atCenterOf(ground)) > 30) {
            return ground;
        }
        return ghost.blockPosition();
    }

    public static BlockPos getBlockInTargetsViewGorgon(EntityGorgon cockatrice, LivingEntity target) {
        float radius = 6;
        float angle = (0.01745329251F * target.yHeadRot);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = BlockPos.containing(target.getX() + extraX, target.getY(), target.getZ() + extraZ);
        if (cockatrice.distanceToSqr(Vec3.atCenterOf(radialPos)) < 300 && !cockatrice.isTargetBlocked(Vec3.atCenterOf(radialPos).add(0, 0.75, 0))) {
            return radialPos;
        }
        return target.blockPosition();
    }


    public static BlockPos getBlockInTargetsViewSeaSerpent(EntitySeaSerpent serpent, LivingEntity target) {
        float radius = 10 * serpent.getSeaSerpentScale() + serpent.getRandom().nextInt(10);
        float angle = (0.01745329251F * target.yHeadRot);
        double extraX = radius * Mth.sin((float) (Math.PI + angle));
        double extraZ = radius * Mth.cos(angle);
        BlockPos radialPos = BlockPos.containing(target.getX() + extraX, 0, target.getZ() + extraZ);
        BlockPos ground = target.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, radialPos);
        if (serpent.distanceToSqr(Vec3.atCenterOf(ground)) > 30) {
            return ground;
        }
        return target.blockPosition();
    }

    public static boolean canTameDragonAttack(TamableAnimal dragon, Entity entity) {
        if (isVillager(entity)) {
            return false;
        }
        if (entity instanceof AbstractVillager || entity instanceof AbstractGolem || entity instanceof Player) {
            return false;
        }
        if (entity instanceof TamableAnimal) {
            return !((TamableAnimal) entity).isTame();
        }
        return true;
    }

    public static boolean isVillager(Entity entity) {
        var tags =  ForgeRegistries.ENTITY_TYPES.tags();
        if (tags == null)
            return false;
        return entity.getType().is(tags.createTagKey(IafTagRegistry.VILLAGERS));
    }

    public static boolean isAnimaniaMob(Entity entity) {
        return false;
    }

    public static boolean isDragonTargetable(Entity entity, ResourceLocation tag) {
        return entity.getType().is(ForgeRegistries.ENTITY_TYPES.tags().createTagKey(tag));
    }

    public static String getDimensionName(Level world) {
        return world.dimension().location().toString();
    }

    public static boolean isInHomeDimension(EntityDragonBase dragonBase) {
        return (dragonBase.getHomeDimensionName() == null || getDimensionName(dragonBase.level()).equals(dragonBase.getHomeDimensionName()));
    }

    public static boolean canDragonBreak(final BlockState state, final Entity entity) {
        if (!ForgeEventFactory.getMobGriefingEvent(entity.level(), entity)) {
            return false;
        }

        Block block = state.getBlock();

        return block.getExplosionResistance() < 1200 && !state.is(IafBlockTags.DRAGON_BLOCK_BREAK_BLACKLIST);
    }

    public static boolean hasSameOwner(TamableAnimal cockatrice, Entity entity) {
        if (entity instanceof TamableAnimal tameable) {
            return tameable.getOwnerUUID() != null && cockatrice.getOwnerUUID() != null && tameable.getOwnerUUID().equals(cockatrice.getOwnerUUID());
        }
        return false;
    }

    public static boolean isAlive(final LivingEntity entity) {
        if (entity instanceof EntityDragonBase dragon && dragon.isMobDead()) {
            return false;
        }

        return (!(entity instanceof IDeadMob deadMob) || !deadMob.isMobDead()) && !EntityGorgon.isStoneMob(entity);
    }


    public static boolean canGrief(EntityDragonBase dragon) {
        if (dragon.isTame() && !IafConfig.tamedDragonGriefing) {
            return false;
        }

        return IafConfig.dragonGriefing < 2;

    }

    public static boolean canHostilesTarget(Entity entity) {
        if (entity instanceof Player && (entity.level().getDifficulty() == Difficulty.PEACEFUL || ((Player) entity).isCreative())) {
            return false;
        }
        if (entity instanceof EntityDragonBase && ((EntityDragonBase) entity).isMobDead()) {
            return false;
        } else {
            return entity instanceof LivingEntity && isAlive((LivingEntity) entity);
        }
    }

    public static boolean onSameTeam(Entity entity1, Entity entity2) {
        Entity owner1 = null;
        Entity owner2 = null;
        boolean def = entity1.isAlliedTo(entity2);
        if (entity1 instanceof TamableAnimal) {
            owner1 = ((TamableAnimal) entity1).getOwner();
        }
        if (entity2 instanceof TamableAnimal) {
            owner2 = ((TamableAnimal) entity2).getOwner();
        }
        if (entity1 instanceof EntityMutlipartPart) {
            Entity multipart = ((EntityMutlipartPart) entity1).getParent();
            if (multipart != null && multipart instanceof TamableAnimal) {
                owner1 = ((TamableAnimal) multipart).getOwner();
            }
        }
        if (entity2 instanceof EntityMutlipartPart) {
            Entity multipart = ((EntityMutlipartPart) entity2).getParent();
            if (multipart != null && multipart instanceof TamableAnimal) {
                owner2 = ((TamableAnimal) multipart).getOwner();
            }
        }
        if (owner1 != null && owner2 != null) {
            return owner1.is(owner2);
        }
        return def;
    }

    public static boolean isDreadBlock(BlockState state) {
        Block block = state.getBlock();
        return block == IafBlockRegistry.DREAD_STONE.get() || block == IafBlockRegistry.DREAD_STONE_BRICKS.get() || block == IafBlockRegistry.DREAD_STONE_BRICKS_CHISELED.get() ||
                block == IafBlockRegistry.DREAD_STONE_BRICKS_CRACKED.get() || block == IafBlockRegistry.DREAD_STONE_BRICKS_MOSSY.get() || block == IafBlockRegistry.DREAD_STONE_TILE.get() ||
                block == IafBlockRegistry.DREAD_STONE_FACE.get() || block == IafBlockRegistry.DREAD_TORCH.get() || block == IafBlockRegistry.DREAD_STONE_BRICKS_STAIRS.get() ||
                block == IafBlockRegistry.DREAD_STONE_BRICKS_SLAB.get() || block == IafBlockRegistry.DREADWOOD_LOG.get() ||
                block == IafBlockRegistry.DREADWOOD_PLANKS.get() || block == IafBlockRegistry.DREADWOOD_PLANKS_LOCK.get() || block == IafBlockRegistry.DREAD_PORTAL.get() ||
                block == IafBlockRegistry.DREAD_SPAWNER.get();
    }
}
