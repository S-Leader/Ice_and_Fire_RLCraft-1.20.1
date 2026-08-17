package com.github.alexthe666.iceandfire.item.blooded;

import com.github.alexthe666.iceandfire.item.IProtectAgainstDragonItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class ItemBloodedArmor extends ArmorItem implements IProtectAgainstDragonItem {

    private final BloodedDragonType dragonType;

    public ItemBloodedArmor(BloodedDragonType dragonType, Type slot) {
        super(BloodedArmorMaterial.fromElement(dragonType.getElement()), slot,
                new Properties());
        this.dragonType = dragonType;
    }

    public BloodedDragonType getDragonType() {
        return dragonType;
    }

    @Override
    public @NotNull String getDescriptionId() {
        return switch (this.type) {
            case HELMET -> "item.iceandfire.blooded_helmet";
            case CHESTPLATE -> "item.iceandfire.blooded_chestplate";
            case LEGGINGS -> "item.iceandfire.blooded_leggings";
            case BOOTS -> "item.iceandfire.blooded_boots";
        };
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new ClientExtensions());
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        String color = dragonType.getColorName();
        String suffix = slot == EquipmentSlot.LEGS ? "_legs.png" : ".png";
        return "iceandfire:textures/models/armor/blooded_" + color + suffix;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        ChatFormatting elementColor = switch (dragonType.getElement()) {
            case FIRE -> ChatFormatting.DARK_RED;
            case ICE -> ChatFormatting.AQUA;
            case LIGHTNING -> ChatFormatting.LIGHT_PURPLE;
        };

        tooltip.add(Component.translatable("dragon." + dragonType.getColorName())
                .withStyle(dragonType.getColor()));
        tooltip.add(Component.translatable("item.blooded_armor.desc")
                .withStyle(elementColor));
        tooltip.add(Component.empty());

        int wornCount = countWornPiecesClient(level);
        boolean fullSet = wornCount >= 4;

        tooltip.add(Component.translatable("item.iceandfire.blooded_set.title")
                .withStyle(ChatFormatting.GRAY));

        ChatFormatting descColor = fullSet ? elementColor : ChatFormatting.GRAY;
        String effectKey = switch (dragonType.getElement()) {
            case FIRE -> "item.iceandfire.blooded_set.fire";
            case ICE -> "item.iceandfire.blooded_set.ice";
            case LIGHTNING -> "item.iceandfire.blooded_set.lightning";
        };
        tooltip.add(
                Component.literal(wornCount + "/4: ").append(Component.translatable(effectKey)).withStyle(descColor));
    }

    private int countWornPiecesClient(@Nullable Level level) {
        if (level == null || !level.isClientSide()) return 0;
        BloodedDragonType.DragonElement myElement = dragonType.getElement();
        return DistExecutor.unsafeCallWhenOn(Dist.CLIENT,
                () -> () -> ClientBloodedHelper.countWornPieces(myElement));
    }

    private static class ClientBloodedHelper {
        static int countWornPieces(BloodedDragonType.DragonElement element) {
            try {
                net.minecraft.world.entity.player.Player player =
                        net.minecraft.client.Minecraft.getInstance().player;
                if (player == null) return 0;
                int count = 0;
                for (EquipmentSlot slot : new EquipmentSlot[]{
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    ItemStack equipped = player.getItemBySlot(slot);
                    if (equipped.getItem() instanceof ItemBloodedArmor armor
                            && armor.getDragonType().getElement() == element) {
                        count++;
                    }
                }
                return count;
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    /**
     * Checks if the given entity is wearing a full set of blooded armor of the specified element. Safe for server-side.
     */
    public static boolean hasFullArmorSet(LivingEntity entity, BloodedDragonType.DragonElement element) {
        if (entity == null) return false;
        int count = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.getItem() instanceof ItemBloodedArmor armor
                    && armor.getDragonType().getElement() == element) {
                count++;
            }
        }
        return count == 4;
    }

    /**
     * Client armor model extensions. Must NOT store any state from constructor parameters — Forge/ModernFix
     * may bypass initializeClient and create instances via reflection. All data must come from method args.
     */
    private static class ClientExtensions
            implements net.minecraftforge.client.extensions.common.IClientItemExtensions {

        @Override
        public @NotNull net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                LivingEntity living, ItemStack stack, EquipmentSlot slot,
                net.minecraft.client.model.HumanoidModel<?> defaultModel) {
            if (!(stack.getItem() instanceof ItemBloodedArmor armor)) {
                return defaultModel;
            }
            boolean inner = slot == EquipmentSlot.LEGS || slot == EquipmentSlot.HEAD;
            return switch (armor.getDragonType().getElement()) {
                case FIRE -> new com.github.alexthe666.iceandfire.client.model.armor.ModelBloodedFireArmor(inner);
                case ICE -> new com.github.alexthe666.iceandfire.client.model.armor.ModelBloodedIceArmor(inner);
                case LIGHTNING ->
                        new com.github.alexthe666.iceandfire.client.model.armor.ModelBloodedLightningArmor(inner);
            };
        }
    }
}
