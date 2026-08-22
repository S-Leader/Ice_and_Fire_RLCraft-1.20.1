package com.github.alexthe666.iceandfire.block;

import org.jetbrains.annotations.NotNull;

public class BlockSeaSerpentScalesPile extends BlockCoinPile {
    private final String colorName;

    public BlockSeaSerpentScalesPile(String colorName) {
        super();
        this.colorName = colorName;
    }

    public String getColorName() {
        return this.colorName;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return "block.iceandfire.sea_serpent_scales_pile";
    }
}
