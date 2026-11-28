package com.github.alexthe666.iceandfire.client.particle;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The gold dragon's breath flame. Moves exactly like the fire dragon's flame (same stream towards the
 * breath target), but draws gold_dragon_flame.png: a 4x4 sheet of 16 frames (a flame growing, then
 * dying out), played once over the particle's lifetime. Texture by artsy.
 */
public class ParticleDragonGoldFlame extends ParticleDragonFlame {

    private static final ResourceLocation GOLD_FLAME = new ResourceLocation("iceandfire:textures/particles/gold_dragon_flame.png");
    // 4 frames per row and column, 16 in total
    private static final int SHEET_SIZE = 4;
    private static final int FRAMES = SHEET_SIZE * SHEET_SIZE;

    // loose flame (no dragon), e.g. the burst where the breath lands
    public ParticleDragonGoldFlame(ClientLevel world, double x, double y, double z, double motX, double motY, double motZ, float size) {
        super(world, x, y, z, motX, motY, motZ, size);
        pinLifetime();
    }

    // breath stream flame flying from the dragon's head to its target
    public ParticleDragonGoldFlame(ClientLevel world, double x, double y, double z, double motX, double motY, double motZ, EntityDragonBase dragon, int startingAge) {
        super(world, x, y, z, motX, motY, motZ, dragon, startingAge);
        pinLifetime();
    }

    // Particle.tick() removes the particle once the vanilla 'lifetime' field runs out, and that field starts as a
    // random 4-40 ticks, so the flame could vanish mid-animation. Pin it to the real lifetime (10 loose, 30 in a
    // breath stream) so all 16 frames always play.
    private void pinLifetime() {
        this.lifetime = this.getLifetime();
    }

    @Override
    protected ResourceLocation getFlameTexture() {
        return GOLD_FLAME;
    }

    @Override
    protected int getSheetSize() {
        return SHEET_SIZE;
    }

    @Override
    protected int getFrame(float partialTicks) {
        // how far through its life the particle is (0 to 1), mapped onto the 16 frames
        float progress = (this.age + partialTicks) / (float) this.getLifetime();
        return Mth.clamp((int) (progress * FRAMES), 0, FRAMES - 1);
    }
}
