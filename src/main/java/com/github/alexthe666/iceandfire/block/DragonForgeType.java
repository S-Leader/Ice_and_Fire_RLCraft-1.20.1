package com.github.alexthe666.iceandfire.block;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/**
 * 龙锻炉类型枚举 - 用于BlockState属性
 * NONE=未激活/无类型, FIRE=火龙, ICE=冰龙, LIGHTNING=闪电龙
 */
public enum DragonForgeType implements StringRepresentable {
    NONE("none", 0),
    FIRE("fire", 0),
    ICE("ice", 1),
    LIGHTNING("lightning", 2);

    private final String name;
    private final int legacyId;

    DragonForgeType(String name, int legacyId) {
        this.name = name;
        this.legacyId = legacyId;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }

    public int getLegacyId() {
        return legacyId;
    }

    /** 从旧的int类型ID获取枚举（用于过渡兼容） */
    public static DragonForgeType fromLegacyId(int id) {
        return switch (id) {
            case 1 -> ICE;
            case 2 -> LIGHTNING;
            default -> FIRE;
        };
    }

    /** 判断是否为有效龙类型（非NONE） */
    public boolean isActive() {
        return this != NONE;
    }

    /** 获取配方匹配用的类型ID字符串 */
    public String getTypeId() {
        return name;
    }
}
