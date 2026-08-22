package com.github.alexthe666.iceandfire.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class ItemScalePileBlock extends BlockItem {
    private final String colorTranslationKey;
    private final ChatFormatting color;

    public ItemScalePileBlock(Block block, Properties properties, String colorTranslationKey, ChatFormatting color) {
        super(block, properties);
        this.colorTranslationKey = colorTranslationKey;
        this.color = color;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable(this.colorTranslationKey).withStyle(this.color));
    }
}
