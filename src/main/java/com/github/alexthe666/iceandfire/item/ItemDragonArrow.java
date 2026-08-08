package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.entity.EntityDragonArrow;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemDragonArrow extends ArrowItem {
    private final EntityDragonArrow.Type type;

    public ItemDragonArrow() {
        this(EntityDragonArrow.Type.DEFAULT);
    }

    public ItemDragonArrow(EntityDragonArrow.Type type) {
        super(new Properties());
        this.type = type;
    }

    public EntityDragonArrow.Type getType() {
        return this.type;
    }

    @Override
    public @NotNull AbstractArrow createArrow(@NotNull Level level, @NotNull ItemStack arrowStack,
                                               @NotNull LivingEntity shooter) {
        EntityDragonArrow arrow = new EntityDragonArrow(IafEntityRegistry.DRAGON_ARROW.get(), shooter, level);
        EntityDragonArrow.Type resolvedType = this.type;
        if (resolvedType == EntityDragonArrow.Type.DEFAULT
                && shooter.getUseItem().getItem() instanceof ItemDragonBow dragonBow) {
            resolvedType = dragonBow.getType();
        }
        arrow.setType(resolvedType);
        return arrow;
    }

    @Override
    public boolean isInfinite(@NotNull ItemStack arrow, @NotNull ItemStack bow, @NotNull Player player) {
        boolean infinite = super.isInfinite(arrow, bow, player);
        if (!infinite && this.type == EntityDragonArrow.Type.DEFAULT) {
            // Keep the 1.20.1 base dragonbone-arrow Infinity behavior, while the legacy
            // elemental arrows remain consumable like their 1.12.2 counterparts.
            infinite = bow.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
        }
        return infinite;
    }
}
