package me.cortex.voxy.client;

import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.VoxyRenderSystem;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * NeoForge 1.21.1 debug screen integration for Voxy.
 * MC 1.21.1 没有 DebugScreenEntry/DebugScreenEntryList（后续版本才引入），
 * 改为监听 CustomizeGuiOverlayEvent.DebugText 向 F3 左侧信息注入 Voxy 统计。
 */
@EventBusSubscriber(modid = "voxy", value = Dist.CLIENT)
public class VoxyDebugScreenEntry {
    @SubscribeEvent
    public static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (!VoxyCommon.isAvailable()) {
            return;
        }

        var instance = VoxyCommon.getInstance();
        if (instance == null) {
            return;
        }

        List<String> lines = new ArrayList<>();
        instance.addDebug(lines);

        var wr = Minecraft.getInstance().levelRenderer;
        if (wr instanceof IGetVoxyRenderSystem igr) {
            VoxyRenderSystem vrs = igr.getVoxyRenderSystem();
            if (vrs != null) {
                vrs.addDebugInfo(lines);
            }
        }

        if (!lines.isEmpty()) {
            event.getLeft().addAll(lines);
        }
    }
}
