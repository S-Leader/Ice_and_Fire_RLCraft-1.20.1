package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.datagen.tags.IafItemTags;
import com.github.alexthe666.iceandfire.entity.EntityDragonArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/** Dragon bone bow and the three legacy dragon-blood elemental bow variants. */
public class ItemDragonBow extends BowItem {
    private static final Predicate<ItemStack> DRAGON_ARROWS = stack -> stack.is(IafItemTags.DRAGON_ARROWS);
    private final EntityDragonArrow.Type type;

    public ItemDragonBow() {
        this(EntityDragonArrow.Type.DEFAULT);
    }

    public ItemDragonBow(EntityDragonArrow.Type type) {
        super(new Item.Properties().durability(type == EntityDragonArrow.Type.DEFAULT ? 584 : 700));
        this.type = type;
    }

    public EntityDragonArrow.Type getType() {
        return this.type;
    }

    @Override
    public boolean isValidRepairItem(@NotNull ItemStack toRepair, @NotNull ItemStack repair) {
        return repair.is(IafItemTags.DRAGON_BONES) || super.isValidRepairItem(toRepair, repair);
    }

    @Override
    public @NotNull Predicate<ItemStack> getAllSupportedProjectiles() {
        return DRAGON_ARROWS;
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow) {
        if (arrow instanceof EntityDragonArrow dragonArrow
                && dragonArrow.getType() == EntityDragonArrow.Type.DEFAULT
                && this.type != EntityDragonArrow.Type.DEFAULT) {
            dragonArrow.setType(this.type);
        }
        return arrow;
    }
}
