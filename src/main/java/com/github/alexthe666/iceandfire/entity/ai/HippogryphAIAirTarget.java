package com.github.alexthe666.iceandfire.entity.ai;

import com.github.alexthe666.iceandfire.entity.EntityHippogryph;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * 骏鹰空中飞行目标选择AI（移植自1.12.2 RLC）
 * 为飞行中的骏鹰选择空中导航目标点(airTarget)
 */
public class HippogryphAIAirTarget extends Goal {
    private final EntityHippogryph hippogryph;

    public HippogryphAIAirTarget(EntityHippogryph hippogryph) {
        this.hippogryph = hippogryph;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!hippogryph.isFlying() && !hippogryph.isHovering()) {
            return false;
        }
        if (hippogryph.isOrderedToSit()) {
            return false;
        }
        if (hippogryph.isBaby()) {
            return false;
        }
        if (hippogryph.getOwner() != null && hippogryph.getPassengers().contains(hippogryph.getOwner())) {
            return false;
        }
        // 已到达目标附近，清除目标
        if (hippogryph.airTarget != null && hippogryph.distanceToSqr(new Vec3(hippogryph.airTarget.getX(), hippogryph.getY(), hippogryph.airTarget.getZ())) < 3) {
            hippogryph.airTarget = null;
        }

        if (hippogryph.airTarget == null) {
            // 有攻击目标时，向其位置飞行
            LivingEntity attackTarget = hippogryph.getTarget();
            if (attackTarget != null) {
                BlockPos pos = attackTarget.blockPosition();
                if (hippogryph.level().isEmptyBlock(pos)) {
                    hippogryph.airTarget = pos;
                    return true;
                }
            }
            // 无攻击目标时，选择随机空中巡航点
            BlockPos pos = getNearbyAirTarget();
            if (pos == null) {
                return false;
            }
            hippogryph.airTarget = pos;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!hippogryph.isFlying() && !hippogryph.isHovering()) {
            return false;
        }
        if (hippogryph.isOrderedToSit()) {
            return false;
        }
        if (hippogryph.isBaby()) {
            return false;
        }
        // 有攻击目标时更新airTarget
        LivingEntity attackTarget = hippogryph.getTarget();
        if (attackTarget != null) {
            BlockPos pos = attackTarget.blockPosition();
            if (hippogryph.level().isEmptyBlock(pos)) {
                hippogryph.airTarget = pos;
            } else {
                hippogryph.airTarget = null;
            }
        }
        return hippogryph.airTarget != null;
    }

    /**
     * 获取附近的空中目标点
     */
    private BlockPos getNearbyAirTarget() {
        for (int i = 0; i < 10; i++) {
            BlockPos pos = DragonUtils.getBlockInViewHippogryph(hippogryph, 0);
            if (pos != null && hippogryph.level().isEmptyBlock(pos)) {
                return pos;
            }
        }
        return null;
    }
}
