package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.api.event.DragonFireDamageWorldEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.PlayMessages;

import javax.annotation.Nullable;

/**
 * 金龙弹射物 — 附魔能量球
 * 不破坏方块，造成魔法伤害
 */
public class EntityDragonGoldCharge extends EntityDragonCharge {

    public EntityDragonGoldCharge(EntityType<? extends Projectile> type, Level worldIn) {
        super(type, worldIn);
    }

    public EntityDragonGoldCharge(PlayMessages.SpawnEntity spawnEntity, Level worldIn) {
        this(IafEntityRegistry.GOLD_DRAGON_CHARGE.get(), worldIn);
    }

    public EntityDragonGoldCharge(EntityType<? extends Projectile> type, Level worldIn,
            double posX, double posY, double posZ,
            double accelX, double accelY, double accelZ) {
        super(type, worldIn, posX, posY, posZ, accelX, accelY, accelZ);
    }

    public EntityDragonGoldCharge(EntityType<? extends Projectile> type, Level worldIn,
            EntityDragonBase shooter,
            double accelX, double accelY, double accelZ) {
        super(type, worldIn, shooter, accelX, accelY, accelZ);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            // enchanting-table runes trailing the charge. The vanilla ENCHANT particle starts at pos + velocity and
            // drifts back in to pos, so the "velocity" is really how far out each rune starts (up to ~1 block here)
            for (int i = 0; i < 40; i++) {
                double vx = (random.nextDouble() - 0.5) * 2.0;
                double vy = (random.nextDouble() - 0.5) * 2.0;
                double vz = (random.nextDouble() - 0.5) * 2.0;
                level().addParticle(ParticleTypes.ENCHANT,
                        this.getX() + random.nextFloat() * 0.5 - 0.25,
                        this.getY() + 0.5 + random.nextFloat() * 0.5 - 0.25,
                        this.getZ() + random.nextFloat() * 0.5 - 0.25,
                        vx, vy, vz);
            }
        }
    }

    // Magic blast where the charge lands: every living thing within max(2, stage) blocks of the impact takes
    // indirect magic damage, stage * "Dragon Charge Explosion Damage" at the centre, minus 1 per block of distance
    // (never below 1), the same size and falloff as the other charges' explosions. It never breaks blocks, so it isn't
    // tied to the griefing settings (onImpact runs whatever they are).
    @Override
    protected void onImpact(Level world, BlockPos center, EntityDragonBase dragon) {
        // other mods can cancel a dragon's world damage at a spot (claims etc.), same as the other charges' blasts
        if (MinecraftForge.EVENT_BUS.post(new DragonFireDamageWorldEvent(dragon, center.getX(), center.getY(), center.getZ()))) {
            return;
        }
        int size = Math.max(2, dragon.getDragonStage());
        float stageDamage = Math.max(1, dragon.getDragonStage()) * (float) IafConfig.dragonChargeExplosionDamage;
        Vec3 hitCenter = Vec3.atCenterOf(center);
        // the riding player gets the credit for kills, like the direct hit
        Entity cause = dragon.getRidingPlayer() != null ? dragon.getRidingPlayer() : dragon;
        DamageSource source = causeDamage(cause);
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new AABB(center).inflate(size))) {
            // never the dragon itself, its allies (owner, tamed pets), or anyone riding it
            if (target == dragon || dragon.isAlliedTo(target) || dragon.hasPassenger(target)) {
                continue;
            }
            double distance = Math.sqrt(target.distanceToSqr(hitCenter));
            // a sphere, not the search box's corners, and not through walls: needs a clear line from the impact
            if (distance > size || !clearPath(world, target)) {
                continue;
            }
            target.hurt(source, Math.max(1.0F, stageDamage - (float) distance));
        }
    }

    // true when no solid block sits between the charge and the target's middle. Starts from the charge itself, which
    // is still just outside the block it hit (the impact point is that block's centre, and a ray starting inside a
    // solid block stops there at once, so it could never see walls further on)
    private boolean clearPath(Level world, LivingEntity target) {
        Vec3 from = this.getBoundingBox().getCenter();
        Vec3 to = target.getBoundingBox().getCenter();
        BlockHitResult hit = world.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return hit.getType() == HitResult.Type.MISS;
    }

    @Override
    public DamageSource causeDamage(@Nullable Entity cause) {
        return level().damageSources().indirectMagic(this, cause);
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
