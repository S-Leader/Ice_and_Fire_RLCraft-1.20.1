package com.github.alexthe666.iceandfire.mixin;

import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin 注入 BookWyrmEntity.aiStep()，
 * 金色书龙消化产出附魔书时有概率额外掉落金龙蛋。
 * 概率 = min(maxLevel / 60.0, 1.0)
 */
@Mixin(targets = "lykrast.defiledlands.common.entity.passive.BookWyrmEntity", remap = false)
public abstract class BookWyrmMixin extends Animal {

    private BookWyrmMixin() {
        super(null, null);
    }

    @Shadow
    public abstract boolean isGolden();

    @Shadow
    public abstract int getMaxLevel();

    /**
     * 在 aiStep 中第二次 playDigestEffect 调用处注入（ordinal=1 = 成功产出时）。
     * playDigestEffect 是模组自定义方法，不受 SRG 映射影响。
     */
    @Inject(method = "m_8107_", at = @At(value = "INVOKE", target = "Llykrast/defiledlands/common/entity/passive/BookWyrmEntity;playDigestEffect(Z)V", ordinal = 1, shift = At.Shift.AFTER))
    private void iaf$onDigestSuccess(CallbackInfo ci) {
        if (this.level().isClientSide)
            return;
        if (!isGolden())
            return;

        double chance = Math.min(getMaxLevel() / 60.0, 1.0);

        if (this.random.nextDouble() < chance) {
            this.spawnAtLocation(new ItemStack(IafItemRegistry.DRAGONEGG_GOLD.get()), 0.5F);
        }
    }
}
