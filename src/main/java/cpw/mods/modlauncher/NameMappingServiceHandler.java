/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.INameMappingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

/**
 * Allow names to be transformed between naming domains.
 */
final class NameMappingServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, INameMappingService> allKnown;
    private final Map<String, INameMappingService> active = new HashMap<>();

    public NameMappingServiceHandler(ModuleLayerHandler layerHandler) {
        var services = ServiceLoader.load(layerHandler.getLayer(Layer.BOOT).orElseThrow(), INameMappingService.class);
        try {
            var found = new HashMap<String, INameMappingService>();
            for (var service : services) {
                found.put(service.mappingName(), service);
            }
            this.allKnown = found.isEmpty() ? Collections.emptyMap() : found;
        } catch (ServiceConfigurationError e) {
            throw new RuntimeException("Encountered serious error loading naming service", e);
        }
        LOGGER.debug(LogMarkers.MODLAUNCHER, "Found naming services : [{}]", () -> String.join(", ", allKnown.keySet()));
    }

    public Optional<BiFunction<INameMappingService.Domain, String, String>> findNameTranslator(String targetNaming) {
        var ret = active.get(targetNaming);
        if (ret == null)
            return Optional.empty();
        return Optional.of(ret.namingFunction());
    }

    public void bindNamingServices(String currentNaming) {
        LOGGER.debug(LogMarkers.MODLAUNCHER, "Current naming domain is '{}'", currentNaming);
        this.active.clear();
        for (var service : allKnown.values()) {
            if (!Objects.equals(service.understanding().getValue(), currentNaming))
                continue;
            this.active.put(service.understanding().getKey(), service);
        }
        LOGGER.debug(LogMarkers.MODLAUNCHER, "Identified name mapping providers {}", active);
    }
}
