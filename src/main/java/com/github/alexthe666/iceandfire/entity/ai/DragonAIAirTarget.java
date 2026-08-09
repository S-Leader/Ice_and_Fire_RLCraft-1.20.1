package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntityIceDragon;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 1.12.2 RLCraft dragon air-target AI, adapted to 1.20.1.
 * Attack targets are followed directly instead of orbiting around them.
 */
public class DragonAIAirTarget extends Goal {
    private final EntityDragonBase dragon;

    public DragonAIAirTarget(EntityDragonBase dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (dragon.lookingForRoostAIFlag) {
            return false;
        }
        if ((!dragon.isFlying() && !dragon.isHovering()) || dragon.onGround()) {
            return false;
        }
        if (dragon.isOrderedToSit() || dragon.isSleeping() || dragon.isBaby() || dragon.getControllingPassenger() != null) {
            return false;
        }

        if (dragon.airTarget == null || dragon.isTargetBlocked(Vec3.atCenterOf(dragon.airTarget))) {
            LivingEntity attackTarget = dragon.getTarget();
            if (attackTarget != null && attackTarget.isAlive()) {
                dragon.airTarget = BlockPos.containing(attackTarget.position());
                return true;
            }

            BlockPos pos = getNearbyAirTarget();
            if (pos == null) {
                dragon.airTarget = null;
                return false;
            }
            dragon.airTarget = pos;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (dragon.lookingForRoostAIFlag) {
            return false;
        }
        if (!dragon.isFlying() && !dragon.isHovering()) {
            return false;
        }
        if (dragon.isOrderedToSit() || dragon.isSleeping() || dragon.isBaby() || dragon.getControllingPassenger() != null) {
            return false;
        }

        LivingEntity attackTarget = dragon.getTarget();
        if (attackTarget != null && attackTarget.isAlive()) {
            dragon.airTarget = BlockPos.containing(attackTarget.position());
            return true;
        }

        if (dragon.airTarget != null) {
            if (dragon.isTargetBlocked(Vec3.atCenterOf(dragon.airTarget))) {
                dragon.airTarget = null;
                return false;
            }
            return true;
        }
        return false;
    }

    private BlockPos getNearbyAirTarget() {
        // Keep the modern ice-dragon water adaptation; old RLC logic otherwise uses getBlockInView directly.
        if (dragon instanceof EntityIceDragon && dragon.isInWater()) {
            return DragonUtils.getWaterBlockInView(dragon);
        }
        BlockPos pos = DragonUtils.getBlockInView(dragon);
        return pos != null && dragon.level().isEmptyBlock(pos) ? pos : null;
    }
}
