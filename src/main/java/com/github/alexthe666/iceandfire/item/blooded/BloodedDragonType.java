package com.github.alexthe666.iceandfire.item.blooded;

import net.minecraft.ChatFormatting;

public enum BloodedDragonType {
    FIRE_RED("fire_red", "red", "flamed", DragonElement.FIRE, ChatFormatting.DARK_RED),
    FIRE_BRONZE("fire_bronze", "bronze", "flamed", DragonElement.FIRE, ChatFormatting.GOLD),
    FIRE_GREEN("fire_green", "green", "flamed", DragonElement.FIRE, ChatFormatting.DARK_GREEN),
    FIRE_GRAY("fire_gray", "gray", "flamed", DragonElement.FIRE, ChatFormatting.GRAY),

    ICE_BLUE("ice_blue", "blue", "iced", DragonElement.ICE, ChatFormatting.AQUA),
    ICE_WHITE("ice_white", "white", "iced", DragonElement.ICE, ChatFormatting.WHITE),
    ICE_SAPPHIRE("ice_sapphire", "sapphire", "iced", DragonElement.ICE, ChatFormatting.BLUE),
    ICE_SILVER("ice_silver", "silver", "iced", DragonElement.ICE, ChatFormatting.DARK_GRAY),

    LIGHTNING_ELECTRIC("lightning_electric", "electric", "shocked", DragonElement.LIGHTNING, ChatFormatting.DARK_BLUE),
    LIGHTNING_AMETHYST("lightning_amethyst", "amythest", "shocked", DragonElement.LIGHTNING,
            ChatFormatting.LIGHT_PURPLE),
    LIGHTNING_COPPER("lightning_copper", "copper", "shocked", DragonElement.LIGHTNING, ChatFormatting.GOLD),
    LIGHTNING_BLACK("lightning_black", "black", "shocked", DragonElement.LIGHTNING, ChatFormatting.DARK_GRAY);

    private final String id;
    private final String colorName;
    private final String texturePrefix;
    private final DragonElement element;
    private final ChatFormatting color;

    BloodedDragonType(String id, String colorName, String texturePrefix,
                      DragonElement element, ChatFormatting color) {
        this.id = id;
        this.colorName = colorName;
        this.texturePrefix = texturePrefix;
        this.element = element;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getColorName() {
        return colorName;
    }

    public String getTexturePrefix() {
        return texturePrefix;
    }

    public DragonElement getElement() {
        return element;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public enum DragonElement {
        FIRE("fire_dragon_blood"),
        ICE("ice_dragon_blood"),
        LIGHTNING("lightning_dragon_blood");

        private final String repairItemId;

        DragonElement(String repairItemId) {
            this.repairItemId = repairItemId;
        }

        public String getRepairItemId() {
            return repairItemId;
        }
    }
}
