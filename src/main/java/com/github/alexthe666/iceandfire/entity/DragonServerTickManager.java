package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.EntityDreadQueen;
import com.github.alexthe666.iceandfire.entity.util.DragonUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;


public class DragonServerTickManager {
    private final EntityDragonBase dragon;
    private long ticksAfterClearingTarget;

    public DragonServerTickManager(EntityDragonBase dragon) {
        this.dragon = dragon;
    }

    public void updateDragonServer() {
        // Update dragon rider
        dragon.updateRider();

        // Update dragon pitch
        dragon.updatePitch(dragon.yo - dragon.getY());

        if (dragon.lookingForRoostAIFlag && dragon.getLastHurtByMob() != null || dragon.isSleeping()) {
            dragon.lookingForRoostAIFlag = false;
        }
        if (IafConfig.doDragonsSleep && !dragon.isSleeping() && !dragon.isTimeToWake() && dragon.getPassengers().isEmpty() && this.dragon.getCommand() != 2) {
            if (dragon.hasHomePosition
                    && dragon.getRestrictCenter() != null
                    && DragonUtils.isInHomeDimension(dragon)
                    && dragon.distanceToSqr(Vec3.atCenterOf(dragon.getRestrictCenter())) > dragon.getBbWidth() * 10
                    && this.dragon.getCommand() != 2 && this.dragon.getCommand() != 1) {
                dragon.lookingForRoostAIFlag = true;
            } else {
                dragon.lookingForRoostAIFlag = false;
                if ((/* Avoid immediately sleeping after killing the target */ dragon.level().getGameTime() - ticksAfterClearingTarget >= 20) && !dragon.isInWater() && dragon.onGround() && !dragon.isFlying() && !dragon.isHovering() && dragon.getTarget() == null) {
                    dragon.setInSittingPose(true);
                }
            }
        } else {
            dragon.lookingForRoostAIFlag = false;
        }
        if (dragon.isSleeping() && (dragon.isFlying() || dragon.isHovering() || dragon.isInWater() || (dragon.level().canSeeSkyFromBelowWater(dragon.blockPosition()) && dragon.isTimeToWake() && !dragon.isTame() || dragon.isTimeToWake() && dragon.isTame()) || dragon.getTarget() != null || !dragon.getPassengers().isEmpty())) {
            dragon.setInSittingPose(false);
        }
        if (dragon.isOrderedToSit() && dragon.getControllingPassenger() != null) {
            dragon.setOrderedToSit(false);
        }
        if (dragon.blockBreakCounter <= 0) {
            dragon.blockBreakCounter = IafConfig.dragonBreakBlockCooldown;
        }
        dragon.updateBurnTarget();
        if (dragon.isOrderedToSit()) {
            if (dragon.getCommand() != 1 || dragon.getControllingPassenger() != null)
                dragon.setOrderedToSit(false);
        } else {
            if (dragon.getCommand() == 1 && dragon.getControllingPassenger() == null)
                dragon.setOrderedToSit(true);
        }
        if (dragon.isOrderedToSit()) {
            dragon.getNavigation().stop();
        }
        if (dragon.isInLove()) {
            dragon.level().broadcastEntityEvent(dragon, (byte) 18);
        }
        // 与1.12.2一致仅比较XZ（贴墙上下浮动不清零）；用floor保证负坐标区判定正确
        // （原实现用(int)截断与blockPosition()的floor比较，负坐标下恒差1格，导致卡住检测在负坐标区从不生效）
        if (Mth.floor(dragon.xo) == dragon.getBlockX() && Mth.floor(dragon.zo) == dragon.getBlockZ()) {
            dragon.ticksStill++;
        } else {
            dragon.ticksStill = 0;
        }
        if (dragon.getControllingPassenger() == null && dragon.isTackling() && !dragon.isFlying() && dragon.onGround()) {
            dragon.tacklingTicks++;
            if (dragon.tacklingTicks == 40) {
                dragon.tacklingTicks = 0;
                dragon.setTackling(false);
                dragon.setFlying(false);
            }
        }
        if (dragon.getRandom().nextInt(500) == 0 && !dragon.isModelDead() && !dragon.isSleeping()) {
            dragon.roar();
        }

        // 1.12.2 RLC: a wild adult dragon that is stuck against terrain uses a tail-whack
        // and breaks open the obstruction on the impact frame.
        if (IafConfig.dragonDigWhenStuck && dragon.getDragonStage() >= 3 && dragon.isStuck() && dragon.getControllingPassenger() == null) {
            if (dragon.getAnimation() != EntityDragonBase.ANIMATION_TAILWHACK) {
                dragon.setAnimation(EntityDragonBase.ANIMATION_TAILWHACK);
            }
            if (dragon.getAnimationTick() == 10 && DragonUtils.canGrief(dragon)
                    && ForgeEventFactory.getMobGriefingEvent(dragon.level(), dragon)) {
                Explosion.BlockInteraction mode = Explosion.BlockInteraction.DESTROY;
                Explosion explosion = new Explosion(dragon.level(), dragon, dragon.getX(), dragon.getY(), dragon.getZ(),
                        (4F * dragon.getDragonStage()) - 2F, false, mode);
                explosion.explode();
                explosion.finalizeExplosion(true);
                dragon.ticksStill = 0;
            }
        }

        // 1.12.2 RLC aerial melee: attackDecision=true makes the dragon dive/tackle its target.
        LivingEntity aerialTarget = dragon.getTarget();
        if (dragon.getControllingPassenger() == null && aerialTarget != null && aerialTarget.isAlive()) {
            if (dragon.isFlying() && dragon.attackDecision
                    && dragon.isDirectPathBetweenPoints(dragon.position().add(0, dragon.getBbHeight() * 0.5D, 0),
                    aerialTarget.position().add(0, aerialTarget.getBbHeight() * 0.5D, 0))) {
                dragon.setTackling(true);
            }

            if (dragon.isTackling() && dragon.getBoundingBox().inflate(2.0D).intersects(aerialTarget.getBoundingBox())) {
                dragon.attackDecision = true;
                aerialTarget.hurt(aerialTarget.level().damageSources().mobAttack(dragon), dragon.getDragonStage() * 3.0F);
                dragon.spawnGroundEffects();
                dragon.setTackling(false);
                dragon.setFlying(false);
                dragon.setHovering(false);
            } else if (dragon.isFlying() && dragon.getBoundingBox().inflate(3.0D).intersects(aerialTarget.getBoundingBox())) {
                dragon.doHurtTarget(aerialTarget);
            }

            // Old behavior switches a distant adult dragon from melee dive to ranged attack.
            double averageEdge = (dragon.getBbWidth() + dragon.getBbHeight() + dragon.getBbWidth()) / 3.0D;
            double rangedThreshold = Math.min(averageEdge * 5.0D, 25.0D);
            if (dragon.attackDecision && dragon.getDragonStage() > 1 && dragon.distanceTo(aerialTarget) > rangedThreshold) {
                dragon.attackDecision = false;
                dragon.setTackling(false);
            }
        }

        if (dragon.getControllingPassenger() == null && dragon.isTackling() && (dragon.getTarget() == null || !dragon.attackDecision)) {
            dragon.setTackling(false);
            dragon.randomizeAttacks();
        }
        if (dragon.isPassenger()) {
            dragon.setFlying(false);
            dragon.setHovering(false);
            dragon.setInSittingPose(false);
        }
        if (dragon.isFlying() && dragon.tickCount % 40 == 0 || dragon.isFlying() && dragon.isSleeping()) {
            dragon.setInSittingPose(false);
        }
        if (!dragon.canMove()) {
            if (dragon.getTarget() != null) {
                dragon.setTarget(null);
                ticksAfterClearingTarget = dragon.level().getGameTime();
            }
            dragon.getNavigation().stop();
        }
        if (!dragon.isTame()) {
            dragon.updateCheckPlayer();
        }
        if (dragon.isModelDead() && (dragon.isFlying() || dragon.isHovering())) {
            dragon.setFlying(false);
            dragon.setHovering(false);
        }
        if (dragon.getControllingPassenger() == null) {
            // AI飞行时停止pathfinder导航，由flyAround()全权控制移动
            // 避免pathfinder和flyAround()两套导航系统同时修改deltaMovement导致冲突
            if (dragon.isFlying() || dragon.isHovering()) {
                dragon.getNavigation().stop();
            }
            if ((dragon.useFlyingPathFinder() || dragon.isHovering()) && dragon.navigatorType != 1) {
                dragon.switchNavigator(1);
            }
        } else {
            if ((dragon.useFlyingPathFinder() || dragon.isHovering()) && dragon.navigatorType != 2) {
                dragon.switchNavigator(2);
            }
        }
        if (dragon.getControllingPassenger() == null && !dragon.useFlyingPathFinder() && !dragon.isHovering() && dragon.navigatorType != 0) {
            dragon.switchNavigator(0);
        }
        // 龙降落：仅在不在空中且想要降落时取消飞行
        if (dragon.getControllingPassenger() == null && !dragon.isOverAir() && dragon.doesWantToLand() && (dragon.isFlying() || dragon.isHovering()) && !dragon.isInWater()) {
            dragon.setFlying(false);
            dragon.setHovering(false);
        }
        if (dragon.isHovering()) {
            if (dragon.isFlying() && dragon.flyTicks > 40) {
                dragon.setHovering(false);
                dragon.setFlying(true);
            }
            dragon.hoverTicks++;
        } else {
            dragon.hoverTicks = 0;
        }
        if (dragon.isHovering() && !dragon.isFlying()) {
            if (dragon.isSleeping()) {
                dragon.setHovering(false);
            }
            // Slowly land the hovering dragon
            if (dragon.getControllingPassenger() == null && dragon.doesWantToLand() && !dragon.onGround() && !dragon.isInWater()) {
                dragon.setDeltaMovement(dragon.getDeltaMovement().add(0, -0.25, 0));
            } else {
                if (dragon.getControllingPassenger() == null || dragon.getControllingPassenger() instanceof EntityDreadQueen) {
                    if (!dragon.isBeyondHeight()) {
                        double up = dragon.isInWater() ? 0.12D : 0.08D;
                        dragon.setDeltaMovement(dragon.getDeltaMovement().add(0, up, 0));
                    } else if (!dragon.isInWater() && dragon.getDeltaMovement().y > 0.0D) {
                        // Kill leftover upward momentum at the local flight ceiling.  Without
                        // this, repeated hover/take-off cycles can still stair-step upward.
                        dragon.setDeltaMovement(dragon.getDeltaMovement().x, 0.0D, dragon.getDeltaMovement().z);
                    }
                }
                if (dragon.hoverTicks > 40) {
                    dragon.setHovering(false);
                    dragon.setFlying(true);
                    dragon.flyHovering = 0;
                    dragon.hoverTicks = 0;
                    dragon.flyTicks = 0;
                }
            }
        }
        if (dragon.isSleeping()) {
            dragon.getNavigation().stop();
        }
        if ((dragon.onGround() || dragon.isInWater()) && dragon.flyTicks != 0) {
            dragon.flyTicks = 0;
        }
        if (dragon.isAllowedToTriggerFlight() && dragon.isFlying() && dragon.doesWantToLand()) {
            dragon.setFlying(false);
            dragon.setHovering(dragon.isOverAir());
            if (!dragon.isOverAir()) {
                dragon.flyTicks = 0;
                dragon.setFlying(false);
            }
        }
        if (dragon.isFlying()) {
            dragon.flyTicks++;
        }
        if ((dragon.isHovering() || dragon.isFlying()) && dragon.isSleeping()) {
            dragon.setFlying(false);
            dragon.setHovering(false);
        }
        if (!dragon.isFlying() && !dragon.isHovering()) {
            if (dragon.isAllowedToTriggerFlight() || dragon.getY() < dragon.level().getMinBuildHeight()) {
                if (dragon.getRandom().nextInt(dragon.getFlightChancePerTick()) == 0 
                        || dragon.getY() < dragon.level().getMinBuildHeight() 
                        || (dragon.getTarget() != null && Math.abs(dragon.getTarget().getY() - dragon.getY()) > 5) 
                        || dragon.isInWater()
                        || (dragon.getTarget() != null && !dragon.attackDecision && dragon.getRandom().nextInt(15) == 0)) {
                    dragon.setHovering(true);
                    dragon.setInSittingPose(false);
                    dragon.setOrderedToSit(false);
                    dragon.flyHovering = 0;
                    dragon.hoverTicks = 0;
                    dragon.flyTicks = 0;
                }
            }
        }
        if (dragon.getTarget() != null) {
            if (!DragonUtils.isAlive(dragon.getTarget())) {
                dragon.setTarget(null);
                ticksAfterClearingTarget = dragon.level().getGameTime();
            }
        }
        if (!dragon.isAgingDisabled()) {
            dragon.setAgeInTicks(dragon.getAgeInTicks() + 1);
            if (dragon.getAgeInTicks() % 24000 == 0) {
                dragon.updateAttributes();
                dragon.growDragon(0);
            }
        }
        if (dragon.tickCount % IafConfig.dragonHungerTickRate == 0 && IafConfig.dragonHungerTickRate > 0) {
            if (dragon.getHunger() > 0) {
                dragon.setHunger(dragon.getHunger() - 1);
            }
        }
        if (!dragon.attackDecision && dragon.getDragonStage() < 2) {
            dragon.attackDecision = true;
            dragon.randomizeAttacks();
            dragon.playSound(dragon.getBabyFireSound(), 1, 1);
        }
        if (dragon.isBreathingFire()) {
            if (dragon.isSleeping() || dragon.isModelDead()) {
                dragon.setBreathingFire(false);
                dragon.randomizeAttacks();
                dragon.fireTicks = 0;
            }
            if (dragon.burningTarget == null) {
                if (dragon.fireTicks > dragon.getDragonStage() * 25 || dragon.getOwner() != null && dragon.getPassengers().contains(dragon.getOwner()) && dragon.fireStopTicks <= 0) {
                    dragon.setBreathingFire(false);
                    dragon.randomizeAttacks();
                    dragon.fireTicks = 0;
                }
            }

            if (dragon.fireStopTicks > 0 && dragon.getOwner() != null && dragon.getPassengers().contains(dragon.getOwner())) {
                dragon.fireStopTicks--;
            }
        }
        // 空中碰撞检测：仅在撞到障碍物时考虑降落，不再强制停飞
        if (dragon.isFlying() && dragon.horizontalCollision && dragon.onGround() && dragon.getControllingPassenger() == null) {
            dragon.setFlying(false);
            dragon.setHovering(false);
        }
    }
}
