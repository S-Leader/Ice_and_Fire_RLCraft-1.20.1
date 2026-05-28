package com.github.alexthe666.iceandfire.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * 眼罩 - 防止蛇女/鸡蛇石化，给予失明效果
 * 支持原版头盔栏和 Curios head 饰品栏
 */
public class ItemBlindfold extends ArmorItem implements ICurioItem {

    public ItemBlindfold() {
        super(IafItemRegistry.BLINDFOLD_ARMOR_MATERIAL, Type.HELMET, new Item.Properties());
    }

    @Override
    public void onArmorTick(ItemStack stack, Level world, Player player) {
        super.onArmorTick(stack, world, player);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return "iceandfire:textures/models/armor/blindfold_layer_1.png";
    }

    // ========== Curios 兼容 ==========

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // Curios 栏中也给予失明效果
        slotContext.entity().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false));
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }
}
