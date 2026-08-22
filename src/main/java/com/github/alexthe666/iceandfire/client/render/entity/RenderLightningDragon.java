package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.iceandfire.client.particle.LightningBoltData;
import com.github.alexthe666.iceandfire.client.particle.LightningRender;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntityLightningDragon;
import com.github.alexthe666.iceandfire.entity.EntityShivaxiDragon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;

public class RenderLightningDragon extends RenderDragonBase {

    private static final LightningBoltData.BoltRenderInfo SHIVAXI_BLUE = new LightningBoltData.BoltRenderInfo(0.5F, 0.25F, 0.25F, 0.15F, new Vector4f(0.15F, 0.55F, 1.0F, 0.8F), 0.8F);
    private final LightningRender lightningRender = new LightningRender();

    public RenderLightningDragon(EntityRendererProvider.Context context, AdvancedEntityModel model, int dragonType) {
        super(context, model, dragonType);
    }

    @Override
    public boolean shouldRender(@NotNull EntityDragonBase livingEntityIn, @NotNull Frustum camera, double camX, double camY, double camZ) {
        if (super.shouldRender(livingEntityIn, camera, camX, camY, camZ)) {
            return true;
        } else {
            EntityLightningDragon lightningDragon = (EntityLightningDragon) livingEntityIn;
            if (lightningDragon.hasLightningTarget()) {
                Vec3 Vector3d1 = lightningDragon.getHeadPosition();
                Vec3 Vector3d = new Vec3(lightningDragon.getLightningTargetX(), lightningDragon.getLightningTargetY(), lightningDragon.getLightningTargetZ());
                return camera.isVisible(new AABB(Vector3d1.x, Vector3d1.y, Vector3d1.z, Vector3d.x, Vector3d.y, Vector3d.z));
            }
            return false;
        }
    }

    @Override
    public void render(@NotNull EntityDragonBase entityIn, float entityYaw, float partialTicks, @NotNull PoseStack matrixStackIn, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
        EntityLightningDragon lightningDragon = (EntityLightningDragon) entityIn;
        matrixStackIn.pushPose();
        if (lightningDragon.hasLightningTarget()) {
            double dist = Minecraft.getInstance().player.distanceTo(lightningDragon);
            if (dist <= Math.max(256, Minecraft.getInstance().options.renderDistance().get() * 16F)) {
                Vec3 Vector3d1 = lightningDragon.getHeadPosition();
                Vec3 Vector3d = new Vec3(lightningDragon.getLightningTargetX(), lightningDragon.getLightningTargetY(), lightningDragon.getLightningTargetZ());
                float energyScale = 0.4F * lightningDragon.getScale();
                LightningBoltData.BoltRenderInfo renderInfo = lightningDragon instanceof EntityShivaxiDragon ? SHIVAXI_BLUE : LightningBoltData.BoltRenderInfo.ELECTRICITY;
                LightningBoltData bolt = new LightningBoltData(renderInfo, Vector3d1, Vector3d, 15).size(0.05F * getBoundedScale(energyScale, 0.5F, 2)).lifespan(4).spawn(LightningBoltData.SpawnFunction.NO_DELAY);
                lightningRender.update(null, bolt, partialTicks);
                matrixStackIn.translate(-lightningDragon.getX(), -lightningDragon.getY(), -lightningDragon.getZ());
                lightningRender.render(partialTicks, matrixStackIn, bufferIn);
            }
        }
        matrixStackIn.popPose();

    }

    private static float getBoundedScale(float scale, float min, float max) {
        return min + scale * (max - min);
    }
}
