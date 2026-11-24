package com.github.alexthe666.iceandfire.compat.jei;

import com.github.alexthe666.iceandfire.block.IafBlockRegistry;
import com.github.alexthe666.iceandfire.compat.jei.firedragonforge.FireDragonForgeCategory;
import com.github.alexthe666.iceandfire.compat.jei.icedragonforge.IceDragonForgeCategory;
import com.github.alexthe666.iceandfire.compat.jei.lightningdragonforge.LightningDragonForgeCategory;
import com.github.alexthe666.iceandfire.enums.EnumDragonArmor;
import com.github.alexthe666.iceandfire.enums.EnumSkullType;
import com.github.alexthe666.iceandfire.item.blooded.BloodedDragonType;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import com.github.alexthe666.iceandfire.recipe.DragonForgeRecipe;
import com.github.alexthe666.iceandfire.recipe.IafRecipeRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.ModIds;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class IceAndFireJEIPlugin implements IModPlugin {

    public static final ResourceLocation MOD = new ResourceLocation("iceandfire:iceandfire");
    /** Items in this tag are hidden from JEI, with their crafting recipes (for the hidden gold dragon content). */
    public static final net.minecraft.tags.TagKey<Item> JEI_HIDDEN = net.minecraft.tags.ItemTags.create(new ResourceLocation("iceandfire", "jei_hidden"));
    public static final mezz.jei.api.recipe.RecipeType<DragonForgeRecipe> FIRE_DRAGON_FORGE_RECIPE_TYPE = mezz.jei.api.recipe.RecipeType.create(ModIds.MINECRAFT_ID, "firedragonforge", DragonForgeRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<DragonForgeRecipe> ICE_DRAGON_FORGE_RECIPE_TYPE = mezz.jei.api.recipe.RecipeType.create(ModIds.MINECRAFT_ID, "icedragonforge", DragonForgeRecipe.class);
    public static final mezz.jei.api.recipe.RecipeType<DragonForgeRecipe> LIGHTNING_DRAGON_FORGE_RECIPE_TYPE = mezz.jei.api.recipe.RecipeType.create(ModIds.MINECRAFT_ID, "lightningdragonforge", DragonForgeRecipe.class);


    public static final ResourceLocation FIRE_DRAGON_FORGE_ID = new ResourceLocation("iceandfire:fire_dragon_forge");
    public static final ResourceLocation ICE_DRAGON_FORGE_ID = new ResourceLocation("iceandfire:ice_dragon_forge");
    public static final ResourceLocation LIGHTNING_DRAGON_FORGE_ID = new ResourceLocation("iceandfire:lightning_dragon_forge");

    private void addDescription(IRecipeRegistration registry, ItemStack itemStack) {
        registry.addIngredientInfo(itemStack, VanillaTypes.ITEM_STACK, Component.translatable(itemStack.getDescriptionId() + ".jei_desc"));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registry) {
        List<DragonForgeRecipe> forgeRecipeList = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(IafRecipeRegistry.DRAGON_FORGE_TYPE.get());

        List<DragonForgeRecipe> fire = forgeRecipeList.stream().filter(item -> item.getDragonType().equals("fire")).collect(Collectors.toList());
        List<DragonForgeRecipe> ice = forgeRecipeList.stream().filter(item -> item.getDragonType().equals("ice")).collect(Collectors.toList());
        List<DragonForgeRecipe> lightning = forgeRecipeList.stream().filter(item -> item.getDragonType().equals("lightning")).collect(Collectors.toList());


        registry.addRecipes(FIRE_DRAGON_FORGE_RECIPE_TYPE, fire);
        registry.addRecipes(ICE_DRAGON_FORGE_RECIPE_TYPE, ice);
        registry.addRecipes(LIGHTNING_DRAGON_FORGE_RECIPE_TYPE, lightning);

        addDescription(registry, new ItemStack(IafItemRegistry.FIRE_DRAGON_BLOOD.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.ICE_DRAGON_BLOOD.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.LIGHTNING_DRAGON_BLOOD.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_RED.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_BRONZE.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_GRAY.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_GREEN.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_BLUE.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_WHITE.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_SAPPHIRE.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_SILVER.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_ELECTRIC.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_AMYTHEST.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_COPPER.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGONEGG_BLACK.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGON_SKULL_FIRE.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGON_SKULL_ICE.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.DRAGON_SKULL_LIGHTNING.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.FIRE_STEW.get()));
        addDescription(registry, new ItemStack(IafItemRegistry.FROST_STEW.get()));

        for (EnumSkullType skull : EnumSkullType.values()) {
            addDescription(registry, new ItemStack(skull.skull_item.get()));
        }

        registry.addIngredientInfo(IafItemRegistry.PATTERN_FIRE.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_ICE.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_LIGHTNING.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_FIRE_HEAD.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_ICE_HEAD.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_LIGHTNING_HEAD.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_AMPHITHERE.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_BIRD.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_EYE.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_FAE.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_FEATHER.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_GORGON.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_HIPPOCAMPUS.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_HIPPOGRYPH_HEAD.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_MERMAID.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_SEA_SERPENT.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_TROLL.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_WEEZER.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
        registry.addIngredientInfo(IafItemRegistry.PATTERN_DREAD.get().getDefaultInstance(), VanillaTypes.ITEM_STACK, Component.translatable("item.iceandfire.custom_banner.jei_desc"));
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new FireDragonForgeCategory());
        registry.addRecipeCategories(new IceDragonForgeCategory());
        registry.addRecipeCategories(new LightningDragonForgeCategory());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
        // the core that makes these recipes in the current Dragonforge Mode
        if (com.github.alexthe666.iceandfire.block.DragonForgeType.individualForges()) {
            registry.addRecipeCatalyst(new ItemStack(IafBlockRegistry.DRAGONFORGE_FIRE_CORE.get()), FIRE_DRAGON_FORGE_RECIPE_TYPE);
            registry.addRecipeCatalyst(new ItemStack(IafBlockRegistry.DRAGONFORGE_ICE_CORE.get()), ICE_DRAGON_FORGE_RECIPE_TYPE);
            registry.addRecipeCatalyst(new ItemStack(IafBlockRegistry.DRAGONFORGE_LIGHTNING_CORE.get()), LIGHTNING_DRAGON_FORGE_RECIPE_TYPE);
        } else {
            ItemStack forgeCore = new ItemStack(IafBlockRegistry.DRAGONFORGE_CORE.get());
            registry.addRecipeCatalyst(forgeCore, FIRE_DRAGON_FORGE_RECIPE_TYPE);
            registry.addRecipeCatalyst(forgeCore, ICE_DRAGON_FORGE_RECIPE_TYPE);
            registry.addRecipeCatalyst(forgeCore, LIGHTNING_DRAGON_FORGE_RECIPE_TYPE);
        }
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return MOD;
    }


    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        // The gold dragonscale and blooded gold sets are a secret: hide the items and the recipes that make them.
        // (Gold dragon forge recipes never show, since there is no gold forge category.)
        List<Item> hidden = new ArrayList<>(List.of(EnumDragonArmor.armor_gold.helmet.get(), EnumDragonArmor.armor_gold.chestplate.get(),
                EnumDragonArmor.armor_gold.leggings.get(), EnumDragonArmor.armor_gold.boots.get()));
        IafItemRegistry.BloodedArmorSet blooded = IafItemRegistry.BLOODED_ARMOR_SETS.get(BloodedDragonType.GOLD_GOLD);
        hidden.addAll(List.of(blooded.helmet().get(), blooded.chestplate().get(), blooded.leggings().get(), blooded.boots().get()));
        hidden.addAll(List.of(IafItemRegistry.DRAGONBONE_SWORD_GOLD.get(), IafItemRegistry.DRAGON_BOW_GOLD.get(), IafItemRegistry.DRAGONBONE_ARROW_GOLD.get()));
        // other mods' gold dragon items (e.g. gold dragon bone weapons) hide themselves by joining the iceandfire:jei_hidden tag
        net.minecraftforge.registries.ForgeRegistries.ITEMS.tags().getTag(JEI_HIDDEN).forEach(item -> {
            if (!hidden.contains(item)) hidden.add(item);
        });
        // dragonsteel when Enable Dragonsteel is off, and gold dragonsteel always
        net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> com.github.alexthe666.iceandfire.item.DragonsteelContent.isDragonsteel(item)
                        && !com.github.alexthe666.iceandfire.item.DragonsteelContent.shown(item) && !hidden.contains(item))
                .forEach(hidden::add);
        // other mods' dragonsteel items join the iceandfire:dragonsteel tag to be hidden with it
        if (!com.github.alexthe666.iceandfire.item.DragonsteelContent.enabled()) {
            net.minecraftforge.registries.ForgeRegistries.ITEMS.tags().getTag(com.github.alexthe666.iceandfire.item.DragonsteelContent.TAG).forEach(item -> {
                if (!hidden.contains(item)) hidden.add(item);
            });
        }
        // the gold forge blocks are part of the secret, and the forge blocks of the other Dragonforge Mode aren't used
        for (com.github.alexthe666.iceandfire.block.DragonForgeType type : com.github.alexthe666.iceandfire.block.DragonForgeType.values()) {
            if (type == com.github.alexthe666.iceandfire.block.DragonForgeType.GOLD
                    || !com.github.alexthe666.iceandfire.block.DragonForgeType.worksInCurrentMode(type.isActive() ? type : null)) {
                IafBlockRegistry.typedForgeBlocks(type).forEach(block -> hidden.add(block.get().asItem()));
            }
        }

        runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK,
                hidden.stream().map(ItemStack::new).collect(Collectors.toList()));

        if (Minecraft.getInstance().level != null) {
            List<CraftingRecipe> recipes = Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .filter(recipe -> hidden.contains(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()).getItem()))
                    .collect(Collectors.toList());
            runtime.getRecipeManager().hideRecipes(RecipeTypes.CRAFTING, recipes);
        }
    }
}
