package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.misc.IafSoundRegistry;
import net.minecraft.world.entity.player.Player;


public class DragonAnimationManager {
    private final EntityDragonBase dragon;

    public DragonAnimationManager(EntityDragonBase dragon) {
        this.dragon = dragon;
    }


    public void updateDragonClient() {
        if (!dragon.isModelDead()) {
            dragon.turn_buffer.calculateChainSwingBuffer(50, 0, 4, dragon);
            dragon.tail_buffer.calculateChainSwingBuffer(90, 20, 5F, dragon);
            if (!dragon.onGround()) {
                dragon.roll_buffer.calculateChainFlapBuffer(55, 1, 2F, 0.5F, dragon);
                dragon.pitch_buffer.calculateChainWaveBuffer(90, 10, 1F, 0.5F, dragon);
                dragon.pitch_buffer_body.calculateChainWaveBuffer(80, 10, 1, 0.5F, dragon);
            }
        }
        if (dragon.walkCycle < 39) {
            dragon.walkCycle++;
        } else {
            dragon.walkCycle = 0;
        }
        if (dragon.getAnimation() == EntityDragonBase.ANIMATION_WINGBLAST && (dragon.getAnimationTick() == 17 || dragon.getAnimationTick() == 22 || dragon.getAnimationTick() == 28)) {
            dragon.spawnGroundEffects();
        }
        dragon.legSolver.update(dragon, dragon.getRenderSize() / 3F);

        if (dragon.flightCycle == 11) {
            dragon.spawnGroundEffects();
        }
        if (dragon.isModelDead() && dragon.flightCycle != 0) {
            dragon.flightCycle = 0;
        }
    }


    public void updateDragonCommon() {
        if (dragon.isBreathingFire()) {
            dragon.fireTicks++;
            if (dragon.burnProgress < 40) {
                dragon.burnProgress++;
            }
        } else {
            dragon.burnProgress = 0;
        }

        if (dragon.flightCycle == 2) {
            if (!dragon.isDiving() && (dragon.isFlying() || dragon.isHovering())) {
                float dragonSoundVolume = IafConfig.dragonFlapNoiseDistance;
                float dragonSoundPitch = dragon.getVoicePitch();
                dragon.playSound(IafSoundRegistry.DRAGON_FLIGHT, dragonSoundVolume, dragonSoundPitch);
            }
        }
        if (dragon.flightCycle < 58) {
            dragon.flightCycle += 2;
        } else {
            dragon.flightCycle = 0;
        }

        final boolean flying = dragon.isFlying();
        if (flying) {
            if (dragon.flyProgress < 20.0F)
                dragon.flyProgress += 0.5F;
        } else {
            if (dragon.flyProgress > 0.0F)
                dragon.flyProgress -= 2F;
        }

        final boolean sleeping = dragon.isSleeping() && !dragon.isHovering() && !flying;
        if (sleeping) {
            if (dragon.sleepProgress < 20.0F)
                dragon.sleepProgress += 0.5F;
        } else {
            if (dragon.sleepProgress > 0.0F)
                dragon.sleepProgress -= 0.5F;
        }

        final boolean sitting = dragon.isOrderedToSit() && !dragon.isModelDead() && !sleeping;
        if (sitting) {
            if (dragon.sitProgress < 20.0F)
                dragon.sitProgress += 0.5F;
        } else {
            if (dragon.sitProgress > 0.0F)
                dragon.sitProgress -= 0.5F;
        }

        final boolean fireBreathing = dragon.isBreathingFire();
        dragon.prevFireBreathProgress = dragon.fireBreathProgress;
        if (fireBreathing) {
            if (dragon.fireBreathProgress < 10.0F)
                dragon.fireBreathProgress += 0.5F;
        } else {
            if (dragon.fireBreathProgress > 0.0F)
                dragon.fireBreathProgress -= 0.5F;
        }

        final boolean hovering = dragon.isHovering() || dragon.isFlying() && !dragon.attackDecision && dragon.getTarget() != null && dragon.distanceTo(dragon.getTarget()) < 17F;
        if (hovering) {
            if (dragon.hoverProgress < 20.0F)
                dragon.hoverProgress += 0.5F;
        } else {
            if (dragon.hoverProgress > 0.0F)
                dragon.hoverProgress -= 2F;
        }

        final boolean diving = dragon.isDiving();
        if (diving) {
            if (dragon.diveProgress < 10.0F)
                dragon.diveProgress += 1F;
        } else {
            if (dragon.diveProgress > 0.0F)
                dragon.diveProgress -= 2F;
        }

        final boolean tackling = dragon.isTackling() && dragon.isOverAir();
        if (tackling) {
            if (dragon.tackleProgress < 5F)
                dragon.tackleProgress += 0.5F;
        } else {
            if (dragon.tackleProgress > 0.0F)
                dragon.tackleProgress -= 1.5F;
        }

        final boolean modelDead = dragon.isModelDead();
        if (modelDead) {
            if (dragon.modelDeadProgress < 20.0F)
                dragon.modelDeadProgress += 0.5F;
        } else {
            if (dragon.modelDeadProgress > 0.0F)
                dragon.modelDeadProgress -= 0.5F;
        }

        final boolean riding = dragon.isPassenger() && dragon.getVehicle() != null && dragon.getVehicle() instanceof Player;
        if (riding) {
            if (dragon.ridingProgress < 20.0F)
                dragon.ridingProgress += 0.5F;
        } else {
            if (dragon.ridingProgress > 0.0F)
                dragon.ridingProgress -= 0.5F;
        }

        if (dragon.hasHadHornUse) {
            dragon.hasHadHornUse = false;
        }

        if (!dragon.attackDecision && dragon.getDragonStage() < 2) {
            if (dragon.level().isClientSide) {
                dragon.spawnBabyParticles();
            }
            dragon.randomizeAttacks();
        }
    }
}
