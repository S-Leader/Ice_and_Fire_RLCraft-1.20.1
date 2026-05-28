package com.github.alexthe666.iceandfire.compat;

import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/**
 * Curios 兼容统一入口
 * 同时检查原版头盔栏位和 Curios 饰品栏位，安全处理 Curios 未安装的情况
 */
public final class CuriosUtil {

    // 缓存 Curios 是否加载，避免每次都查询 ModList
    private static final boolean CURIOS_LOADED = ModList.get().isLoaded("curios");

    private CuriosUtil() {}

    /**
     * 检查实体是否佩戴了眼罩（头盔栏 或 Curios 栏）
     */
    public static boolean isWearingBlindfold(LivingEntity entity) {
        if (entity == null) return false;
        // 先检查原版头盔栏
        if (entity.getItemBySlot(EquipmentSlot.HEAD).getItem() == IafItemRegistry.BLINDFOLD.get()) {
            return true;
        }
        // 再检查 Curios 栏
        if (CURIOS_LOADED) {
            return CuriosCompat.isWearingBlindfoldInCurios(entity);
        }
        return false;
    }

    /**
     * 检查实体是否佩戴了耳塞（头盔栏 或 Curios 栏）
     */
    public static boolean isWearingEarplugs(LivingEntity entity) {
        if (entity == null) return false;
        // 先检查原版头盔栏
        ItemStack helmet = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() == IafItemRegistry.EARPLUGS.get()
                || !helmet.isEmpty() && helmet.getItem().getDescriptionId().contains("earmuff")) {
            return true;
        }
        // 再检查 Curios 栏
        if (CURIOS_LOADED) {
            return CuriosCompat.isWearingEarplugsInCurios(entity);
        }
        return false;
    }
}
