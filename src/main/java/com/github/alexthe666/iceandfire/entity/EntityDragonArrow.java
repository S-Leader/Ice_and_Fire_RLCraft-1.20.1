package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.api.ChainLightningUtils;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import com.github.alexthe666.iceandfire.entity.props.EntityDataProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;

public class EntityDragonArrow extends AbstractArrow {
    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityDragonArrow.class, EntityDataSerializers.INT);

    public enum ArrowType {
        DEFAULT, FIRE, ICE, LIGHTNING;

        public Item getArrowItem() {
            return switch (this) {
                case FIRE -> IafItemRegistry.DRAGONBONE_ARROW_FIRE.get();
                case ICE -> IafItemRegistry.DRAGONBONE_ARROW_ICE.get();
                case LIGHTNING -> IafItemRegistry.DRAGONBONE_ARROW_LIGHTNING.get();
                default -> IafItemRegistry.DRAGONBONE_ARROW.get();
            };
        }
    }

    public EntityDragonArrow(EntityType<? extends AbstractArrow> typeIn, Level worldIn) {
        super(typeIn, worldIn);
        this.setBaseDamage(10.0D);
    }

    public EntityDragonArrow(EntityType<? extends AbstractArrow> typeIn, double x, double y, double z, Level world) {
        super(typeIn, x, y, z, world);
        this.setBaseDamage(10.0D);
    }

    public EntityDragonArrow(PlayMessages.SpawnEntity spawnEntity, Level worldIn) {
        this(IafEntityRegistry.DRAGON_ARROW.get(), worldIn);
    }

    public EntityDragonArrow(EntityType<? extends AbstractArrow> typeIn, LivingEntity shooter, Level worldIn) {
        super(typeIn, shooter, worldIn);
        this.setBaseDamage(10.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TYPE, ArrowType.DEFAULT.ordinal());
    }

    public void setType(ArrowType type) {
        this.entityData.set(TYPE, type.ordinal());
    }

    public ArrowType getArrowType() {
        int ordinal = this.entityData.get(TYPE);
        return ordinal >= 0 && ordinal < ArrowType.values().length ? ArrowType.values()[ordinal] : ArrowType.DEFAULT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && !this.inGround && this.getArrowType() != ArrowType.DEFAULT) {
            for (int i = 0; i < 2; i++) {
                double x = this.getX() - this.getDeltaMovement().x * 0.25D * i;
                double y = this.getY() - this.getDeltaMovement().y * 0.25D * i;
                double z = this.getZ() - this.getDeltaMovement().z * 0.25D * i;
                switch (this.getArrowType()) {
                    case FIRE -> this.level().addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
                    case ICE -> this.level().addParticle(ParticleTypes.SNOWFLAKE, x, y, z, 0.0D, 0.0D, 0.0D);
                    case LIGHTNING -> this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0.0D, 0.0D, 0.0D);
                    default -> { }
                }
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide() || !(result.getEntity() instanceof LivingEntity target)) {
            return;
        }

        Entity owner = this.getOwner();
        switch (this.getArrowType()) {
            case FIRE -> {
                if (target instanceof EntityIceDragon) {
                    target.hurt(this.level().damageSources().inFire(), 13.5F);
                }
                target.setSecondsOnFire(5);
            }
            case ICE -> {
                if (target instanceof EntityFireDragon) {
                    target.hurt(this.level().damageSources().drown(), 13.5F);
                }
                EntityDataProvider.getCapability(target)
                        .ifPresent(data -> data.frozenData.setFrozen(target, 200));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 2));
            }
            case LIGHTNING -> {
                if (target instanceof EntityFireDragon || target instanceof EntityIceDragon) {
                    target.hurt(this.level().damageSources().lightningBolt(), 4.0F);
                }
                if (owner instanceof LivingEntity livingOwner) {
                    ChainLightningUtils.createChainLightning(this.level(), target, livingOwner, (float) this.getBaseDamage(), false);
                }
            }
            default -> { }
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DragonArrowType", this.getArrowType().ordinal());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int ordinal = tag.getInt("DragonArrowType");
        this.setType(ordinal >= 0 && ordinal < ArrowType.values().length ? ArrowType.values()[ordinal] : ArrowType.DEFAULT);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return new ItemStack(this.getArrowType().getArrowItem());
    }
}
