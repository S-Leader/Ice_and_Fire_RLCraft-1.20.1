package com.github.alexthe666.iceandfire.item.blooded;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.github.alexthe666.iceandfire.effect.MobEffectFrostbite;
import com.github.alexthe666.iceandfire.effect.MobEffectMelt;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.item.ItemAlchemySword;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BloodedSetEffect {

    private BloodedSetEffect() {
    }

    public static void tryApplySetEffect(Player player, LivingEntity target, float damage) {
        if (player.level().isClientSide)
            return;

        BloodedDragonType.DragonElement element = getFullSetElement(player);
        if (element == null)
            return;

        if (isHoldingMatchingWeapon(player, element))
            return;

        switch (element) {
            case FIRE -> applyFireEffect(player, target);
            case ICE -> applyIceEffect(player, target, damage);
            case LIGHTNING -> applyLightningEffect(player, target, damage);
        }
    }

    private static BloodedDragonType.DragonElement getFullSetElement(Player player) {
        BloodedDragonType.DragonElement element = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ItemBloodedArmor blooded))
                return null;
            BloodedDragonType.DragonElement pieceElement = blooded.getDragonType().getElement();
            if (element == null) {
                element = pieceElement;
            } else if (element != pieceElement) {
                return null;
            }
        }
        return element;
    }

    private static boolean isHoldingMatchingWeapon(Player player, BloodedDragonType.DragonElement element) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.isEmpty())
            return false;

        if (mainHand.getItem() instanceof ItemAlchemySword) {
            return switch (element) {
                case FIRE -> mainHand.is(IafItemRegistry.DRAGONBONE_SWORD_FIRE.get());
                case ICE -> mainHand.is(IafItemRegistry.DRAGONBONE_SWORD_ICE.get());
                case LIGHTNING -> mainHand.is(IafItemRegistry.DRAGONBONE_SWORD_LIGHTNING.get());
            };
        }

        return false;
    }

    private static void applyFireEffect(Player player, LivingEntity target) {
        if (!IafConfig.dragonWeaponFireAbility)
            return;
        MobEffectMelt.applyMelt(target, player, 0, 160);
    }

    private static void applyIceEffect(Player player, LivingEntity target, float damage) {
        if (!IafConfig.dragonWeaponIceAbility)
            return;
        MobEffectFrostbite.applyFrostbite(target, player, 200, damage);
    }

    private static void applyLightningEffect(Player player, LivingEntity target, float damage) {
        if (!IafConfig.dragonWeaponLightningAbility)
            return;
        if (!player.level().isClientSide) {
            ChainLightningUtils.createChainLightning(player.level(), target, player, damage);
        }
    }
}
