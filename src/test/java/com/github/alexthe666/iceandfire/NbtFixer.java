package com.github.alexthe666.iceandfire;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class NbtFixer {
    public static void main(String[] args) {
        try {
            File file = new File("src/main/resources/data/iceandfire/structures/mausoleum/building.nbt");
            CompoundTag root = NbtIo.readCompressed(new FileInputStream(file));
            
            ListTag palette = root.getList("palette", Tag.TAG_COMPOUND);
            for (int i = 0; i < palette.size(); i++) {
                CompoundTag state = palette.getCompound(i);
                String name = state.getString("Name");
                if (name.contains("dragonforge_fire_brick") || name.contains("dragonforge_ice_brick") || name.contains("dragonforge_lightning_brick")) {
                    state.putString("Name", "iceandfire:dragonforge_brick");
                    CompoundTag props = state.getCompound("Properties");
                    props.putString("type", "none");
                    props.putString("grill", "false");
                    state.put("Properties", props);
                    System.out.println("Replaced: " + name);
                } else if (name.contains("dragonforge_fire_input") || name.contains("dragonforge_ice_input") || name.contains("dragonforge_lightning_input")) {
                    state.putString("Name", "iceandfire:dragonforge_input");
                    CompoundTag props = state.getCompound("Properties");
                    props.putString("type", "none");
                    props.putString("active", "false");
                    state.put("Properties", props);
                    System.out.println("Replaced: " + name);
                } else if (name.contains("dragonforge_fire_core") || name.contains("dragonforge_ice_core") || name.contains("dragonforge_lightning_core")) {
                    state.putString("Name", "iceandfire:dragonforge_core");
                    CompoundTag props = state.getCompound("Properties");
                    props.putString("type", "none");
                    props.putString("active", "false");
                    state.put("Properties", props);
                    System.out.println("Replaced: " + name);
                }
            }
            
            NbtIo.writeCompressed(root, new FileOutputStream(file));
            System.out.println("Successfully updated building.nbt");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
