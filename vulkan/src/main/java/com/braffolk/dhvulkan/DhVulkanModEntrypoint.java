package com.braffolk.dhvulkan;

import com.braffolk.dhvulkan.bridge.DhIntegration;
import com.braffolk.dhvulkan.bridge.DhVersionDetector;
import com.braffolk.dhvulkan.config.DhVulkanConfig;
import com.braffolk.dhvulkan.core.VulkanBackend;
import com.braffolk.dhvulkan.core.VulkanRenderEngine;
import com.braffolk.dhvulkan.compat.Compat;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main entrypoint for the DH-VulkanMod extension mod.
 * Detects DH version at runtime and initializes the appropriate integration.
 */
public class DhVulkanModEntrypoint implements ClientModInitializer {

    private static final Logger LOGGER = LogManager.getLogger("DH-VulkanMod");

    private static final String[] MODE_NAMES = {
            "Normal", "DH Depth", "SSAO", "Fog Alpha", "Fog Color", "Normals", "MC Depth"
    };

    /** The active integration (dh24 or api), set during init */
    private static DhIntegration activeIntegration;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[DH-VulkanMod] Extension mod initializing...");

        DhVulkanConfig config = DhVulkanConfig.get();
        LOGGER.info("[DH-VulkanMod] Config loaded. vulkanRenderMode={}", config.vulkanRenderMode);

        Compat.registerClientCommands(MODE_NAMES);

        if (!Compat.isVulkanModActive()) {
            LOGGER.warn("[DH-VulkanMod] VulkanMod is NOT detected. Extension will be inactive.");
            return;
        }

        LOGGER.info("[DH-VulkanMod] VulkanMod detected. Vulkan rendering backend will be used.");

        // Create the shared Vulkan backend
        VulkanBackend backend = new VulkanRenderEngine();

        // Detect DH version and initialize the appropriate integration
        DhVersionDetector.DhVersion dhVersion = DhVersionDetector.detect();
        LOGGER.info("[DH-VulkanMod] Detected DH version: {}", dhVersion);

        if (dhVersion == DhVersionDetector.DhVersion.DH_3_0) {
            com.braffolk.dhvulkan.api.ApiDhIntegration apiIntegration = new com.braffolk.dhvulkan.api.ApiDhIntegration();
            apiIntegration.initialize(backend);
            activeIntegration = apiIntegration;
        } else {
            LOGGER.error("[DH-VulkanMod] Distant Horizons version 3.0+ is required but not found. Extension will be inactive.");
        }

        LOGGER.info("[DH-VulkanMod] Using integration: {}", activeIntegration.getName());
    }


    /** Get the active integration (for mixins that need it) */
    public static DhIntegration getActiveIntegration() {
        return activeIntegration;
    }

    /**
     * Called from MixinLevelRenderer at renderLevel @RETURN to read MC's depth
     * buffer while the swapchain depth is in a known-good Vulkan image layout.
     * The result is cached and used by the next deferredComposite() call.
     */
    public static void readAndCacheMcDepth() {
        if (activeIntegration == null) return;
        VulkanBackend backend = activeIntegration.getBackend();
        if (backend != null) {
            backend.readAndCacheMcDepth();
        }
    }
}
