package com.github.alexthe666.iceandfire.entity.props;

import com.github.alexthe666.iceandfire.entity.EntityShivaxiDragon;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** RLCraft ShivaxiBlazed entity effect, ported to the existing 1.20.1 entity-data capability. */
public class ShivaxiBlazeData {
    private int ticks;
    private boolean triggerClientUpdate;

    public void setShivaxiBlazed(int duration) {
        if (duration > ticks) {
            ticks = duration;
            triggerClientUpdate = true;
        }
    }

    public boolean isShivaxiBlazed() {
        return ticks > 0;
    }

    public void tick(final LivingEntity entity) {
        if (ticks <= 0) {
            return;
        }
        if (!entity.isAlive() || entity instanceof EntityShivaxiDragon || entity.isInWaterOrBubble()) {
            ticks = 0;
            triggerClientUpdate = true;
            return;
        }

        // RLC effect: heavy movement suppression plus persistent fire damage.
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5D, entity.getDeltaMovement().y > 0 ? 0.5D : 1.0D, 0.5D));
        if (!entity.level().isClientSide() && !(entity instanceof Player player && player.getAbilities().invulnerable)) {
            entity.hurt(entity.level().damageSources().onFire(), 1.0F);
            if (ticks % 20 == 0) {
                entity.hurt(entity.level().damageSources().onFire(), 1.0F);
            }
        }
        ticks--;
        if (ticks == 0) {
            triggerClientUpdate = true;
        }
    }

    public void serialize(CompoundTag root) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("ticks", ticks);
        root.put("shivaxiBlazeData", tag);
    }

    public void deserialize(CompoundTag root) {
        ticks = root.getCompound("shivaxiBlazeData").getInt("ticks");
    }

    public boolean doesClientNeedUpdate() {
        if (triggerClientUpdate) {
            triggerClientUpdate = false;
            return true;
        }
        return false;
    }
}
