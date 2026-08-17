package com.github.alexthe666.iceandfire.mixins;

import com.github.alexthe666.iceandfire.effect.IafMobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMeltFireMixin {

    @Inject(method = "displayFireAnimation", at = @At("HEAD"), cancellable = true)
    private void iceandfire$showMeltFire(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasEffect(IafMobEffects.MELT.get())) {
            cir.setReturnValue(true);
        }
    }
}
