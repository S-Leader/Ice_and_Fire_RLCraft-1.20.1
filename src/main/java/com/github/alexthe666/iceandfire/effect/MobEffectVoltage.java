package com.github.alexthe666.iceandfire.effect;

import com.github.alexthe666.iceandfire.config.VoltageDischargeConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MobEffectVoltage extends MobEffect {

    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    private static final double REDUCTION_PER_LEVEL = -0.05;

    public static final int MAX_AMPLIFIER = 9;

    public static final int DURATION_TICKS = 100;

    private static final ConcurrentHashMap<UUID, Vec3> PREV_POSITIONS = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Double> MOVE_ACCUMULATOR = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, Float> WEAPON_DAMAGE_CACHE = new ConcurrentHashMap<>();

    // PersistentData key
    private static final String NBT_WEAPON_DAMAGE = "iaf_voltage_weapon_dmg";

    public MobEffectVoltage() {
        super(MobEffectCategory.HARMFUL, 0xB060FF);
        addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_MODIFIER_UUID.toString(),
                REDUCTION_PER_LEVEL, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return REDUCTION_PER_LEVEL * (amplifier + 1);
    }

    @Override
    public boolean isDurationEffectTick(int remainingDuration, int amplifier) {
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide() || entity.isDeadOrDying())
            return;

        UUID uuid = entity.getUUID();
        Vec3 currentPos = entity.position();

        Vec3 prevPos = PREV_POSITIONS.get(uuid);
        if (prevPos != null) {
            double dist = currentPos.distanceTo(prevPos);
            double accumulated = MOVE_ACCUMULATOR.getOrDefault(uuid, 0.0) + dist;

            if (accumulated >= 1.0) {
                int layersToAdd = (int) accumulated;
                accumulated -= layersToAdd;

                int currentAmp = amplifier;
                int newAmp = Math.min(currentAmp + layersToAdd, MAX_AMPLIFIER);

                if (newAmp > currentAmp) {
                    int dischargeThreshold = VoltageDischargeConfig.getThreshold(entity.getType());
                    if (newAmp + 1 >= dischargeThreshold) {
                        triggerDischarge(entity, newAmp + 1);
                        MOVE_ACCUMULATOR.remove(uuid);
                        PREV_POSITIONS.put(uuid, currentPos);
                        return;
                    }

                    entity.addEffect(new MobEffectInstance(
                            IafMobEffects.VOLTAGE.get(),
                            DURATION_TICKS,
                            newAmp,
                            false, true, true));
                }

                MOVE_ACCUMULATOR.put(uuid, accumulated);
            } else {
                MOVE_ACCUMULATOR.put(uuid, accumulated);
            }
        }

        PREV_POSITIONS.put(uuid, currentPos);
    }

    public static void applyVoltage(LivingEntity target, LivingEntity attacker,
            int durationTicks, float baseWeaponDamage) {

        // 配置关闭时回退为直接闪电伤害+击退
        if (!com.github.alexthe666.iceandfire.IafConfig.enableVoltageEffect) {
            target.hurt(target.level().damageSources().lightningBolt(), baseWeaponDamage * 0.5f);
            if (attacker != null) {
                target.knockback(1F, attacker.getX() - target.getX(), attacker.getZ() - target.getZ());
            }
            return;
        }

        if (MobEffectChaos.tryChaosReaction(target, MobEffectChaos.Element.LIGHTNING, attacker, baseWeaponDamage)) {
            return;
        }

        int currentAmplifier = -1;
        MobEffectInstance existing = target.getEffect(IafMobEffects.VOLTAGE.get());
        if (existing != null) {
            currentAmplifier = existing.getAmplifier();
        }

        int layersToAdd = 1;
        if (target.level() instanceof ServerLevel serverLevel) {
            if (serverLevel.isThundering() && target.level().canSeeSky(target.blockPosition())) {
                layersToAdd = 3;
            } else if (serverLevel.isRaining() && target.level().isRainingAt(target.blockPosition())) {
                layersToAdd = 2;
            }
        }

        int newAmplifier = Math.min(currentAmplifier + layersToAdd, MAX_AMPLIFIER);

        if (baseWeaponDamage > 0) {
            WEAPON_DAMAGE_CACHE.put(target.getUUID(), baseWeaponDamage);
        }

        int dischargeThreshold = VoltageDischargeConfig.getThreshold(target.getType());

        if (newAmplifier + 1 >= dischargeThreshold) {
            triggerDischarge(target, newAmplifier + 1);
        } else {
            target.addEffect(new MobEffectInstance(
                    IafMobEffects.VOLTAGE.get(),
                    durationTicks,
                    newAmplifier,
                    false, true, true));
        }
    }

    private static void triggerDischarge(LivingEntity target, int voltageStacks) {
        target.removeEffect(IafMobEffects.VOLTAGE.get());

        float weaponDamage = WEAPON_DAMAGE_CACHE.getOrDefault(target.getUUID(), 10.0f);
        WEAPON_DAMAGE_CACHE.remove(target.getUUID());

        float damage = weaponDamage * voltageStacks;
        net.minecraft.world.entity.Entity credit = target.getKillCredit();
        net.minecraft.world.damagesource.DamageSource source = credit != null
                ? target.level().damageSources().indirectMagic(credit, credit)
                : target.level().damageSources().lightningBolt();
        target.hurt(source, damage);

        target.playSound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6F, 1.5F);
        sendDischargeEffect(target);

        cleanupTracking(target.getUUID());
    }

    public static void cleanupTracking(UUID uuid) {
        PREV_POSITIONS.remove(uuid);
        MOVE_ACCUMULATOR.remove(uuid);
    }

    private static void sendDischargeEffect(LivingEntity entity) {
        if (entity.level() instanceof ServerLevel serverLevel) {
            com.github.alexthe666.iceandfire.message.MessageVoltageDischargeFX msg = new com.github.alexthe666.iceandfire.message.MessageVoltageDischargeFX(
                    entity.getId());
            for (net.minecraft.server.level.ServerPlayer player : serverLevel
                    .getPlayers(p -> p.distanceTo(entity) < 64)) {
                com.github.alexthe666.iceandfire.IceAndFire.sendMSGToPlayer(msg, player);
            }
        }
    }
}
