package com.github.alexthe666.iceandfire.mixins;

import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin 注入 BookWyrmEntity.getBreedOffspring()：
 * 两只金色书龙繁殖时有 0.5% 概率额外掉落金龙蛋。
 * Two golden Book Wyrms breeding have a 0.5% chance to also drop a gold dragon egg.
 */
@Mixin(targets = "lykrast.defiledlands.common.entity.passive.BookWyrmEntity", remap = false)
public abstract class BookWyrmMixin extends Animal {

    /** 两只金色书龙繁殖时掉落金龙蛋的概率 / chance for a golden pair's breeding to drop the egg */
    private static final double IAF$GOLD_EGG_CHANCE = 0.005;

    private BookWyrmMixin() {
        super(null, null);
    }

    @Shadow
    public abstract boolean isGolden();

    /**
     * 在繁殖产出幼崽后注入（getBreedOffspring 是模组自己的方法，不受 SRG 映射影响）。
     * Runs after a baby is made by breeding; this and the partner must both be golden.
     */
    @Inject(method = "getBreedOffspring(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/AgeableMob;)Llykrast/defiledlands/common/entity/passive/BookWyrmEntity;", at = @At("RETURN"))
    private void iaf$onBreed(ServerLevel level, AgeableMob partner, CallbackInfoReturnable<?> cir) {
        if (level.isClientSide || cir.getReturnValue() == null)
            return;
        // the partner is a BookWyrmEntity too, so it has this mixin's isGolden()
        if (!isGolden() || !((Object) partner instanceof BookWyrmMixin other) || !other.isGolden())
            return;

        if (this.random.nextDouble() < IAF$GOLD_EGG_CHANCE) {
            this.spawnAtLocation(new ItemStack(IafItemRegistry.DRAGONEGG_GOLD.get()), 0.5F);
        }
    }
}
