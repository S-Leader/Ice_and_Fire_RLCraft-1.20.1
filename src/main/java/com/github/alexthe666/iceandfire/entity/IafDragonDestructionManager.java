package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.github.alexthe666.iceandfire.api.VoltageConfig;
import com.github.alexthe666.iceandfire.api.event.DragonFireDamageWorldEvent;
import com.github.alexthe666.iceandfire.block.*;
import com.github.alexthe666.iceandfire.effect.MobEffectMelt;
import com.github.alexthe666.iceandfire.effect.MobEffectVoltage;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import com.github.alexthe666.iceandfire.entity.tile.TileEntityDragonforgeInput;
import com.github.alexthe666.iceandfire.entity.util.BlockLaunchExplosion;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import com.github.alexthe666.iceandfire.misc.IafDamageRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;

public class IafDragonDestructionManager {
    private static final String BREATH_CHAIN_COOLDOWN = "IceAndFireDragonBreathChainCooldown";

    public static void destroyAreaBreath(final Level level, final BlockPos center, final EntityDragonBase dragon) {
        if (MinecraftForge.EVENT_BUS
                .post(new DragonFireDamageWorldEvent(dragon, center.getX(), center.getY(), center.getZ()))) {
            return;
        }

        int statusDuration;
        float damageScale;

        DragonForgeType forgeType;
        if (dragon.dragonType == DragonType.FIRE) {
            statusDuration = 5 + dragon.getDragonStage() * 5;
            damageScale = (float) IafConfig.dragonAttackDamageFire;
            forgeType = DragonForgeType.FIRE;
        } else if (dragon.dragonType == DragonType.ICE) {
            statusDuration = 50 * dragon.getDragonStage();
            damageScale = (float) IafConfig.dragonAttackDamageIce;
            forgeType = DragonForgeType.ICE;
        } else if (dragon.dragonType == DragonType.LIGHTNING) {
            statusDuration = 3;
            damageScale = (float) IafConfig.dragonAttackDamageLightning;
            forgeType = DragonForgeType.LIGHTNING;
        } else {
            return;
        }

        double damageRadius = 3.5;
        boolean canBreakBlocks = ForgeEventFactory.getMobGriefingEvent(level, dragon);

        if (dragon.getDragonStage() <= 3) {
            BlockPos.betweenClosedStream(center.offset(-1, -1, -1), center.offset(1, 1, 1)).forEach(position -> {
                if (level.getBlockEntity(position) instanceof TileEntityDragonforgeInput forge) {
                    forge.onHitWithFlame(forgeType);
                    return;
                }

                if (canBreakBlocks && DragonUtils.canGrief(dragon) && dragon.getRandom().nextBoolean()) {
                    attackBlock(level, dragon, position);
                }
            });
        } else {
            final int radius = dragon.getDragonStage() == 4 ? 2 : 3;
            final int x = radius + level.random.nextInt(1);
            final int y = radius + level.random.nextInt(1);
            final int z = radius + level.random.nextInt(1);
            final float f = (float) (x + y + z) * 0.333F + 0.5F;
            final float ff = f * f;

            damageRadius = 2.5F + f * 1.2F;

            BlockPos.betweenClosedStream(center.offset(-x, -y, -z), center.offset(x, y, z)).forEach(position -> {
                if (level.getBlockEntity(position) instanceof TileEntityDragonforgeInput forge) {
                    forge.onHitWithFlame(forgeType);
                    return;
                }

                if (canBreakBlocks && center.distSqr(position) <= ff) {
                    if (DragonUtils.canGrief(dragon)
                            && level.random.nextFloat() > (float) center.distSqr(position) / ff) {
                        attackBlock(level, dragon, position);
                    }
                }
            });
        }

        DamageSource damageSource = getDamageSource(dragon);
        float stageDamage = dragon.getDragonStage() * damageScale;

        level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(
                                (double) center.getX() - damageRadius,
                                (double) center.getY() - damageRadius,
                                (double) center.getZ() - damageRadius,
                                (double) center.getX() + damageRadius,
                                (double) center.getY() + damageRadius,
                                (double) center.getZ() + damageRadius))
                .forEach(target -> {
                    if (!DragonUtils.onSameTeam(dragon, target) && !dragon.is(target)
                            && dragon.hasLineOfSight(target)) {
                        if (dragon instanceof EntityShivaxiDragon shivaxi) {
                            damageShivaxiBreathTarget(target, shivaxi);
                        } else {
                            if (target.hurt(damageSource, stageDamage)
                                    && applyDragonEffect(target, dragon.dragonType, statusDuration, dragon, stageDamage)
                                    && dragon.dragonType == DragonType.LIGHTNING) {
                                tryCreateBreathChain(level, target, dragon, stageDamage);
                            }
                        }
                        if (target instanceof Player player) {
                            DragonUtils.fillBottleWithDragonBreath(player, dragon.dragonType);
                        }
                    }
                });
    }

    /**
     * Recreates the small elemental explosion produced by the throwable dragon-breath bottles
     * from the 1.12.2 RLCraft build.  It deliberately uses the modern block transformations and
     * block-protection checks so claims, dragon-proof blocks and the current blacklist continue
     * to work.
     */
    public static void destroyAreaBreathItem(final Level level, final BlockPos center, final DragonType type,
                                             final Entity projectile, @Nullable final Entity owner) {
        if (type != DragonType.FIRE && type != DragonType.ICE && type != DragonType.LIGHTNING) {
            return;
        }

        final Entity source = owner == null ? projectile : owner;
        final int radius = 2;
        final double radiusSquared = radius * radius;
        final DragonForgeType forgeType = type == DragonType.FIRE ? DragonForgeType.FIRE
                : type == DragonType.ICE ? DragonForgeType.ICE : DragonForgeType.LIGHTNING;

        if (ForgeEventFactory.getMobGriefingEvent(level, source)) {
            BlockPos.betweenClosedStream(center.offset(-radius, -radius, -radius),
                            center.offset(radius, radius, radius))
                    .filter(position -> center.distSqr(position) <= radiusSquared)
                    .forEach(position -> {
                        if (level.getBlockEntity(position) instanceof TileEntityDragonforgeInput forge) {
                            forge.onHitWithFlame(forgeType);
                        } else {
                            attackBlock(level, type, source, position, level.getBlockState(position));
                        }
                    });
        }

        final double damageRadius = radius * 2.0D;
        final Vec3 explosionCenter = Vec3.atCenterOf(center);
        final DamageSource damageSource = getDamageSource(type, projectile, owner);

        level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(center).inflate(damageRadius),
                        target -> !target.is(source))
                .forEach(target -> {
                    double normalizedDistance = Math.sqrt(target.distanceToSqr(explosionCenter)) / damageRadius;
                    if (normalizedDistance > 1.0D) {
                        return;
                    }

                    double x = target.getX() - explosionCenter.x;
                    double y = target.getEyeY() - explosionCenter.y;
                    double z = target.getZ() - explosionCenter.z;
                    double length = Math.sqrt(x * x + y * y + z * z);
                    if (length == 0.0D) {
                        return;
                    }

                    double exposure = Explosion.getSeenPercent(explosionCenter, target);
                    double impact = (1.0D - normalizedDistance) * exposure;
                    float damage = (float) ((int) (((impact * impact + impact) / 2.0D)
                            * 7.0D * damageRadius + 1.0D)) / 3.0F;
                    if (damage > 0.0F && target.hurt(damageSource, damage)) {
                        int duration = type == DragonType.FIRE ? 5 : type == DragonType.ICE ? 200 : 3;
                        if (applyDragonEffect(target, type, duration, source, damage)
                                && type == DragonType.LIGHTNING) {
                            tryCreateBreathChain(level, target, source, damage);
                        }
                    }

                    x /= length;
                    y /= length;
                    z /= length;
                    target.setDeltaMovement(target.getDeltaMovement().add(x * impact, y * impact, z * impact));
                });
    }

    public static void destroyAreaCharge(final Level level, final BlockPos center, final EntityDragonBase dragon) {
        if (dragon == null) {
            return;
        }

        if (MinecraftForge.EVENT_BUS
                .post(new DragonFireDamageWorldEvent(dragon, center.getX(), center.getY(), center.getZ()))) {
            return;
        }

        int x = 2;
        int y = 2;
        int z = 2;

        boolean canBreakBlocks = DragonUtils.canGrief(dragon) && ForgeEventFactory.getMobGriefingEvent(level, dragon);

        if (canBreakBlocks) {
            if (dragon.getDragonStage() <= 3) {
                BlockPos.betweenClosedStream(center.offset(-x, -y, -z), center.offset(x, y, z)).forEach(position -> {
                    BlockState state = level.getBlockState(position);

                    if (state.getBlock() instanceof IDragonProof) {
                        return;
                    }

                    if (dragon.getRandom().nextFloat() * 3 > center.distSqr(position)
                            && DragonUtils.canDragonBreak(state, dragon)) {
                        level.destroyBlock(position, false);
                    }

                    if (dragon.getRandom().nextBoolean()) {
                        attackBlock(level, dragon, position, state);
                    }
                });
            } else {
                final int radius = dragon.getDragonStage() == 4 ? 2 : 3;
                x = radius + level.random.nextInt(2);
                y = radius + level.random.nextInt(2);
                z = radius + level.random.nextInt(2);
                final float f = (float) (x + y + z) * 0.333F + 0.5F;
                final float ff = f * f;

                destroyBlocks(level, center, x, y, z, ff, dragon);

                x++;
                y++;
                z++;

                BlockPos.betweenClosedStream(center.offset(-x, -y, -z), center.offset(x, y, z)).forEach(position -> {
                    if (center.distSqr(position) <= ff) {
                        attackBlock(level, dragon, position);
                    }
                });
            }
        }

        final int statusDuration;

        if (dragon.dragonType == DragonType.FIRE) {
            statusDuration = 15;
        } else if (dragon.dragonType == DragonType.ICE) {
            statusDuration = 400;
        } else if (dragon.dragonType == DragonType.LIGHTNING) {
            statusDuration = 9;
        } else {
            return;
        }

        final float stageDamage = Math.max(1, dragon.getDragonStage() - 1) * 2F;
        DamageSource damageSource = getDamageSource(dragon);

        level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(
                                (double) center.getX() - x,
                                (double) center.getY() - y,
                                (double) center.getZ() - z,
                                (double) center.getX() + x,
                                (double) center.getY() + y,
                                (double) center.getZ() + z))
                .forEach(target -> {
                    if (!dragon.isAlliedTo(target) && !dragon.is(target) && dragon.hasLineOfSight(target)) {
                        if (target.hurt(damageSource, stageDamage)
                                && applyDragonEffect(target, dragon.dragonType, statusDuration, dragon, stageDamage)
                                && dragon.dragonType == DragonType.LIGHTNING) {
                            tryCreateBreathChain(level, target, dragon, stageDamage);
                        }
                    }
                });

        if (IafConfig.explosiveDragonBreath) {
            causeExplosion(level, center, dragon, damageSource, dragon.getDragonStage());
        }
    }

    private static DamageSource getDamageSource(final EntityDragonBase dragon) {
        return getDamageSource(dragon, dragon.dragonType);
    }

    private static DamageSource getDamageSource(final EntityDragonBase dragon, final DragonType type) {
        Player player = dragon.getRidingPlayer();

        if (type == DragonType.FIRE) {
            return player != null ? IafDamageRegistry.causeIndirectDragonFireDamage(dragon, player)
                    : IafDamageRegistry.causeDragonFireDamage(dragon);
        } else if (type == DragonType.ICE) {
            return player != null ? IafDamageRegistry.causeIndirectDragonIceDamage(dragon, player)
                    : IafDamageRegistry.causeDragonIceDamage(dragon);
        } else if (type == DragonType.LIGHTNING) {
            return player != null ? IafDamageRegistry.causeIndirectDragonLightningDamage(dragon, player)
                    : IafDamageRegistry.causeDragonLightningDamage(dragon);
        } else {
            return dragon.level().damageSources().mobAttack(dragon);
        }
    }

    private static DamageSource getDamageSource(final DragonType type, final Entity projectile,
                                                @Nullable final Entity owner) {
        if (type == DragonType.FIRE) {
            return owner == null ? IafDamageRegistry.causeDragonFireDamage(projectile)
                    : IafDamageRegistry.causeIndirectDragonFireDamage(projectile, owner);
        } else if (type == DragonType.ICE) {
            return owner == null ? IafDamageRegistry.causeDragonIceDamage(projectile)
                    : IafDamageRegistry.causeIndirectDragonIceDamage(projectile, owner);
        }
        return owner == null ? IafDamageRegistry.causeDragonLightningDamage(projectile)
                : IafDamageRegistry.causeIndirectDragonLightningDamage(projectile, owner);
    }

    /**
     * Shivaxi breath replacement for the temporary three-element port.  The breath keeps the
     * lightning dragon's normal direct breath damage, but its status component is the legacy
     * RLCraft ShivaxiBlazed capability for 200 ticks.
     */
    private static void damageShivaxiBreathTarget(final LivingEntity target, final EntityShivaxiDragon dragon) {
        float damage = dragon.getDragonStage() * (float) IafConfig.dragonAttackDamageLightning;
        target.hurt(getDamageSource(dragon, DragonType.LIGHTNING), damage);
        EntityDataProvider.getCapability(target)
                .ifPresent(data -> data.shivaxiBlazeData.setShivaxiBlazed(200, 0));
    }

    private static void attackBlock(final Level level, final EntityDragonBase dragon, final BlockPos position,
                                    final BlockState state) {
        attackBlock(level, dragon.dragonType, dragon, position, state);
    }

    private static void attackBlock(final Level level, final DragonType type, final Entity source,
                                    final BlockPos position, final BlockState state) {
        if (state.getBlock() instanceof IDragonProof || !DragonUtils.canDragonBreak(state, source)) {
            return;
        }

        BlockState transformed;

        if (type == DragonType.FIRE) {
            transformed = transformBlockFire(state);
        } else if (type == DragonType.ICE) {
            transformed = transformBlockIce(state);
        } else if (type == DragonType.LIGHTNING) {
            transformed = transformBlockLightning(state);
        } else {
            return;
        }

        if (!transformed.is(state.getBlock())) {
            level.setBlockAndUpdate(position, transformed);
        }

        Block elementalBlock;
        boolean doPlaceBlock;

        if (type == DragonType.FIRE) {
            elementalBlock = Blocks.FIRE;
            doPlaceBlock = level.random.nextBoolean();
        } else if (type == DragonType.ICE) {
            elementalBlock = IafBlockRegistry.DRAGON_ICE_SPIKES.get();
            doPlaceBlock = level.random.nextInt(9) == 0;
        } else {
            return;
        }

        BlockState stateAbove = level.getBlockState(position.above());

        if (doPlaceBlock && transformed.isSolid() && stateAbove.getFluidState().isEmpty() && !stateAbove.canOcclude()
                && state.canOcclude() && DragonUtils.canDragonBreak(stateAbove, source)) {
            level.setBlockAndUpdate(position.above(), elementalBlock.defaultBlockState());
        }
    }

    private static void attackBlock(final Level level, final EntityDragonBase dragon, final BlockPos position) {
        attackBlock(level, dragon, position, level.getBlockState(position));
    }

    private static boolean applyDragonEffect(final LivingEntity target, final DragonType type, int statusDuration,
                                             final Entity source, float baseDamage) {
        LivingEntity livingSource = source instanceof LivingEntity living ? living : null;
        if (type == DragonType.FIRE) {
            if (com.github.alexthe666.iceandfire.item.blooded.ItemBloodedArmor.hasFullArmorSet(target,
                    com.github.alexthe666.iceandfire.item.blooded.BloodedDragonType.DragonElement.FIRE))
                return false;
            MobEffectMelt.applyMelt(target, livingSource, baseDamage, statusDuration * 20);
        } else if (type == DragonType.ICE) {
            if (com.github.alexthe666.iceandfire.item.blooded.ItemBloodedArmor.hasFullArmorSet(target,
                    com.github.alexthe666.iceandfire.item.blooded.BloodedDragonType.DragonElement.ICE))
                return false;
            EntityDataProvider.getCapability(target)
                    .ifPresent(data -> data.frozenData.setFrozen(target, statusDuration));
        } else if (type == DragonType.LIGHTNING) {
            if (com.github.alexthe666.iceandfire.item.blooded.ItemBloodedArmor.hasFullArmorSet(target,
                    com.github.alexthe666.iceandfire.item.blooded.BloodedDragonType.DragonElement.LIGHTNING))
                return false;
            MobEffectVoltage.applyVoltage(target, livingSource, MobEffectVoltage.DURATION_TICKS, baseDamage);
            double x = source.getX() - target.getX();
            double y = source.getZ() - target.getZ();
            target.knockback((double) statusDuration / 10, x, y);
        } else {
            return false;
        }
        return true;
    }

    private static void tryCreateBreathChain(final Level level, final LivingEntity target,
                                             final Entity source, float baseDamage) {
        if (level.isClientSide) {
            return;
        }

        long gameTime = level.getGameTime();
        long nextAllowedTime = source.getPersistentData().getLong(BREATH_CHAIN_COOLDOWN);
        if (nextAllowedTime > gameTime) {
            return;
        }

        source.getPersistentData().putLong(BREATH_CHAIN_COOLDOWN,
                gameTime + VoltageConfig.CHAIN_COOLDOWN_TICKS);
        ChainLightningUtils.createChainLightning(level, target, source, baseDamage, false, false);
    }

    private static void causeExplosion(Level world, BlockPos center, EntityDragonBase destroyer, DamageSource source,
                                       int stage) {
        Explosion.BlockInteraction mode = ForgeEventFactory.getMobGriefingEvent(world, destroyer)
                ? Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.KEEP;
        BlockLaunchExplosion explosion = new BlockLaunchExplosion(world, destroyer, source, center.getX(),
                center.getY(), center.getZ(), Math.min(2, stage - 2), mode);
        explosion.explode();
        explosion.finalizeExplosion(true);
    }

    private static void destroyBlocks(Level world, BlockPos center, int x, int y, int z, double radius2,
                                      Entity destroyer) {
        BlockPos.betweenClosedStream(center.offset(-x, -y, -z), center.offset(x, y, z)).forEach(pos -> {
            if (center.distSqr(pos) <= radius2) {
                BlockState state = world.getBlockState(pos);

                if (state.getBlock() instanceof IDragonProof) {
                    return;
                }

                if (world.random.nextFloat() * 3 > (float) center.distSqr(pos) / radius2
                        && DragonUtils.canDragonBreak(state, destroyer)) {
                    world.destroyBlock(pos, false);
                }
            }
        });
    }

    public static BlockState transformBlockFire(BlockState in) {
        if (in.getBlock() instanceof SpreadingSnowyDirtBlock) {
            return IafBlockRegistry.CHARRED_GRASS.get().defaultBlockState().setValue(BlockReturningState.REVERTS, true);
        } else if (in.is(Blocks.DIRT)) {
            return IafBlockRegistry.CHARRED_DIRT.get().defaultBlockState().setValue(BlockReturningState.REVERTS, true);
        } else if (in.is(BlockTags.SAND) && in.getBlock() == Blocks.GRAVEL) {
            return IafBlockRegistry.CHARRED_GRAVEL.get().defaultBlockState()
                    .setValue(BlockFallingReturningState.REVERTS, true);
        } else if (in.is(BlockTags.BASE_STONE_OVERWORLD)
                && (in.getBlock() == Blocks.COBBLESTONE || in.getBlock().getDescriptionId().contains("cobblestone"))) {
            return IafBlockRegistry.CHARRED_COBBLESTONE.get().defaultBlockState().setValue(BlockReturningState.REVERTS,
                    true);
        } else if (in.is(BlockTags.BASE_STONE_OVERWORLD)
                && in.getBlock() != IafBlockRegistry.CHARRED_COBBLESTONE.get()) {
            return IafBlockRegistry.CHARRED_STONE.get().defaultBlockState().setValue(BlockReturningState.REVERTS, true);
        } else if (in.getBlock() == Blocks.DIRT_PATH) {
            return IafBlockRegistry.CHARRED_DIRT_PATH.get().defaultBlockState().setValue(BlockCharedPath.REVERTS, true);
        } else if (in.is(BlockTags.LOGS) || in.is(BlockTags.PLANKS)) {
            return IafBlockRegistry.ASH.get().defaultBlockState();
        } else if (in.is(BlockTags.LEAVES) || in.is(BlockTags.FLOWERS) || in.is(BlockTags.CROPS)
                || in.getBlock() == Blocks.SNOW) {
            return Blocks.AIR.defaultBlockState();
        }
        return in;
    }

    public static BlockState transformBlockIce(BlockState in) {
        if (in.getBlock() instanceof SpreadingSnowyDirtBlock) {
            return IafBlockRegistry.FROZEN_GRASS.get().defaultBlockState().setValue(BlockReturningState.REVERTS, true);
        } else if (in.is(BlockTags.DIRT) && in.getBlock() == Blocks.DIRT || in.is(BlockTags.SNOW)) {
            return IafBlockRegistry.FROZEN_DIRT.get().defaultBlockState().setValue(BlockReturningState.REVERTS, true);
        } else if (in.is(BlockTags.SAND) && in.getBlock() == Blocks.GRAVEL) {
            return IafBlockRegistry.FROZEN_GRAVEL.get().defaultBlockState().setValue(BlockFallingReturningState.REVERTS,
                    true);
        } else if (in.is(BlockTags.SAND) && in.getBlock() != Blocks.GRAVEL) {
            return in;
        } else if (in.is(BlockTags.BASE_STONE_OVERWORLD)
                && (in.getBlock() == Blocks.COBBLESTONE || in.getBlock().getDescriptionId().contains("cobblestone"))) {
            return IafBlockRegistry.FROZEN_COBBLESTONE.get().defaultBlockState().setValue(BlockReturningState.REVERTS,
                    true);
        } else if (in.is(BlockTags.BASE_STONE_OVERWORLD)
                && in.getBlock() != IafBlockRegistry.FROZEN_COBBLESTONE.get()) {
            return IafBlockRegistry.FROZEN_STONE.get().defaultBlockState().setValue(BlockReturningState.REVERTS, true);
        } else if (in.getBlock() == Blocks.DIRT_PATH) {
            return IafBlockRegistry.FROZEN_DIRT_PATH.get().defaultBlockState().setValue(BlockCharedPath.REVERTS, true);
        } else if (in.is(BlockTags.LOGS) || in.is(BlockTags.PLANKS)) {
            return IafBlockRegistry.FROZEN_SPLINTERS.get().defaultBlockState();
        } else if (in.is(Blocks.WATER)) {
            return Blocks.ICE.defaultBlockState();
        } else if (in.is(BlockTags.LEAVES) || in.is(BlockTags.FLOWERS) || in.is(BlockTags.CROPS)
                || in.getBlock() == Blocks.SNOW) {
            return Blocks.AIR.defaultBlockState();
        }
        return in;
    }

    public static BlockState transformBlockLightning(BlockState in) {
        if (in.getBlock() instanceof SpreadingSnowyDirtBlock) {
            return IafBlockRegistry.CRACKLED_GRASS.get().defaultBlockState().setValue(BlockReturningState.REVERTS,
                    true);
        } else if (in.is(BlockTags.DIRT) && in.getBlock() == Blocks.DIRT) {
            return IafBlockRegistry.CRACKLED_DIRT.get().defaultBlockState().setValue(BlockReturningState.REVERTS, true);
        } else if (in.is(BlockTags.SAND) && in.getBlock() == Blocks.GRAVEL) {
            return IafBlockRegistry.CRACKLED_GRAVEL.get().defaultBlockState()
                    .setValue(BlockFallingReturningState.REVERTS, true);
        } else if (in.is(BlockTags.BASE_STONE_OVERWORLD)
                && (in.getBlock() == Blocks.COBBLESTONE || in.getBlock().getDescriptionId().contains("cobblestone"))) {
            return IafBlockRegistry.CRACKLED_COBBLESTONE.get().defaultBlockState().setValue(BlockReturningState.REVERTS,
                    true);
        } else if (in.is(BlockTags.BASE_STONE_OVERWORLD)
                && in.getBlock() != IafBlockRegistry.CRACKLED_COBBLESTONE.get()) {
            return IafBlockRegistry.CRACKLED_STONE.get().defaultBlockState().setValue(BlockReturningState.REVERTS,
                    true);
        } else if (in.getBlock() == Blocks.DIRT_PATH) {
            return IafBlockRegistry.CRACKLED_DIRT_PATH.get().defaultBlockState().setValue(BlockCharedPath.REVERTS,
                    true);
        } else if (in.is(BlockTags.LOGS) || in.is(BlockTags.PLANKS)) {
            return IafBlockRegistry.ASH.get().defaultBlockState();
        } else if (in.is(BlockTags.LEAVES) || in.is(BlockTags.FLOWERS) || in.is(BlockTags.CROPS)
                || in.getBlock() == Blocks.SNOW) {
            return Blocks.AIR.defaultBlockState();
        }
        return in;
    }
}
