package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.IafConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dragonsteel (ingots, blocks, tools, armor and dragon armor) is optional, controlled by the Enable Dragonsteel config
 * (off by default). Its items and blocks are always registered, so worlds and other mods' recipes that name them keep
 * working; when it's off their recipes don't load (recipe condition iceandfire:dragonsteel_enabled) and they're left
 * out of the creative tab and JEI. Gold dragonsteel stays hidden from both like the rest of the gold dragon content.
 */
public final class DragonsteelContent {
    /**
     * Item tag iceandfire:dragonsteel for other mods' dragonsteel items (for example Better Survival's dragonsteel
     * weapons): Ice and Fire's JEI plugin hides everything in it while Enable Dragonsteel is off.
     */
    public static final net.minecraft.tags.TagKey<Item> TAG = net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation("iceandfire", "dragonsteel"));

    /** Every dragonsteel item registered through IafItemRegistry.registerDragonsteel. */
    public static final List<RegistryObject<? extends Item>> ITEMS = new ArrayList<>();

    // what the server sent this client (MessageSyncDragonsteel); null when not connected to a server
    private static volatile Boolean serverEnabled;

    private DragonsteelContent() {
    }

    /** Client: the server's Enable Dragonsteel setting (null on disconnect, to fall back to the local config). */
    public static void setServerEnabled(@Nullable Boolean enabled) {
        serverEnabled = enabled;
    }

    /** Enable Dragonsteel; on a client connected to a server this is the server's setting. */
    public static boolean enabled() {
        return serverEnabled != null ? serverEnabled : IafConfig.dragonsteelEnabled;
    }

    /** True for dragonsteel items and the dragonsteel blocks' items. */
    public static boolean isDragonsteel(ItemLike item) {
        var key = ForgeRegistries.ITEMS.getKey(item.asItem());
        return key != null && key.getNamespace().equals("iceandfire") && key.getPath().contains("dragonsteel");
    }

    /** Gold dragonsteel is part of the secret gold dragon content. */
    public static boolean isGold(ItemLike item) {
        var key = ForgeRegistries.ITEMS.getKey(item.asItem());
        return isDragonsteel(item) && key.getPath().contains("dragonsteel_gold");
    }

    /** Whether an item may show in the creative tab and JEI: everything except disabled or gold dragonsteel. */
    public static boolean shown(ItemLike item) {
        return !isDragonsteel(item) || (enabled() && !isGold(item));
    }
}
