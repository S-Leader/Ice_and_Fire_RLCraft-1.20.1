package com.github.alexthe666.iceandfire.entity;

import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.item.ItemDragonBreath;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.NotNull;

public class EntityDragonBreath extends ThrowableItemProjectile {
    public EntityDragonBreath(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public EntityDragonBreath(EntityType<? extends ThrowableItemProjectile> type, LivingEntity owner, Level level,
                              ItemStack breath) {
        super(type, owner, level);
        setItem(breath);
    }

    public EntityDragonBreath(EntityType<? extends ThrowableItemProjectile> type, double x, double y, double z,
                              Level level, ItemStack breath) {
        super(type, x, y, z, level);
        setItem(breath);
    }

    @Override
    protected void onHit(@NotNull HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            ItemStack breathStack = getItem();
            if (breathStack.getItem() instanceof ItemDragonBreath breath) {
                BlockPos impactPos = BlockPos.containing(result.getLocation());
                IafDragonDestructionManager.destroyAreaBreathItem(level(), impactPos, breath.getDragonType(), this,
                        getOwner());
                level().levelEvent(2002, impactPos, getEffectColor(breath.getDragonType()));
            }
            discard();
        }
    }

    private static int getEffectColor(DragonType type) {
        if (type == DragonType.FIRE) {
            return 0xFF7636;
        } else if (type == DragonType.ICE) {
            return 0x4FADEF;
        }
        return 0xEA98FF;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return IafItemRegistry.FIRE_DRAGON_BREATH.get();
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
