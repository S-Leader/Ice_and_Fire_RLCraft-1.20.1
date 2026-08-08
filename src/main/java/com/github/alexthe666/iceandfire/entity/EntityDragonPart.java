package com.github.alexthe666.iceandfire.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;

public class EntityDragonPart extends EntityMutlipartPart {
    private EntityDragonBase dragon;

    public EntityDragonPart(EntityType<?> t, Level world) {
        super(t, world);
    }

    public EntityDragonPart(PlayMessages.SpawnEntity spawnEntity, Level worldIn) {
        this(IafEntityRegistry.DRAGON_MULTIPART.get(), worldIn);
    }

    public EntityDragonPart(EntityType<?> type, EntityDragonBase dragon, float radius, float angleYaw, float offsetY,
        float sizeX, float sizeY, float damageMultiplier) {
        super(type, dragon, radius, angleYaw, offsetY, sizeX, sizeY, damageMultiplier);
        this.dragon = dragon;
    }

    public EntityDragonPart(EntityDragonBase parent, float radius, float angleYaw, float offsetY, float sizeX, float sizeY, float damageMultiplier) {
        super(IafEntityRegistry.DRAGON_MULTIPART.get(), parent, radius, angleYaw, offsetY, sizeX, sizeY,
            damageMultiplier);
        this.dragon = parent;
    }

    /**
     * 1.12.2 parity: a dragon multipart must never relay damage created by its own parent
     * back into that parent. This is especially important for dragon-charge explosions: the
     * vanilla Explosion excludes the dragon entity itself, but its head/neck/wing/tail parts
     * are separate entities and would otherwise each forward the same explosion damage to the
     * dragon, producing apparently random mid-air deaths.
     */
    @Override
    public boolean hurt(DamageSource source, float damage) {
        Entity parent = this.getParent();
        if (parent instanceof EntityDragonBase dragonParent) {
            Entity direct = source.getDirectEntity();
            Entity causing = source.getEntity();

            if (direct == dragonParent || causing == dragonParent) {
                return false;
            }

            // Also reject a projectile owned by this dragon, for compatibility with damage
            // sources that preserve the projectile as the direct entity.
            if (direct instanceof Projectile projectile && projectile.getOwner() == dragonParent) {
                return false;
            }
        }
        return super.hurt(source, damage);
    }

    @Override
    public void collideWithNearbyEntities() {
    }
}
