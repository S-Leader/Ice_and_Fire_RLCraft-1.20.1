package com.github.alexthe666.iceandfire.compat;

import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Curios API 调用封装，仅在 Curios 已加载时使用
 * 此类会直接引用 Curios 类，必须通过 CuriosUtil 间接调用以避免 ClassNotFoundException
 */
public final class CuriosCompat {

    private CuriosCompat() {}

    /**
     * 检查实体是否在 Curios 饰品栏中装备了指定物品
     */
    public static boolean isWearingInCurios(LivingEntity entity, Item item) {
        try {
            return CuriosApi.getCuriosInventory(entity)
                    .map(inv -> inv.findFirstCurio(item).isPresent())
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查实体是否在 Curios 饰品栏中装备了眼罩
     */
    public static boolean isWearingBlindfoldInCurios(LivingEntity entity) {
        return isWearingInCurios(entity, IafItemRegistry.BLINDFOLD.get());
    }

    /**
     * 检查实体是否在 Curios 饰品栏中装备了耳塞
     */
    public static boolean isWearingEarplugsInCurios(LivingEntity entity) {
        return isWearingInCurios(entity, IafItemRegistry.EARPLUGS.get());
    }
}
