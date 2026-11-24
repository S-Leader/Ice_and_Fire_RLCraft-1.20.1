package com.github.alexthe666.iceandfire.recipe;

import com.github.alexthe666.iceandfire.IafConfig;
import com.github.alexthe666.iceandfire.IceAndFire;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * Recipe/advancement condition {@code {"type": "iceandfire:dragonsteel_enabled"}}: true when Enable Dragonsteel is on.
 * The common config loads before datapacks, so dragonsteel recipes are filtered as they load.
 */
public class DragonsteelEnabledCondition implements ICondition {
    public static final ResourceLocation ID = new ResourceLocation(IceAndFire.MODID, "dragonsteel_enabled");
    public static final DragonsteelEnabledCondition INSTANCE = new DragonsteelEnabledCondition();

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return IafConfig.dragonsteelEnabled;
    }

    public static class Serializer implements IConditionSerializer<DragonsteelEnabledCondition> {
        public static final Serializer INSTANCE = new Serializer();

        @Override
        public void write(JsonObject json, DragonsteelEnabledCondition value) {
        }

        @Override
        public DragonsteelEnabledCondition read(JsonObject json) {
            return DragonsteelEnabledCondition.INSTANCE;
        }

        @Override
        public ResourceLocation getID() {
            return ID;
        }
    }
}
