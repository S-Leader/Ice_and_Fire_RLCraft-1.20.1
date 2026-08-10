package com.github.alexthe666.iceandfire.item.blooded;

import com.github.alexthe666.iceandfire.item.IafArmorMaterial;
import net.minecraft.sounds.SoundEvents;

public class BloodedArmorMaterial {

    private static final int[] PROTECTION = {5, 9, 7, 5};

    public static final IafArmorMaterial FIRE = new IafArmorMaterial(
            "iceandfire:blooded_fire", 43, PROTECTION, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F);

    public static final IafArmorMaterial ICE = new IafArmorMaterial(
            "iceandfire:blooded_ice", 43, PROTECTION, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F);

    public static final IafArmorMaterial LIGHTNING = new IafArmorMaterial(
            "iceandfire:blooded_lightning", 43, PROTECTION, 25,
            SoundEvents.ARMOR_EQUIP_LEATHER, 2.0F);

    public static IafArmorMaterial fromElement(BloodedDragonType.DragonElement element) {
        return switch (element) {
            case FIRE -> FIRE;
            case ICE -> ICE;
            case LIGHTNING -> LIGHTNING;
        };
    }
}
