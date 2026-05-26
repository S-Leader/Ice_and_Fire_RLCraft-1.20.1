package com.github.alexthe666.iceandfire.effect;

import com.github.alexthe666.iceandfire.config.FrostShatterConfig;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

public class MobEffectFrostbite extends MobEffect {

    public static final int MAX_AMPLIFIER = 9;

    public static final int DURATION_TICKS = 200;

    public MobEffectFrostbite() {
        super(MobEffectCategory.HARMFUL, 0x8CE4EF);
    }

    public static void applyFrostbite(LivingEntity target, LivingEntity attacker,
            int durationTicks, float baseWeaponDamage) {
        // 配置关闭时回退为原版冰与火冻结效果
        if (!com.github.alexthe666.iceandfire.IafConfig.enableFrostbiteEffect) {
            com.github.alexthe666.iceandfire.entity.props.EntityDataProvider.getCapability(target)
                    .ifPresent(data -> data.frozenData.setFrozen(target, durationTicks));
            return;
        }

        if (target.hasEffect(IafMobEffects.FROSTBURN.get())) {
            return;
        }
        if (MobEffectChaos.isImmune(target, MobEffectChaos.Element.ICE)) {
            return;
        }

        int currentAmplifier = -1;
        MobEffectInstance existing = target.getEffect(IafMobEffects.FROSTBITE.get());
        if (existing != null) {
            currentAmplifier = existing.getAmplifier();
        }

        int newAmplifier = Math.min(currentAmplifier + 1, MAX_AMPLIFIER);

        int shatterThreshold = FrostShatterConfig.getThreshold(target.getType());

        if (newAmplifier + 1 >= shatterThreshold) {
            triggerShatter(target, attacker, baseWeaponDamage, newAmplifier + 1);
        } else {
            target.addEffect(new MobEffectInstance(
                    IafMobEffects.FROSTBITE.get(),
                    durationTicks,
                    newAmplifier,
                    false, true, true));
        }
    }

    private static void triggerShatter(LivingEntity target, LivingEntity attacker,
            float baseWeaponDamage, int frostbiteStacks) {
        target.removeEffect(IafMobEffects.FROSTBITE.get());

        target.addEffect(new MobEffectInstance(
                IafMobEffects.FROSTBURN.get(),
                MobEffectFrostburn.DURATION_TICKS,
                0,
                false, true, true));

        target.playSound(SoundEvents.GLASS_BREAK, 3.0F, 1.0F);
        if (!target.level().isClientSide()) {
            ((ServerLevel) target.level()).sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK,
                            com.github.alexthe666.iceandfire.block.IafBlockRegistry.DRAGON_ICE.get()
                                    .defaultBlockState()),
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    30, target.getBbWidth() / 2, target.getBbHeight() / 2, target.getBbWidth() / 2, 0.1D);
        }

        float maxHp = target.getMaxHealth();

        target.hurt(target.level().damageSources().indirectMagic(attacker, attacker), maxHp * 0.05f);

        float physicalDamage = baseWeaponDamage * (11 - frostbiteStacks);
        target.hurt(target.level().damageSources().mobAttack(attacker), physicalDamage);

        if (MobEffectChaos.tryChaosReaction(target, MobEffectChaos.Element.ICE, attacker, baseWeaponDamage)) {
            return;
        }
    }
}
