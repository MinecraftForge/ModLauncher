/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.util.ModuleExceptionEnhancer;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.securemodules.SecureModuleFinder;

public final class ModuleLayerHandler implements IModuleLayerManager {
    record LayerInfo(ModuleLayer layer, ClassLoader cl) {}
    private final EnumMap<Layer, List<SecureJar>> layers = new EnumMap<>(Layer.class);
    private final EnumMap<Layer, LayerInfo> completedLayers = new EnumMap<>(Layer.class);
    private static final Logger LOGGER = LogManager.getLogger();

    ModuleLayerHandler() {
        var classLoader = getClass().getClassLoader();
        var layer = getClass().getModule().getLayer();
        // If we have not booted into a Module layer, lets stick ourselves in one. This is here for unit tests.
        if (layer == null) {
            var cfg = ModuleLayer.boot().configuration().resolveAndBind(ModuleFinder.of(), ModuleFinder.ofSystem(), List.of());
            var cl = new SecureModuleClassLoader("BOOT", classLoader, cfg, List.of(ModuleLayer.boot()));
            layer = ModuleLayer.boot().defineModules(cfg, m -> cl);
            System.out.println("Making new classloader: " + classLoader);
            classLoader = cl;
        }
        completedLayers.put(Layer.BOOT, new LayerInfo(layer, classLoader));
    }

    @Override
    public Optional<ModuleLayer> getLayer(Layer layer) {
        return Optional.ofNullable(completedLayers.get(layer)).map(LayerInfo::layer);
    }

    void addToLayer(Layer layer, SecureJar jar) {
        if (completedLayers.containsKey(layer))
             throw new IllegalStateException("Layer already populated");
        layers.computeIfAbsent(layer, l -> new ArrayList<>()).add(jar);
    }

    /** TODO: Make package private */
    @SuppressWarnings("exports")
    @Deprecated(since = "10.1")
    public LayerInfo buildLayer(Layer layer, BiFunction<Configuration, List<ModuleLayer>, ClassLoader> classLoaderSupplier) {
        return build(layer, (cfg, layers, loaders) -> classLoaderSupplier.apply(cfg, layers));
    }

    LayerInfo build(Layer layer) {
        return build(layer, (cfg, layers, loaders) -> new SecureModuleClassLoader("LAYER " + layer.name(), null, cfg, layers, loaders));
    }

    LayerInfo build(Layer layer, ClassLoaderFactory classLoaderSupplier) {
        var jars = layers.getOrDefault(layer, List.of()).toArray(SecureJar[]::new);
        var targets = Arrays.stream(jars).map(SecureJar::name).toList();
        var parentLayers = new ArrayList<ModuleLayer>();
        var parentConfigs = new ArrayList<Configuration>();
        var parentLoaders = new ArrayList<ClassLoader>();

        for (var parent : layer.getParents()) {
            var info = completedLayers.get(parent);
            if (info == null)
                throw new IllegalStateException("Attempted to build " + layer + " before it's parent " + parent + " was populated");
            parentLayers.add(info.layer());
            parentConfigs.add(info.layer().configuration());
            parentLoaders.add(info.cl());
        }
        Configuration cfg = null;
        try {
            cfg = Configuration.resolveAndBind(SecureModuleFinder.of(jars), parentConfigs, ModuleFinder.of(), targets);
        } catch (RuntimeException e) {
            e = ModuleExceptionEnhancer.enhance(e, jars);
            LOGGER.error("Failed to create module layer " + layer.name(), e);
            throw e;
        }
        var classLoader = classLoaderSupplier.create(cfg, parentLayers, parentLoaders);

        // TODO: [ML] This should actually find the correct CL for each module, not just use the newly created one
        var newLayer = ModuleLayer.defineModules(cfg,parentLayers, module -> classLoader).layer();

        var info = new LayerInfo(newLayer, classLoader);
        completedLayers.put(layer, info);
        return info;
    }

    /** TODO: Make package private */
    @SuppressWarnings("exports")
    @Deprecated(since = "10.1")
    public LayerInfo buildLayer(Layer layer) {
        return build(layer);
    }

    /** TODO: Make package private */
    @SuppressWarnings("exports")
    @Deprecated(since = "10.1")
    public void updateLayer(Layer layer, Consumer<LayerInfo> action) {
        action.accept(completedLayers.get(layer));
    }

    interface ClassLoaderFactory {
        ClassLoader create(Configuration config, List<ModuleLayer> parentLayers, List<ClassLoader> parentLoaders);
    }
}
