package com.github.alexthe666.iceandfire.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/** 1.20.1 renderer port of the 1.12.2 RenderShivaxiFire overlay. */
public final class ShivaxiBlazeRenderer {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("iceandfire", "textures/models/misc/shivaxi_fire.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucent(TEXTURE);

    private ShivaxiBlazeRenderer() {
    }

    public static void render(LivingEntity entity, PoseStack stack, MultiBufferSource buffers, int packedLight) {
        stack.pushPose();
        float scale = entity.getBbWidth() * 1.8F;
        stack.scale(scale, scale, scale);

        float remainingHeight = entity.getBbHeight() / scale;
        float halfWidth = 0.5F;
        float yOffset = (float) (entity.getY() - entity.getBoundingBox().minY);
        float zOffset = 0.0F;

        Quaternionf camera = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
        stack.mulPose(new Quaternionf(0.0F, camera.y, 0.0F, camera.w));
        stack.translate(0.0F, 0.0F, ((int) remainingHeight) * 0.02F);

        VertexConsumer consumer = buffers.getBuffer(RENDER_TYPE);
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        int layer = 0;

        while (remainingHeight > 0.0F) {
            boolean flipped = layer % 2 == 0;
            int frame = entity.tickCount % 32;
            float minU = flipped ? 0.5F : 0.0F;
            float minV = frame / 32.0F;
            float maxU = flipped ? 1.0F : 0.5F;
            float maxV = (frame + 1) / 32.0F;
            if (flipped) {
                float swap = maxU;
                maxU = minU;
                minU = swap;
            }

            vertex(consumer, pose, normal, halfWidth, -yOffset, zOffset, maxU, maxV, packedLight);
            vertex(consumer, pose, normal, -halfWidth, -yOffset, zOffset, minU, maxV, packedLight);
            vertex(consumer, pose, normal, -halfWidth, 1.4F - yOffset, zOffset, minU, minV, packedLight);
            vertex(consumer, pose, normal, halfWidth, 1.4F - yOffset, zOffset, maxU, minV, packedLight);

            remainingHeight -= 0.45F;
            yOffset -= 0.45F;
            halfWidth *= 0.9F;
            zOffset += 0.03F;
            ++layer;
        }
        stack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Matrix3f normal,
                               float x, float y, float z, float u, float v, int light) {
        consumer.vertex(pose, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
