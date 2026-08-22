package com.github.alexthe666.iceandfire.block;

import com.github.alexthe666.iceandfire.enums.EnumDragonEgg;
import org.jetbrains.annotations.NotNull;

public class BlockDragonScalesPile extends BlockCoinPile {
    private final EnumDragonEgg type;

    public BlockDragonScalesPile(EnumDragonEgg type) {
        super();
        this.type = type;
    }

    public EnumDragonEgg getType() {
        return this.type;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return "block.iceandfire.dragonscales_pile";
    }
}
