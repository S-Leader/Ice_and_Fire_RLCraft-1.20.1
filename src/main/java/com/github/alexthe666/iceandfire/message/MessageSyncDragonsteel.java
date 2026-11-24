package com.github.alexthe666.iceandfire.message;

import com.github.alexthe666.iceandfire.item.DragonsteelContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Server to client: the server's Enable Dragonsteel setting, sent with the Dragonforge Mode (OnDatapackSyncEvent), so
 * the creative tab and JEI show dragonsteel only when the server has it on. Dropped when the client disconnects.
 */
public class MessageSyncDragonsteel {

    public final boolean enabled;

    public MessageSyncDragonsteel(boolean enabled) {
        this.enabled = enabled;
    }

    public static MessageSyncDragonsteel read(FriendlyByteBuf buf) {
        return new MessageSyncDragonsteel(buf.readBoolean());
    }

    public static void write(MessageSyncDragonsteel message, FriendlyByteBuf buf) {
        buf.writeBoolean(message.enabled);
    }

    public static void handle(MessageSyncDragonsteel message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DragonsteelContent.setServerEnabled(message.enabled);
            MessageSyncDragonforgeMode.refreshCreativeTabs();
        });
        context.setPacketHandled(true);
    }
}
