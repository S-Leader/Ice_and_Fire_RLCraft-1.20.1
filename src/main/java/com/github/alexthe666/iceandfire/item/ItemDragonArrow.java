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
    private final EntityDragonArrow.ArrowType type;

    public ItemDragonArrow() {
        this(EntityDragonArrow.ArrowType.DEFAULT);
    }

    public ItemDragonArrow(EntityDragonArrow.ArrowType type) {
        super(new Properties());
        this.type = type;
    }

    public EntityDragonArrow.ArrowType getArrowType() {
        return this.type;
    }

    @Override
    public @NotNull AbstractArrow createArrow(@NotNull Level level, @NotNull ItemStack arrowStack,
                                               @NotNull LivingEntity shooter) {
        EntityDragonArrow arrow = new EntityDragonArrow(IafEntityRegistry.DRAGON_ARROW.get(), shooter, level);
        EntityDragonArrow.ArrowType resolvedType = this.type;
        if (resolvedType == EntityDragonArrow.ArrowType.DEFAULT
                && shooter.getUseItem().getItem() instanceof ItemDragonBow dragonBow) {
            resolvedType = dragonBow.getArrowType();
        }
        arrow.setType(resolvedType);
        return arrow;
    }

    @Override
    public boolean isInfinite(@NotNull ItemStack arrow, @NotNull ItemStack bow, @NotNull Player player) {
        boolean infinite = super.isInfinite(arrow, bow, player);
        if (!infinite && this.type == EntityDragonArrow.ArrowType.DEFAULT) {
            // Keep the 1.20.1 base dragonbone-arrow Infinity behavior, while the legacy
            // elemental arrows remain consumable like their 1.12.2 counterparts.
            infinite = bow.getEnchantmentLevel(Enchantments.INFINITY_ARROWS) > 0;
        }
        return infinite;
    }
}
