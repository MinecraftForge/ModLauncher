/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.INameMappingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
class NameMappingServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, INameMappingService> allKnown = new HashMap<>();
    private final Map<String, INameMappingService> active = new HashMap<>();

    public NameMappingServiceHandler(ModuleLayerHandler layerHandler) {
        var services = ServiceLoader.load(layerHandler.getLayer(Layer.BOOT).orElseThrow(), INameMappingService.class);
        for (var itr = services.iterator(); itr.hasNext(); ) {
            try {
                var srvc = itr.next();
                allKnown.put(srvc.mappingName(), srvc);
            } catch (ServiceConfigurationError sce) {
                LOGGER.fatal("Encountered serious error loading naming service, expect problems", sce);
            }
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
