package com.github.alexthe666.iceandfire.mixin;

import com.github.alexthe666.iceandfire.entity.EntityMutlipartPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow @Final Minecraft minecraft;

    @Inject(method = "pick", at = @At("TAIL"))
    private void iaf$redirectMultipartHit(float partialTicks, CallbackInfo ci) {
        if (this.minecraft.hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof EntityMutlipartPart multipart) {
            Entity parent = multipart.getParent();
            if (parent != null) {
                this.minecraft.hitResult = new EntityHitResult(parent, entityHit.getLocation());
            }
        }
    }
}
