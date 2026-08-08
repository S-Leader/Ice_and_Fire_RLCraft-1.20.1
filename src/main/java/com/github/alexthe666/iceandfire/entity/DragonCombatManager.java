package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;


public class DragonCombatManager {
    private final EntityDragonBase dragon;

    public DragonCombatManager(EntityDragonBase dragon) {
        this.dragon = dragon;
    }


    public void updateDragonAttack() {
        Player ridingPlayer = dragon.getRidingPlayer();
        if (dragon.isPlayingAttackAnimation() && dragon.getTarget() != null && dragon.hasLineOfSight(dragon.getTarget())) {
            LivingEntity target = dragon.getTarget();
            final double dist = dragon.distanceTo(target);
            if (dist < dragon.getRenderSize() * 0.2574 * 2 + 2) {
                if (dragon.getAnimation() == EntityDragonBase.ANIMATION_BITE) {
                    if (dragon.getAnimationTick() > 15 && dragon.getAnimationTick() < 25) {
                        attackTarget(target, ridingPlayer, (int) dragon.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
                        dragon.attackDecision = dragon.getRandom().nextBoolean();
                        dragon.randomizeAttacks();
                    }
                } else if (dragon.getAnimation() == EntityDragonBase.ANIMATION_TAILWHACK) {
                    if (dragon.getAnimationTick() > 20 && dragon.getAnimationTick() < 30) {
                        attackTarget(target, ridingPlayer, (int) dragon.getAttribute(Attributes.ATTACK_DAMAGE).getValue());
                        target.knockback(dragon.getDragonStage() * 0.6F, Mth.sin(dragon.getYRot() * 0.017453292F), -Mth.cos(dragon.getYRot() * 0.017453292F));
                        dragon.attackDecision = dragon.getRandom().nextBoolean();
                        dragon.randomizeAttacks();
                    }
                } else if (dragon.getAnimation() == EntityDragonBase.ANIMATION_WINGBLAST) {
                    if ((dragon.getAnimationTick() == 17 || dragon.getAnimationTick() == 22 || dragon.getAnimationTick() == 28)) {
                        attackTarget(target, ridingPlayer, (float) dragon.getAttribute(Attributes.ATTACK_DAMAGE).getValue() / 4.0F);
                        dragon.spawnGroundEffects();
                        target.knockback(dragon.getDragonStage() * 0.6F, Mth.sin(dragon.getYRot() * 0.017453292F), -Mth.cos(dragon.getYRot() * 0.017453292F));
                        dragon.attackDecision = dragon.getRandom().nextBoolean();
                        dragon.randomizeAttacks();
                    }
                }
            }
        }
    }


    public boolean attackTarget(Entity target, Player ridingPlayer, float damage) {
        if (ridingPlayer == null)
            return target.hurt(target.level().damageSources().mobAttack(dragon), damage);
        else
            return target.hurt(target.level().damageSources().indirectMagic(dragon, ridingPlayer), damage);
    }
}
