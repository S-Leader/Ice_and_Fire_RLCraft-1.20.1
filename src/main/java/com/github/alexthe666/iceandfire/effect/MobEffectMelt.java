package com.github.alexthe666.iceandfire.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public class MobEffectMelt extends MobEffect {

    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d");

    private static final UUID TOUGHNESS_MODIFIER_UUID = UUID.fromString("b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e");

    private static final double ARMOR_REDUCTION_PER_LEVEL = -0.05;

    public static final int MAX_AMPLIFIER = 9;

    public MobEffectMelt() {
        super(MobEffectCategory.HARMFUL, 0x8A1208);
        addAttributeModifier(Attributes.ARMOR, ARMOR_MODIFIER_UUID.toString(),
                ARMOR_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        addAttributeModifier(Attributes.ARMOR_TOUGHNESS, TOUGHNESS_MODIFIER_UUID.toString(),
                ARMOR_REDUCTION_PER_LEVEL,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
    }

    @Override
    public boolean isDurationEffectTick(int remainingDuration, int amplifier) {
        return remainingDuration % 20 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide())
            return;

        if (entity.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true));
        } else {
            float magicDamage = amplifier + 1.0F;
            net.minecraft.world.entity.Entity credit = entity.getKillCredit();
            net.minecraft.world.damagesource.DamageSource source = credit != null
                    ? entity.level().damageSources().indirectMagic(credit, credit)
                    : entity.level().damageSources().magic();
            entity.hurt(source, magicDamage);
        }
    }

    public static void applyMelt(LivingEntity target, LivingEntity attacker,
            float weaponDamage, int durationTicks) {
        // 配置关闭时回退为原版着火
        if (!com.github.alexthe666.iceandfire.IafConfig.enableMeltEffect) {
            target.setSecondsOnFire(durationTicks / 20);
            return;
        }

        if (MobEffectChaos.tryChaosReaction(target, MobEffectChaos.Element.FIRE, attacker, weaponDamage)) {
            return;
        }

        int newAmplifier = 0;
        MobEffectInstance existing = target.getEffect(IafMobEffects.MELT.get());
        if (existing != null) {
            newAmplifier = Math.min(existing.getAmplifier() + 1, MAX_AMPLIFIER);
        }
        target.addEffect(new MobEffectInstance(
                IafMobEffects.MELT.get(),
                durationTicks,
                newAmplifier,
                false, true, true));
    }

    public static void applyMelt(LivingEntity target, int durationTicks) {
        applyMelt(target, null, 0, durationTicks);
    }

    @Override
    public double getAttributeModifierValue(int amplifier, AttributeModifier modifier) {
        return ARMOR_REDUCTION_PER_LEVEL * (amplifier + 1);
    }
}
