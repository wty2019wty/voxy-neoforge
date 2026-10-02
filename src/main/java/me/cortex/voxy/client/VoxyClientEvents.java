package me.cortex.voxy.client;

import me.cortex.voxy.client.config.VoxyConfig;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Client event handlers for Voxy on NeoForge.
 *
 * Handles fog rendering:
 *  - 把原版渲染距离雾推到无穷远，避免 LoD 在渲染距离处出现雾墙；
 *  - 捕获当前帧真正的环境雾（水/岩浆/细雪等），供 LoD 渲染使用。
 */
@EventBusSubscriber(modid = "voxy", value = Dist.CLIENT)
public class VoxyClientEvents {

    // 当前帧的环境雾数据，由下面的 NeoForge 雾事件捕获，供 LoD 渲染阶段使用。
    // 注意：这些是渲染线程上的每帧状态，直接在渲染线程读取即可。
    public static float envFogRed = 1.0f;
    public static float envFogGreen = 1.0f;
    public static float envFogBlue = 1.0f;
    public static float envFogStart = 0.0f;
    public static float envFogEnd = 0.0f;
    public static boolean envFogActive = false;

    /**
     * 捕获原版计算出的雾颜色（含水下/岩浆/细雪等环境雾）。
     */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        envFogRed = event.getRed();
        envFogGreen = event.getGreen();
        envFogBlue = event.getBlue();
    }

    /**
     * Push terrain fog to infinity when Voxy is enabled.
     *
     * This event fires AFTER setupFog() completes but BEFORE terrain renders.
     * By setting fog distances to very large values and cancelling the event,
     * we prevent the fog wall from appearing at vanilla render distance.
     *
     * Both vanilla terrain and Voxy LODs will render without fog-based distance fading.
     * This is the same approach used by Distant Horizons.
     *
     * 由配置项 useRenderFog 控制：
     * false（默认）= 去掉原版渲染距离处的雾墙；
     * true = 保留原版渲染雾，不修改雾参数。
     *
     * 同时会在此处（覆盖之前）捕获原版雾，用于 LoD 的环境雾。
     */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (event.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            // 在可能被覆盖之前捕获原版计算出的雾距离。
            envFogStart = event.getNearPlaneDistance();
            envFogEnd = event.getFarPlaneDistance();
            // 只把真正的环境雾（水下/岩浆/细雪等）应用到 LoD，
            // 普通渲染距离雾不套到 LoD 上，否则又会形成雾墙。
            envFogActive = event.getType() != FogType.NONE && envFogEnd > envFogStart;
        }

        // Only modify terrain fog when Voxy is enabled and rendering, and the
        // user has not opted to keep the vanilla render fog.
        if (event.getMode() == FogRenderer.FogMode.FOG_TERRAIN
                && VoxyConfig.CONFIG.enabled
                && VoxyConfig.CONFIG.enableRendering
                && !VoxyConfig.CONFIG.useRenderFog) {

            // Push fog to very large values (not MAX_VALUE to avoid shader math issues)
            // This removes the fog wall at vanilla render distance
            event.setNearPlaneDistance(999999.0f);
            event.setFarPlaneDistance(9999999.0f);

            // MUST cancel for changes to take effect (per NeoForge docs)
            event.setCanceled(true);
        }
    }
}
