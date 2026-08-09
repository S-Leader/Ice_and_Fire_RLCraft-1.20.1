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

        // Clear dead/stale combat targets before deciding whether the dragon should return home.
        // The old sleep behavior gives an active combat target priority over sleeping.  The
        // previous port set lookingForRoostAIFlag even while a live target was still assigned,
        // creating two mutually-exclusive intents at the same time: the roost goal refused to
        // start because getTarget() was non-null, while the air-combat goal kept rewriting
        // airTarget.  That is the main cause of the night-time hover loop.
        if (dragon.getTarget() != null && !DragonUtils.isAlive(dragon.getTarget())) {
            dragon.setTarget(null);
            ticksAfterClearingTarget = dragon.level().getGameTime();
        }
        final boolean hasLiveCombatTarget = dragon.getTarget() != null && dragon.getTarget().isAlive();
        final boolean wasRecentlyHurt = dragon.getLastHurtByMob() != null
                && dragon.tickCount - dragon.getLastHurtByMobTimestamp() < 100;
        final boolean canReturnToRoost = IafConfig.doDragonsSleep
                && !dragon.isSleeping()
                && !dragon.isTimeToWake()
                && dragon.getPassengers().isEmpty()
                && dragon.getCommand() != 2
                && dragon.getCommand() != 1
                && !hasLiveCombatTarget
                && !wasRecentlyHurt;

        if (canReturnToRoost
                && dragon.hasHomePosition
                && dragon.getRestrictCenter() != null
                && DragonUtils.isInHomeDimension(dragon)
                && !isAtUsableRoostArea()) {
            // Do not require the dragon to stand on the exact saved home block.  A roost can be
            // excavated after generation; a dragon that successfully lands on nearby intact
            // ground should still count as having returned home and be allowed to sleep.
            dragon.lookingForRoostAIFlag = true;
        } else {
            // Combat and return-to-roost are deliberately exclusive.  If a target becomes valid
            // while returning home, combat wins immediately and the roost goal releases MOVE.
            dragon.lookingForRoostAIFlag = false;
            if (!hasLiveCombatTarget
                    && IafConfig.doDragonsSleep
                    && !dragon.isSleeping()
                    && !dragon.isTimeToWake()
                    && dragon.getPassengers().isEmpty()
                    && dragon.getCommand() != 2
                    && dragon.getCommand() != 1
                    && (dragon.level().getGameTime() - ticksAfterClearingTarget >= 20)
                    && !dragon.isInWater()
                    && dragon.onGround()
                    && !dragon.isFlying()
                    && !dragon.isHovering()) {
                dragon.setInSittingPose(true);
            }
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

                // 1.12.2 used BlockBreakExplosion here.  That implementation explicitly
                // excluded the exploder from entity damage.  A plain 1.20.1 Explosion
                // can feed its damage back into the dragon, and at stage 5 this is an
                // 18-block-radius explosion -- easily enough to kill its own caster.
                // Preserve the old behavior by shielding only the source dragon while
                // this one explosion is being resolved.
                boolean wasInvulnerable = dragon.isInvulnerable();
                Vec3 motionBeforeExplosion = dragon.getDeltaMovement();
                dragon.setInvulnerable(true);
                try {
                    Explosion explosion = new Explosion(dragon.level(), dragon, dragon.getX(), dragon.getY(), dragon.getZ(),
                            (4F * dragon.getDragonStage()) - 2F, false, mode);
                    explosion.explode();
                    explosion.finalizeExplosion(true);
                } finally {
                    dragon.setInvulnerable(wasInvulnerable);
                    // The old BlockBreakExplosion did not knock the exploder away either.
                    dragon.setDeltaMovement(motionBeforeExplosion);
                }
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
        if (dragon.getControllingPassenger() == null && !dragon.lookingForRoostAIFlag && !dragon.isOverAir() && dragon.doesWantToLand() && (dragon.isFlying() || dragon.isHovering()) && !dragon.isInWater()) {
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
            // Slowly land the hovering dragon.  Landing is a real descending state: do not
            // allow residual take-off/upward velocity to cancel the descent.
            final boolean roostLanding = isRoostLandingPhase();
            if (roostLanding && dragon.onGround()) {
                dragon.setHovering(false);
                dragon.setFlying(false);
                dragon.setDeltaMovement(dragon.getDeltaMovement().x * 0.5D, 0.0D, dragon.getDeltaMovement().z * 0.5D);
                dragon.hoverTicks = 0;
                dragon.flyTicks = 0;
            } else if (dragon.getControllingPassenger() == null
                    && ((!dragon.lookingForRoostAIFlag && dragon.doesWantToLand()) || roostLanding)
                    && !dragon.onGround() && !dragon.isInWater()) {
                Vec3 motion = dragon.getDeltaMovement();
                double landingY = Math.min(motion.y - 0.25D, -0.12D);
                dragon.setDeltaMovement(motion.x * 0.9D, landingY, motion.z * 0.9D);
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
        // 1.12.2 landing transition.  Do NOT gate this with isAllowedToTriggerFlight():
        // that method deliberately requires onGround()/water because it answers whether a
        // grounded dragon may TAKE OFF.  Using it here made this branch impossible for an
        // airborne dragon, leaving it stuck in Flying/Hovering forever.
        if (dragon.getControllingPassenger() == null && !dragon.lookingForRoostAIFlag && dragon.isFlying() && dragon.doesWantToLand() && !dragon.isInWater()) {
            dragon.airTarget = null;
            dragon.setFlying(false);
            dragon.setHovering(!dragon.onGround());
            if (dragon.onGround()) {
                dragon.hoverTicks = 0;
                dragon.flyTicks = 0;
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
    /**
     * Return-to-roost has its own landing phase and must not depend on flyTicks/doesWantToLand().
     * The latter is a general flight-fatigue decision and used to be reset every tick by the
     * roost goal, which made landing at the nest impossible.
     */
    private boolean isRoostLandingPhase() {
        if (!dragon.lookingForRoostAIFlag
                || dragon.getRestrictCenter() == null
                || (dragon.getTarget() != null && dragon.getTarget().isAlive())
                || !DragonUtils.isInHomeDimension(dragon)) {
            return false;
        }
        // DragonAIReturnToRoost only switches to Hovering=true/Flying=false after it has found
        // and reached a validated landingTarget.  Do not infer landing from distance to the saved
        // home block: that block may now be a hole.
        return dragon.isHovering() && !dragon.isFlying();
    }

    private boolean isAtUsableRoostArea() {
        if (!dragon.onGround() || dragon.getRestrictCenter() == null) {
            return false;
        }
        final double dx = dragon.getX() - (dragon.getRestrictCenter().getX() + 0.5D);
        final double dz = dragon.getZ() - (dragon.getRestrictCenter().getZ() + 0.5D);
        final double acceptanceRadius = Mth.clamp(dragon.getBbWidth() * 3.0D, 12.0D, 32.0D);
        return dx * dx + dz * dz <= acceptanceRadius * acceptanceRadius;
    }

}
