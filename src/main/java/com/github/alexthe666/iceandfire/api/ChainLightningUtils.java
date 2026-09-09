package com.github.alexthe666.iceandfire.api;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.entity.util.IDeadMob;
import com.github.alexthe666.iceandfire.message.MessageChainLightningFX;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public final class ChainLightningUtils {

    private ChainLightningUtils() {
    }

    public static void createChainLightning(Level level, LivingEntity target, Entity attacker,
            float baseWeaponDamage) {
        createChainLightning(level, target, attacker, baseWeaponDamage, true);
    }

    /**
     * Variant used by legacy projectile ports. The 1.12.2 lightning arrow did not put the
     * bow itself on cooldown, so callers can suppress the held-item cooldown while keeping
     * the same chain damage and visuals.
     */
    public static void createChainLightning(Level level, LivingEntity target, Entity attacker,
            float baseWeaponDamage, boolean applyPlayerCooldown) {
        if (level.isClientSide)
            return;
        if (!target.isAttackable())
            return;

        if (applyPlayerCooldown && attacker instanceof Player player) {
            if (player.getCooldowns().isOnCooldown(player.getMainHandItem().getItem())) {
                return;
            }
            player.getCooldowns().addCooldown(
                    player.getMainHandItem().getItem(),
                    VoltageConfig.CHAIN_COOLDOWN_TICKS);
        }

        float[] damage = VoltageConfig.CHAIN_DAMAGE_PER_HOP;
        int range = VoltageConfig.CHAIN_RANGE;

        int hop = 0;
        attackWithLightning(level, attacker, target, damage[hop]);
        com.github.alexthe666.iceandfire.effect.MobEffectVoltage.applyVoltage(
                target, (LivingEntity) attacker,
                com.github.alexthe666.iceandfire.effect.MobEffectVoltage.DURATION_TICKS,
                baseWeaponDamage);
        target.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.0F);

        List<Vec3> chainPositions = new ArrayList<>();
        chainPositions.add(target.getBoundingBox().getCenter());

        LivingEntity source = target;
        Set<Integer> visited = new HashSet<>();
        visited.add(target.getId());

        for (hop = 1; hop < damage.length; hop++) {
            final LivingEntity currentSource = source;
            AABB searchBox = currentSource.getBoundingBox().inflate(range);

            List<LivingEntity> candidates = level.getEntitiesOfClass(
                    LivingEntity.class, searchBox,
                    e -> !visited.contains(e.getId())
                            && canChainTo(e, attacker)
                            && currentSource.hasLineOfSight(e));

            if (candidates.isEmpty())
                break;

            candidates.sort(Comparator.comparingDouble(
                    e -> e.distanceToSqr(currentSource)));
            LivingEntity next = candidates.get(0);

            attackWithLightning(level, attacker, next, damage[hop]);
            com.github.alexthe666.iceandfire.effect.MobEffectVoltage.applyVoltage(
                    next, (LivingEntity) attacker,
                    com.github.alexthe666.iceandfire.effect.MobEffectVoltage.DURATION_TICKS,
                    baseWeaponDamage);

            visited.add(next.getId());
            chainPositions.add(next.getBoundingBox().getCenter());
            source = next;
        }

        if (level instanceof ServerLevel serverLevel && chainPositions.size() >= 2) {
            sendLightningFxPacket(serverLevel, target, chainPositions);
        }
    }


    private static void attackWithLightning(Level level, Entity attacker,
            LivingEntity target, float dmg) {
        DamageSource src;
        if (VoltageConfig.CHAIN_BYPASSES_ARMOR) {
            src = level.damageSources().magic();
        } else {
            src = level.damageSources().lightningBolt();
        }
        target.hurt(src, dmg);

        if (target instanceof Creeper creeper && !creeper.isPowered()) {
            CompoundTag tag = new CompoundTag();
            creeper.addAdditionalSaveData(tag);
            tag.putBoolean("powered", true);
            creeper.readAdditionalSaveData(tag);
        }
    }


    private static boolean canHurt(LivingEntity target, Entity attacker) {
        if (!target.isAlive())
            return false;
        if (!target.isAttackable())
            return false;
        if (target instanceof Mob mob && mob.isNoAi())
            return false;
        if (target instanceof IDeadMob deadMob && deadMob.isMobDead())
            return false;
        return target instanceof Mob || target instanceof Player;
    }

    private static boolean canChainTo(LivingEntity target, Entity attacker) {
        if (target instanceof Player)
            return false;
        if (!canHurt(target, attacker))
            return false;

        if (target instanceof TamableAnimal tamable) {
            LivingEntity owner = tamable.getOwner();
            if (owner instanceof Player) {
                if (target instanceof Mob mob) {
                    LivingEntity attackTarget = mob.getTarget();
                    LivingEntity revengeTarget = target.getLastHurtByMob();
                    if (!Objects.equals(attacker, attackTarget) &&
                            !Objects.equals(attacker, revengeTarget)) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }


    private static void sendLightningFxPacket(ServerLevel level,
            LivingEntity origin,
            List<Vec3> positions) {
        MessageChainLightningFX msg = new MessageChainLightningFX(positions);
        for (ServerPlayer player : level.getPlayers(p -> p.distanceTo(origin) < 64)) {
            IceAndFire.sendMSGToPlayer(msg, player);
        }
    }
}
