package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.github.alexthe666.iceandfire.entity.EntityDeathWorm;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public interface DragonSteelOverrides<T extends TieredItem> {

    /**
     * Kept for compatibility
     *
     * @deprecated use data pack overrides instead
     */
    @Deprecated
    Multimap<Attribute, AttributeModifier> bakeDragonsteel();

    default float getAttackDamage(T item) {
        if (item instanceof SwordItem) {
            return ((SwordItem) item).getDamage();
        }
        if (item instanceof DiggerItem) {
            return ((DiggerItem) item).getAttackDamage();
        }
        return item.getTier().getAttackDamageBonus();
        // return item.getDamage(item.asItem().getDefaultInstance())
    }

    default boolean isDragonsteel(Tier tier) {
        return tier.getTag() == DragonSteelTier.DRAGONSTEEL_TIER_TAG;
    }

    default boolean isDragonsteelFire(Tier tier) {
        return tier == DragonSteelTier.DRAGONSTEEL_TIER_FIRE;
    }

    default boolean isDragonsteelIce(Tier tier) {
        return tier == DragonSteelTier.DRAGONSTEEL_TIER_ICE;
    }

    default boolean isDragonsteelLightning(Tier tier) {
        return tier == DragonSteelTier.DRAGONSTEEL_TIER_LIGHTNING;
    }

    default boolean isDragonsteelGold(Tier tier) {
        return tier == DragonSteelTier.DRAGONSTEEL_TIER_GOLD;
    }

    default void hurtEnemy(T item, ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (item.getTier() == IafItemRegistry.SILVER_TOOL_MATERIAL) {
            if (target.getMobType() == MobType.UNDEAD) {
                target.hurt(attacker.level().damageSources().magic(), getAttackDamage(item) + 3.0F);
            }
        }

        if (item.getTier() == IafItemRegistry.MYRMEX_CHITIN_TOOL_MATERIAL) {
            if (target.getMobType() != MobType.ARTHROPOD) {
                target.hurt(attacker.level().damageSources().generic(), getAttackDamage(item) + 5.0F);
            }
            if (target instanceof EntityDeathWorm) {
                target.hurt(attacker.level().damageSources().generic(), getAttackDamage(item) + 5.0F);
            }
        }
        if (isDragonsteelFire(item.getTier()) && IafConfig.dragonWeaponFireAbility) {
            com.github.alexthe666.iceandfire.effect.MobEffectMelt.applyMelt(
                    target, attacker, getAttackDamage(item), 160);
        }
        if (isDragonsteelIce(item.getTier()) && IafConfig.dragonWeaponIceAbility) {
            float baseDamage = getAttackDamage(item);
            com.github.alexthe666.iceandfire.effect.MobEffectFrostbite.applyFrostbite(target, attacker, 200,
                    baseDamage);
        }
        if (isDragonsteelLightning(item.getTier()) && IafConfig.dragonWeaponLightningAbility) {
            if (!attacker.level().isClientSide) {
                ChainLightningUtils.createChainLightning(attacker.level(), target, attacker,
                        getAttackDamage(item));
            }
        }
        // 金龙钢：圣铭之力 — 魔法伤害，正面效果增伤
        if (isDragonsteelGold(item.getTier())) {
            float baseDamage = getAttackDamage(item);
            float bonusDamage = 0;
            // 目标有正面效果时增伤
            if (target.getActiveEffects().stream().anyMatch(e -> e.getEffect().isBeneficial())) {
                bonusDamage = baseDamage * 0.3F;
            }
            target.hurt(attacker.level().damageSources().magic(), baseDamage * 0.5F + bonusDamage);
            // 附加短暂发光效果
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
        }

    }

    default void appendHoverText(Tier tier, ItemStack stack, @Nullable Level worldIn, List<Component> tooltip,
            TooltipFlag flagIn) {
        if (tier == IafItemRegistry.SILVER_TOOL_MATERIAL) {
            tooltip.add(Component.translatable("silvertools.hurt").withStyle(ChatFormatting.GREEN));
        }
        if (tier == IafItemRegistry.MYRMEX_CHITIN_TOOL_MATERIAL) {
            tooltip.add(Component.translatable("myrmextools.hurt").withStyle(ChatFormatting.GREEN));
        }
        if (isDragonsteelFire(tier) && IafConfig.dragonWeaponFireAbility) {
            tooltip.add(Component.translatable("dragon_sword_fire.hurt2").withStyle(ChatFormatting.DARK_RED));
        }
        if (isDragonsteelIce(tier) && IafConfig.dragonWeaponIceAbility) {
            tooltip.add(Component.translatable("dragon_sword_ice.hurt2").withStyle(ChatFormatting.AQUA));
        }
        if (isDragonsteelLightning(tier) && IafConfig.dragonWeaponLightningAbility) {
            tooltip.add(Component.translatable("dragon_sword_lightning.hurt2").withStyle(ChatFormatting.DARK_PURPLE));
        }
        if (isDragonsteelGold(tier)) {
            tooltip.add(Component.translatable("dragon_sword_gold.hurt2").withStyle(ChatFormatting.GOLD));
        }
    }
}
