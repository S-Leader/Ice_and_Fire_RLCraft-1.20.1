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
 * 龙空中飞行目标选择AI（移植自1.12.2 RLC）
 * 核心逻辑：给空中飞行的龙寻找并更新airTarget巡航点/攻击点，并独占MOVE标志。
 */
public class DragonAIAirTarget extends Goal {
    private final EntityDragonBase dragon;

    public DragonAIAirTarget(EntityDragonBase dragon) {
        this.dragon = dragon;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!dragon.isFlying() && !dragon.isHovering()) {
            return false;
        }
        if (dragon.isOrderedToSit() || dragon.isSleeping()) {
            return false;
        }
        if (dragon.isBaby()) {
            return false;
        }
        if (dragon.getControllingPassenger() != null) {
            return false;
        }

        // 已到达目标附近，清除目标，促使寻找下一个目标
        if (dragon.airTarget != null && dragon.getDistanceSquared(Vec3.atCenterOf(dragon.airTarget)) < 16) {
            dragon.airTarget = null;
        }

        if (dragon.airTarget == null) {
            // 有攻击目标时，向其位置飞行
            LivingEntity attackTarget = dragon.getTarget();
            if (attackTarget != null && attackTarget.isAlive()) {
                dragon.airTarget = attackTarget.blockPosition();
                return true;
            }
            // 无攻击目标时，选择随机空中巡航点
            BlockPos pos = getNearbyAirTarget();
            if (pos == null) {
                return false;
            }
            dragon.airTarget = pos;
            return true;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!dragon.isFlying() && !dragon.isHovering()) {
            return false;
        }
        if (dragon.isOrderedToSit() || dragon.isSleeping()) {
            return false;
        }
        if (dragon.isBaby()) {
            return false;
        }
        if (dragon.getControllingPassenger() != null) {
            return false;
        }
        LivingEntity attackTarget = dragon.getTarget();
        if (attackTarget != null && attackTarget.isAlive()) {
            dragon.airTarget = attackTarget.blockPosition();
            return true;
        }
        return dragon.airTarget != null && !dragon.isTargetBlocked(Vec3.atCenterOf(dragon.airTarget));
    }

    private BlockPos getNearbyAirTarget() {
        if (dragon instanceof EntityIceDragon && dragon.isInWater()) {
            return DragonUtils.getWaterBlockInView(dragon);
        }
        return DragonUtils.getBlockInView(dragon);
    }
}
