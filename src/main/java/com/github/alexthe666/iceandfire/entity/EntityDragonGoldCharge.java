package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 金龙弹射物 — 附魔能量球
 * 不破坏方块，造成魔法伤害
 */
public class EntityDragonGoldCharge extends EntityDragonCharge {

    public EntityDragonGoldCharge(EntityType<? extends Fireball> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityDragonGoldCharge(net.minecraftforge.network.PlayMessages.SpawnEntity spawnEntity, Level worldIn) {
        this(IafEntityRegistry.GOLD_DRAGON_CHARGE.get(), worldIn);
    }

    public EntityDragonGoldCharge(EntityType<? extends Fireball> type, Level worldIn,
            double posX, double posY, double posZ,
            double accelX, double accelY, double accelZ) {
        super(type, worldIn, posX, posY, posZ, accelX, accelY, accelZ);
    }

    public EntityDragonGoldCharge(EntityType<? extends Fireball> type, Level worldIn,
            EntityDragonBase shooter,
            double accelX, double accelY, double accelZ) {
        super(type, worldIn, shooter, accelX, accelY, accelZ);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            for (int i = 0; i < 4; i++) {
                double vx = (random.nextDouble() - 0.5) * 0.3;
                double vy = (random.nextDouble() - 0.5) * 0.3;
                double vz = (random.nextDouble() - 0.5) * 0.3;
                level().addParticle(ParticleTypes.ENCHANT,
                        this.getX() + random.nextFloat() * 0.5 - 0.25,
                        this.getY() + 0.5 + random.nextFloat() * 0.5 - 0.25,
                        this.getZ() + random.nextFloat() * 0.5 - 0.25,
                        vx, vy, vz);
            }
        }
    }

    @Override
    public DamageSource causeDamage(@Nullable Entity cause) {
        return level().damageSources().magic();
    }

    @Override
    public void destroyArea(Level world, BlockPos center, EntityDragonBase destroyer) {
    }

    @Override
    public float getDamage() {
        return (float) IafConfig.dragonAttackDamage;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }
}
