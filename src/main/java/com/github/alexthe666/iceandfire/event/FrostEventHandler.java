package com.github.alexthe666.iceandfire.event;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.effect.IafMobEffects;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IceAndFire.MODID)
public class FrostEventHandler {

    private static final float FROSTBURN_DAMAGE_BONUS = 0.20f;

    private static final float ICE_WEAPON_EXTRA_BONUS = 0.10f;

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide())
            return;
        if (!target.hasEffect(IafMobEffects.FROSTBURN.get()))
            return;

        float bonus = FROSTBURN_DAMAGE_BONUS;

        if (event.getSource().getEntity() instanceof LivingEntity attacker) {
            if (isIceDragonWeapon(attacker.getMainHandItem())) {
                bonus += ICE_WEAPON_EXTRA_BONUS;
            }
        }

        event.setAmount(event.getAmount() * (1.0f + bonus));
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide())
            return;
        if (!entity.hasEffect(IafMobEffects.FROSTBURN.get()))
            return;

        if (entity.hasEffect(IafMobEffects.MELT.get())) {
            entity.removeEffect(IafMobEffects.FROSTBURN.get());
            entity.removeEffect(IafMobEffects.MELT.get());
            return;
        }

        if (entity.isOnFire()) {
            entity.removeEffect(IafMobEffects.FROSTBURN.get());
        }
    }

    private static boolean isIceDragonWeapon(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (!(stack.getItem() instanceof TieredItem tiered))
            return false;

        return tiered.getTier() == IafItemRegistry.ICE_DRAGONBONE_TOOL_MATERIAL;
    }
}
