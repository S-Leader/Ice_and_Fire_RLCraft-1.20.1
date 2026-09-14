package com.github.alexthe666.iceandfire.mixins;

import com.github.alexthe666.iceandfire.block.BlockDreadBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class DreadBlockDestroySpeedMixin {

    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void iceandfire$getPlayerPlacedDreadDestroySpeed(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() instanceof BlockDreadBase
            && state.hasProperty(BlockDreadBase.PLAYER_PLACED)
            && state.getValue(BlockDreadBase.PLAYER_PLACED)) {
            cir.setReturnValue(8.0F);
        }
    }
}
