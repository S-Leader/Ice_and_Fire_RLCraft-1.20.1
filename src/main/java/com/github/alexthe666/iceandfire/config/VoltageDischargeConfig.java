package com.github.alexthe666.iceandfire.config;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

public class VoltageDischargeConfig {

    private static final ConcurrentHashMap<String, Integer> THRESHOLDS = new ConcurrentHashMap<>();

    private static volatile int defaultThreshold = 10;

    private VoltageDischargeConfig() {
    }

    public static int getThreshold(EntityType<?> entityType) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        if (key == null) {
            return defaultThreshold;
        }
        return THRESHOLDS.getOrDefault(key.toString(), defaultThreshold);
    }

    public static void load() {
    }

    public static void parse(java.util.List<String> list, int defaultVal) {
        THRESHOLDS.clear();
        defaultThreshold = defaultVal;
        if (list == null) {
            return;
        }
        for (String line : list) {
            if (line == null || line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length == 2) {
                String key = parts[0].trim();
                try {
                    int value = Math.max(1, Math.min(10, Integer.parseInt(parts[1].trim())));
                    THRESHOLDS.put(key, value);
                } catch (NumberFormatException e) {
                    IceAndFire.LOGGER.warn("[VoltageDischargeConfig] 无法解析配置行: " + line);
                }
            }
        }
        IceAndFire.LOGGER.info("[VoltageDischargeConfig] 已从主配置解析 {} 条放电阈值配置，默认阈值: {}",
                THRESHOLDS.size(), defaultThreshold);
    }
}
