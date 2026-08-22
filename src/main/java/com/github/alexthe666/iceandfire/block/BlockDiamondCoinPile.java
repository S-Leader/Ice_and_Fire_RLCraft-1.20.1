package com.github.alexthe666.iceandfire.block;

/**
 * 1.20.1 port of the legacy diamond pile block. The legacy implementation
 * selected its drop item in Java through the Varied Commodities integration;
 * in this branch the pile drops the native iceandfire:diamond_shard through
 * its block loot table instead.
 */
public class BlockDiamondCoinPile extends BlockCoinPile {
    public BlockDiamondCoinPile() {
        super();
    }
}
