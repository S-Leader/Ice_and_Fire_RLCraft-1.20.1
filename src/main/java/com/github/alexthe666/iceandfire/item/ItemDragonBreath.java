package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.entity.DragonType;
import com.github.alexthe666.iceandfire.entity.EntityDragonBreath;
import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class ItemDragonBreath extends Item {
    private final DragonType type;

    public ItemDragonBreath(DragonType type) {
        super(new Item.Properties().stacksTo(8));
        this.type = type;
    }

    public DragonType getDragonType() {
        return type;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack heldStack = player.getItemInHand(hand);
        ItemStack thrownStack = heldStack.copy();
        thrownStack.setCount(1);

        if (!player.isCreative()) {
            heldStack.shrink(1);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW,
                SoundSource.PLAYERS, 0.5F, 0.4F / (level.random.nextFloat() * 0.4F + 0.8F));

        if (!level.isClientSide) {
            EntityDragonBreath breath = new EntityDragonBreath(IafEntityRegistry.DRAGON_BREATH.get(), player,
                    level, thrownStack);
            breath.shootFromRotation(player, player.getXRot(), player.getYRot(), -20.0F, 0.5F, 1.0F);
            level.addFreshEntity(breath);
        }

        return new InteractionResultHolder<>(InteractionResult.SUCCESS, heldStack);
    }
}
