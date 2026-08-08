package com.github.alexthe666.iceandfire.entity.props;

import com.github.alexthe666.iceandfire.entity.EntityShivaxiDragon;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 1.20.1 port of RLCraft Ice & Fire's SHIVAXI_BLAZED entity effect.
 *
 * Severity 0 (the normal Shivaxi hit):
 * - horizontal movement x0.5
 * - positive vertical movement x0.5
 * - 1 fire damage every tick
 * - +1 fire damage every 20 ticks
 * - water immediately removes the effect
 *
 * Severity 1+: applies the movement reduction a second time and deals 2 fire damage every tick.
 * Severity 2+: the 20-tick pulse is also 2 damage, matching the 1.12.2 handler.
 */
public class ShivaxiBlazeData {
    private int ticks;
    private int severity;
    private boolean triggerClientUpdate;

    public void setShivaxiBlazed(int duration) {
        setShivaxiBlazed(duration, 0);
    }

    public void setShivaxiBlazed(int duration, int severity) {
        if (duration <= 0) {
            return;
        }
        boolean changed = false;
        if (duration > this.ticks) {
            this.ticks = duration;
            changed = true;
        }
        if (severity > this.severity) {
            this.severity = severity;
            changed = true;
        }
        if (changed) {
            this.triggerClientUpdate = true;
        }
    }

    public boolean isShivaxiBlazed() {
        return this.ticks > 0;
    }

    public int getTicks() {
        return this.ticks;
    }

    public int getSeverity() {
        return this.severity;
    }

    public void tick(final LivingEntity entity) {
        if (this.ticks <= 0) {
            return;
        }

        // 1.12.2 tickTime() happened before effect processing.
        --this.ticks;

        if (this.ticks <= 0 || !canBeApplied(entity) || entity.isInWaterOrBubble()) {
            clear(entity, true);
            return;
        }

        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(motion.x * 0.5D, motion.y > 0.0D ? motion.y * 0.5D : motion.y, motion.z * 0.5D);

        if (this.severity > 0) {
            motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x * 0.5D, motion.y > 0.0D ? motion.y * 0.5D : motion.y, motion.z * 0.5D);
        }

        if (!entity.level().isClientSide()) {
            if (this.ticks % 20 == 0) {
                entity.hurt(entity.level().damageSources().onFire(), this.severity > 1 ? 2.0F : 1.0F);
            }
            entity.hurt(entity.level().damageSources().onFire(), this.severity > 0 ? 2.0F : 1.0F);
        }
    }

    private static boolean canBeApplied(final LivingEntity entity) {
        if (!entity.isAlive() || entity instanceof EntityShivaxiDragon) {
            return false;
        }
        if (entity instanceof Player player) {
            return !player.isSpectator() && !player.getAbilities().instabuild;
        }
        // 1.12.2 EntityEffectEnum only accepted EntityLiving (modern Mob) plus players.
        return entity instanceof Mob;
    }

    private void clear(final LivingEntity entity, boolean playEndFx) {
        boolean wasActive = true;
        this.ticks = 0;
        this.severity = 0;
        this.triggerClientUpdate = true;

        if (entity.isOnFire()) {
            entity.clearFire();
        }

        if (playEndFx && wasActive && entity.level().isClientSide()) {
            for (int i = 0; i < 4; i++) {
                entity.level().addParticle(ParticleTypes.SMOKE,
                        entity.getX() + (entity.getRandom().nextDouble() - 0.5D) * entity.getBbWidth(),
                        entity.getY() + entity.getRandom().nextDouble() * entity.getBbHeight(),
                        entity.getZ() + (entity.getRandom().nextDouble() - 0.5D) * entity.getBbWidth(),
                        0.0D, 0.0D, 0.0D);
            }
            entity.playSound(SoundEvents.GENERIC_EXTINGUISH_FIRE, 3.0F, 1.0F);
        }
    }

    public void serialize(CompoundTag root) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ticks", this.ticks);
        tag.putInt("severity", this.severity);
        root.put("shivaxiBlazeData", tag);
    }

    public void deserialize(CompoundTag root) {
        CompoundTag tag = root.getCompound("shivaxiBlazeData");
        this.ticks = tag.getInt("ticks");
        this.severity = tag.getInt("severity");
    }

    public boolean doesClientNeedUpdate() {
        if (this.triggerClientUpdate) {
            this.triggerClientUpdate = false;
            return true;
        }
        return false;
    }
}
