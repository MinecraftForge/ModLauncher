/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService.Phase;

import static cpw.mods.modlauncher.LogMarkers.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class LaunchPluginHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, ILaunchPluginService> plugins = new HashMap<>();

    public LaunchPluginHandler(ModuleLayerHandler layerHandler) {
        var boot = layerHandler.getLayer(Layer.BOOT).orElseThrow();
        var modlist = Launcher.INSTANCE == null ? null : Launcher.INSTANCE.environment()
            .getProperty(IEnvironment.Keys.MODLIST.get())
            .orElseThrow(() -> new IllegalStateException("Invalid environment, Missing MODLIST"));

        for (var itr = ServiceLoader.load(boot, ILaunchPluginService.class).iterator(); itr.hasNext(); ) {
            try {
                var srvc = itr.next();
                @SuppressWarnings("removal")
                var file = cpw.mods.modlauncher.util.ServiceLoaderUtils.fileNameFor(srvc.getClass());
                plugins.put(srvc.name(), srvc);
                if (modlist != null) {
                    modlist.add(Map.of(
                        "name", srvc.name(),
                        "type", "PLUGINSERVICE",
                        "file", file
                    ));
                }
            } catch (ServiceConfigurationError e) {
                LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading launch plugin service. Things will not work well", e);
            }
        }

        LOGGER.debug(MODLAUNCHER, "Found launch plugins: [{}]", () -> String.join(",", plugins.keySet()));
    }

    public Optional<ILaunchPluginService> get(String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    public EnumMap<Phase, List<ILaunchPluginService>> computeLaunchPluginTransformerSet(Type className, boolean isEmpty, String reason, TransformerAuditTrail auditTrail) {
        var uniqueValues = new HashSet<ILaunchPluginService>();
        EnumMap<Phase, List<ILaunchPluginService>> phaseObjectEnumMap = new EnumMap<>(Phase.class);
        for (var plugin : plugins.values()) {
            for (var ph : plugin.handlesClass(className, isEmpty, reason)) {
                phaseObjectEnumMap.computeIfAbsent(ph, e -> new ArrayList<>()).add(plugin);
                if (uniqueValues.add(plugin)) {
                    plugin.customAuditConsumer(className.getClassName(), strings -> auditTrail.addPluginCustomAuditTrail(className.getClassName(), plugin, strings));
                }
            }
        }
        LOGGER.debug(LAUNCHPLUGIN, "LaunchPluginService {}", ()->phaseObjectEnumMap);
        return phaseObjectEnumMap;
    }

    void offerScanResultsToPlugins(List<SecureJar> scanResults) {
        for (ILaunchPluginService p : plugins.values()) {
            p.addResources(scanResults);
        }
    }

    int offerClassNodeToPlugins(Phase phase, List<ILaunchPluginService> plugins, @Nullable ClassNode node, Type className, TransformerAuditTrail auditTrail, String reason) {
        int flags = 0;
        for (var plugin : plugins) {
            LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} offering transform {}", plugin.name(), className.getClassName());
            var pluginFlags = plugin.processClassWithFlags(phase, node, className, reason);
            if (pluginFlags != ILaunchPluginService.ComputeFlags.NO_REWRITE) {
                auditTrail.addPluginAuditTrail(className.getClassName(), plugin, phase);
                LOGGER.debug(LAUNCHPLUGIN, "LauncherPluginService {} transformed {} with class compute flags {}", plugin.name(), className.getClassName(), pluginFlags);
                flags |= pluginFlags;
            }
        }
        LOGGER.debug(LAUNCHPLUGIN, "Final flags state for {} is {}", className.getClassName(), flags);
        return flags;
    }

    void announceLaunch(final TransformingClassLoader transformerLoader) {
        plugins.forEach((name, plugin) -> {
            plugin.initializeLaunch(clazzName -> transformerLoader.buildTransformedClassNodeFor(clazzName, name));
        });
    }
}
