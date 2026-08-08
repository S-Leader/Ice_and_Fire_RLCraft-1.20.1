package com.github.alexthe666.iceandfire.client.render.entity;

import com.github.alexthe666.iceandfire.entity.EntityDragonArrow;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class RenderDragonArrow extends ArrowRenderer<EntityDragonArrow> {
    private static final ResourceLocation DEFAULT = new ResourceLocation("iceandfire:textures/models/misc/dragonbone_arrow.png");
    private static final ResourceLocation FIRE = new ResourceLocation("iceandfire:textures/models/misc/dragonbone_arrow_fire.png");
    private static final ResourceLocation ICE = new ResourceLocation("iceandfire:textures/models/misc/dragonbone_arrow_ice.png");
    private static final ResourceLocation LIGHTNING = new ResourceLocation("iceandfire:textures/models/misc/dragonbone_arrow_lightning.png");

    public RenderDragonArrow(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull EntityDragonArrow entity) {
        return switch (entity.getType()) {
            case FIRE -> FIRE;
            case ICE -> ICE;
            case LIGHTNING -> LIGHTNING;
            default -> DEFAULT;
        };
    }
}
