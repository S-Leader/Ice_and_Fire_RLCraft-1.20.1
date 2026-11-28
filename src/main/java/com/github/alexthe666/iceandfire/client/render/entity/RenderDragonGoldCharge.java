package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.entity.EntityDragonGoldCharge;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The gold dragon's charge: a spinning glowing gold cube, built like the fire dragon's charge (a spinning
 * magma block, see RenderDragonFireCharge) but drawn with golddragon/charge.png (by artsy) on all 6 faces.
 * Drawn full-bright so it glows at night and in caves instead of taking the world's light like the magma block.
 */
public class RenderDragonGoldCharge extends EntityRenderer<EntityDragonGoldCharge> {

    public static final ResourceLocation TEXTURE = new ResourceLocation(
            "iceandfire:textures/models/golddragon/charge.png");

    public RenderDragonGoldCharge(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(EntityDragonGoldCharge entity, float entityYaw, float partialTicks, PoseStack matrixStackIn,
            MultiBufferSource bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();
        // same motion as the fire charge: face the way it's flying and tumble a full turn every 20 ticks
        Vec3 motion = entity.getDeltaMovement();
        float yaw = (float) (Math.PI - Math.atan2(motion.z, motion.x));
        float roll = ((entity.tickCount + partialTicks) % 20) * 18F * Mth.DEG_TO_RAD;
        matrixStackIn.translate(0.0, 0.5, 0.0);
        matrixStackIn.mulPose(Axis.YP.rotation(yaw));
        matrixStackIn.mulPose(Axis.ZP.rotation(roll));
        VertexConsumer buffer = bufferIn.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        drawCube(matrixStackIn, buffer, LightTexture.FULL_BRIGHT);
        matrixStackIn.popPose();
        super.render(entity, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    // A 1x1x1 cube centred on the current pose, with the whole texture on each face.
    private static void drawCube(PoseStack poseStack, VertexConsumer buffer, int light) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        float h = 0.5F;
        // each face: 4 corners (counter-clockwise seen from outside), then its outward normal
        face(m, n, buffer, light, -h, -h, h, h, -h, h, h, h, h, -h, h, h, 0, 0, 1);       // south (+Z)
        face(m, n, buffer, light, h, -h, -h, -h, -h, -h, -h, h, -h, h, h, -h, 0, 0, -1);  // north (-Z)
        face(m, n, buffer, light, h, -h, h, h, -h, -h, h, h, -h, h, h, h, 1, 0, 0);       // east (+X)
        face(m, n, buffer, light, -h, -h, -h, -h, -h, h, -h, h, h, -h, h, -h, -1, 0, 0);  // west (-X)
        face(m, n, buffer, light, -h, h, h, h, h, h, h, h, -h, -h, h, -h, 0, 1, 0);       // up (+Y)
        face(m, n, buffer, light, -h, -h, -h, h, -h, -h, h, -h, h, -h, -h, h, 0, -1, 0);  // down (-Y)
    }

    // one square face: corners 1-4 get the texture's bottom-left, bottom-right, top-right, top-left
    private static void face(Matrix4f m, Matrix3f n, VertexConsumer buffer, int light,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4,
            float nx, float ny, float nz) {
        vertex(m, n, buffer, light, x1, y1, z1, 0, 1, nx, ny, nz);
        vertex(m, n, buffer, light, x2, y2, z2, 1, 1, nx, ny, nz);
        vertex(m, n, buffer, light, x3, y3, z3, 1, 0, nx, ny, nz);
        vertex(m, n, buffer, light, x4, y4, z4, 0, 0, nx, ny, nz);
    }

    private static void vertex(Matrix4f m, Matrix3f n, VertexConsumer buffer, int light,
            float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        buffer.vertex(m, x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(n, nx, ny, nz).endVertex();
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityDragonGoldCharge entity) {
        return TEXTURE;
    }
}
