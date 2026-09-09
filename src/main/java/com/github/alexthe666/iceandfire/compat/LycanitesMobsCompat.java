package com.github.alexthe666.iceandfire.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

public final class LycanitesMobsCompat {
    private static final String LYCANITES_MOBS_MOD_ID = "lycanitesmobs";
    private static final ResourceLocation PARALYSIS = new ResourceLocation(LYCANITES_MOBS_MOD_ID, "paralysis");

    private LycanitesMobsCompat() {
    }

    public static void applyParalysis(LivingEntity target, int duration) {
        if (duration <= 0 || !ModList.get().isLoaded(LYCANITES_MOBS_MOD_ID)) {
            return;
        }

        MobEffect paralysis = ForgeRegistries.MOB_EFFECTS.getValue(PARALYSIS);
        if (paralysis != null && !target.hasEffect(paralysis)) {
            target.addEffect(new MobEffectInstance(paralysis, duration));
        }
    }
}
