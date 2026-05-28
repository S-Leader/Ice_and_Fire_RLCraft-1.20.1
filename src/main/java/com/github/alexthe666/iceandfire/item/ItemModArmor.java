package com.github.alexthe666.iceandfire.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 通用模组盔甲（蚁穴盔甲、羊伪装、耳塞等）
 * 实现 ICurioItem 但仅对耳塞启用 Curios 功能
 */
public class ItemModArmor extends ArmorItem implements ICurioItem {

    public ItemModArmor(ArmorMaterial material, ArmorItem.Type slot) {
        super(material, slot, new Item.Properties()/*.tab(IceAndFire.TAB_ITEMS)*/);
    }

    @Override
    public @NotNull String getDescriptionId(@NotNull ItemStack stack) {
        if (this == IafItemRegistry.EARPLUGS.get()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            if (calendar.get(Calendar.MONTH) + 1 == 4 && calendar.get(Calendar.DATE) == 1) {
                return "item.iceandfire.air_pods";
            }
        }
        return super.getDescriptionId(stack);
    }

    @Override
    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (this.material == IafItemRegistry.MYRMEX_DESERT_ARMOR_MATERIAL) {
            return "iceandfire:textures/models/armor/" + (slot == EquipmentSlot.LEGS ? "myrmex_desert_layer_2" : "myrmex_desert_layer_1") + ".png";
        }
        if (this.material == IafItemRegistry.MYRMEX_JUNGLE_ARMOR_MATERIAL) {
            return "iceandfire:textures/models/armor/" + (slot == EquipmentSlot.LEGS ? "myrmex_jungle_layer_2" : "myrmex_jungle_layer_1") + ".png";
        }
        if (this.material == IafItemRegistry.SHEEP_ARMOR_MATERIAL) {
            return "iceandfire:textures/models/armor/" + (slot == EquipmentSlot.LEGS ? "sheep_disguise_layer_2" : "sheep_disguise_layer_1") + ".png";
        }
        if (this.material == IafItemRegistry.EARPLUGS_ARMOR_MATERIAL) {
            return "iceandfire:textures/models/armor/earplugs_layer_1.png";
        }
        return null;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level worldIn, @NotNull List<Component> tooltip, @NotNull TooltipFlag flagIn) {
        if (this == IafItemRegistry.EARPLUGS.get()) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date());
            if (calendar.get(Calendar.MONTH) + 1 == 4 && calendar.get(Calendar.DATE) == 1) {
                tooltip.add(Component.translatable("item.iceandfire.air_pods.desc").withStyle(ChatFormatting.GREEN));
            }
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
    }

    // ========== Curios 兼容（仅耳塞启用） ==========

    @Override
    public boolean hasCurioCapability(ItemStack stack) {
        // 仅耳塞启用 Curios 功能，其他盔甲（蚁穴、羊伪装等）不受影响
        return this == IafItemRegistry.EARPLUGS.get();
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        // 耳塞为被动防护，不需要 tick 效果
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return this == IafItemRegistry.EARPLUGS.get();
    }
}
