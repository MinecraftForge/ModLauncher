/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import net.minecraftforge.securemodules.SecureModuleFinder;
import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.NamedPath;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

@SuppressWarnings("removal")
public final class ModuleLayerHandler implements IModuleLayerManager {
    record LayerInfo(ModuleLayer layer, ModuleClassLoader cl) {}

    private record PathOrJar(NamedPath path, SecureJar jar) {
        static PathOrJar from(SecureJar jar) {
            return new PathOrJar(null, jar);
        }
        static PathOrJar from(NamedPath path) {
            return new PathOrJar(path, null);
        }

        SecureJar build() {
            return jar != null ? jar : SecureJar.from(path.paths());
        }
    }
    private final EnumMap<Layer, List<PathOrJar>> layers = new EnumMap<>(Layer.class);
    private final EnumMap<Layer, LayerInfo> completedLayers = new EnumMap<>(Layer.class);

    ModuleLayerHandler() {
        ClassLoader classLoader = getClass().getClassLoader();
        // Create a new ModuleClassLoader from the boot module layer if it doesn't exist already.
        // This allows us to launch without BootstrapLauncher.
        ModuleClassLoader cl = null;
        var layer = getClass().getModule().getLayer();
        if (classLoader instanceof ModuleClassLoader moduleCl) {
            cl = moduleCl;
        } else {
            var cfg = ModuleLayer.boot().configuration().resolveAndBind(SecureModuleFinder.of(), ModuleFinder.ofSystem(), List.of());
            var tmpcl = new ModuleClassLoader("BOOT", cfg, List.of(ModuleLayer.boot()));
            layer = ModuleLayer.boot().defineModules(cfg, m -> tmpcl);
            cl = tmpcl;
        }
        completedLayers.put(Layer.BOOT, new LayerInfo(layer, cl));
    }

    void addToLayer(final Layer layer, final SecureJar jar) {
        if (completedLayers.containsKey(layer)) throw new IllegalStateException("Layer already populated");
        layers.computeIfAbsent(layer, l->new ArrayList<>()).add(PathOrJar.from(jar));
    }

    void addToLayer(final Layer layer, final NamedPath namedPath) {
        if (completedLayers.containsKey(layer)) throw new IllegalStateException("Layer already populated");
        layers.computeIfAbsent(layer, l->new ArrayList<>()).add(PathOrJar.from(namedPath));
    }

    @SuppressWarnings("exports")
    public LayerInfo buildLayer(final Layer layer, BiFunction<Configuration, List<ModuleLayer>, ModuleClassLoader> classLoaderSupplier) {
        final var finder = layers.getOrDefault(layer, List.of()).stream()
                .map(PathOrJar::build)
                .toArray(SecureJar[]::new);
        final var targets = Arrays.stream(finder).map(SecureJar::name).toList();
        final var newConf = Configuration.resolveAndBind(SecureModuleFinder.of(finder), Arrays.stream(layer.getParent()).map(completedLayers::get).map(li->li.layer().configuration()).toList(), ModuleFinder.of(), targets);
        final var allParents = Arrays.stream(layer.getParent()).map(completedLayers::get).map(LayerInfo::layer).<ModuleLayer>mapMulti((moduleLayer, comp)-> {
            comp.accept(moduleLayer);
            moduleLayer.parents().forEach(comp);
        }).toList();
        final var classLoader = classLoaderSupplier.apply(newConf, allParents);
        final var modController = ModuleLayer.defineModules(newConf, Arrays.stream(layer.getParent()).map(completedLayers::get).map(LayerInfo::layer).toList(), f->classLoader);
        completedLayers.put(layer, new LayerInfo(modController.layer(), classLoader));
        classLoader.setFallbackClassLoader(completedLayers.get(Layer.BOOT).cl());
        return new LayerInfo(modController.layer(), classLoader);
    }
    public LayerInfo buildLayer(final Layer layer) {
        return buildLayer(layer, (cf, p) -> new ModuleClassLoader("LAYER "+layer.name(), cf, p));
    }

    @Override
    public Optional<ModuleLayer> getLayer(final Layer layer) {
        return Optional.ofNullable(completedLayers.get(layer)).map(LayerInfo::layer);
    }

    public void updateLayer(Layer layer, Consumer<LayerInfo> action) {
        action.accept(completedLayers.get(layer));
    }
}
