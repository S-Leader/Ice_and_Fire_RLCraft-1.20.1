package com.github.alexthe666.iceandfire.item;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class IafTabRegistry {

    public static final DeferredRegister<CreativeModeTab> TAB_REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IceAndFire.MODID);

    public static final List<Supplier<? extends Block>> TAB_BLOCKS_LIST = new ArrayList<>();
    public static final List<Supplier<? extends Item>> TAB_ITEMS_LIST = new ArrayList<>();
    public static final RegistryObject<CreativeModeTab> TAB_BLOCKS = TAB_REGISTER.register("blocks", () -> CreativeModeTab.builder()
            // Set name of tab to display
            .title(Component.translatable("itemGroup." + IceAndFire.MODID + ".blocks"))
            // Set icon of creative tab
            .icon(() -> new ItemStack(IafBlockRegistry.DRAGON_SCALE_RED.get()))
            // properly order tab after spawn egg tab
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            // Add default items to tab
            .displayItems((params, output) -> {
                // forge blocks of the Dragonforge Mode that isn't in use are left out
                TAB_BLOCKS_LIST.stream().map(Supplier::get).filter(IafTabRegistry::forgeBlockInUse)
                        .filter(DragonsteelContent::shown).forEach(output::accept);
            })
            .build()
    );

    private static boolean forgeBlockInUse(Block block) {
        com.github.alexthe666.iceandfire.block.DragonForgeType fixed;
        if (block instanceof com.github.alexthe666.iceandfire.block.BlockDragonforgeBricks b) fixed = b.getFixedType();
        else if (block instanceof com.github.alexthe666.iceandfire.block.BlockDragonforgeInput i) fixed = i.getFixedType();
        else if (block instanceof com.github.alexthe666.iceandfire.block.BlockDragonforgeCore c) fixed = c.getFixedType();
        else return true;
        return com.github.alexthe666.iceandfire.block.DragonForgeType.worksInCurrentMode(fixed);
    }

    public static final RegistryObject<CreativeModeTab> TAB_ITEMS = TAB_REGISTER.register("items", () -> CreativeModeTab.builder()
            // Set name of tab to display
            .title(Component.translatable("itemGroup." + IceAndFire.MODID + ".items"))
            // Set icon of creative tab
            .icon(() -> new ItemStack(IafItemRegistry.DRAGON_SKULL_FIRE.get()))
            // properly order tab after block tab
            .withTabsBefore(TAB_BLOCKS.getKey())
            // Add default items to tab
            .displayItems((params, output) -> {
                TAB_ITEMS_LIST.forEach(block -> output.accept(block.get()));
                // dragonsteel only with Enable Dragonsteel on (gold dragonsteel never)
                DragonsteelContent.ITEMS.stream().map(Supplier::get).filter(DragonsteelContent::shown).forEach(output::accept);
            })
            .build()
    );
}
