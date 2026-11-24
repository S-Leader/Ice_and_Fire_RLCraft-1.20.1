package com.github.alexthe666.iceandfire.client;

import com.github.alexthe666.iceandfire.IceAndFire;
import com.github.alexthe666.iceandfire.block.DragonForgeType;
import com.github.alexthe666.iceandfire.message.MessageSyncDragonforgeMode;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Drops the server's Dragonforge Mode and Enable Dragonsteel when the client leaves, so the title screen and the next world use the local config. */
@Mod.EventBusSubscriber(modid = IceAndFire.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ClientDragonforgeModeEvents {
    private ClientDragonforgeModeEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DragonForgeType.setServerMode(null);
        com.github.alexthe666.iceandfire.item.DragonsteelContent.setServerEnabled(null);
        MessageSyncDragonforgeMode.refreshCreativeTabs();
    }
}
