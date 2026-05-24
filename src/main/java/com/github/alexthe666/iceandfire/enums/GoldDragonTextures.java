package com.github.alexthe666.iceandfire.enums;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntityDragonSkull;
import com.github.alexthe666.iceandfire.entity.EntityGoldDragon;
import net.minecraft.resources.ResourceLocation;

/**
 * 金龙纹理管理 — 独立于 EnumDragonTextures
 * 金龙只有一种变体，使用 golddragon/ 文件夹
 */
public final class GoldDragonTextures {

    private static final String BASE = "iceandfire:textures/models/golddragon/";

    public static final ResourceLocation[] STAGE = new ResourceLocation[6];
    public static final ResourceLocation[] SLEEPING = new ResourceLocation[6];
    public static final ResourceLocation[] EYES = new ResourceLocation[6];
    public static final ResourceLocation[] SKELETON = new ResourceLocation[6];
    public static final ResourceLocation MALE_OVERLAY = new ResourceLocation(BASE + "male_gold.png");
    public static final ResourceLocation EGG = new ResourceLocation(BASE + "egg_gold.png");

    static {
        for (int i = 1; i <= 5; i++) {
            STAGE[i] = new ResourceLocation(BASE + "gold_" + i + ".png");
            SLEEPING[i] = new ResourceLocation(BASE + "gold_" + i + "_sleeping.png");
            EYES[i] = new ResourceLocation(BASE + "gold_" + i + "_eyes.png");
            SKELETON[i] = new ResourceLocation(BASE + "gold_skeleton_" + i + ".png");
        }
    }

    private GoldDragonTextures() {
    }

    public static ResourceLocation getTexture(EntityDragonBase dragon) {
        int stage = clampStage(dragon.getDragonStage());
        if (dragon.isModelDead()) {
            if (dragon.getDeathStage() >= (dragon.getAgeInDays() / 5) / 2) {
                return SKELETON[stage];
            } else {
                return SLEEPING[stage];
            }
        }
        if (dragon.isSleeping() || dragon.isBlinking()) {
            return SLEEPING[stage];
        }
        return STAGE[stage];
    }

    public static ResourceLocation getEyeTexture(EntityDragonBase dragon) {
        return EYES[clampStage(dragon.getDragonStage())];
    }

    public static ResourceLocation getSkullTexture(EntityDragonSkull skull) {
        return SKELETON[clampStage(skull.getDragonStage())];
    }

    private static int clampStage(int stage) {
        return Math.max(1, Math.min(5, stage));
    }
}
