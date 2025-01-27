/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.harness.internal;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

public class TestTransformerService implements ITransformationService {
    public static final ThreadLocal<List<ITransformer<?>>> TRANSFORMERS = ThreadLocal.withInitial(ArrayList::new);
    public static final ThreadLocal<Map<Layer, Set<Path>>> paths = ThreadLocal.withInitial(HashMap::new);
    public static Collection<Path> getPaths(IModuleLayerManager.Layer layer) {
        if (layer == IModuleLayerManager.Layer.BOOT)
            throw new IllegalArgumentException("Cannot modify the boot layer, it would do nothing");
        return paths.get().computeIfAbsent(layer, l -> new HashSet<>());
    }

    @NotNull
    @Override
    public String name() {
        return "test.harness.transformer.service";
    }

    @Override
    public void initialize(final IEnvironment environment) {
    }

    @Override
    public void onLoad(final IEnvironment env, final Set<String> otherServices) throws IncompatibleEnvironmentException {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @NotNull
    @Override
    public List<ITransformer> transformers() {
        return (List)TRANSFORMERS.get();
    }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        var ret = new ArrayList<Resource>();
        for (var layer : new Layer[]{ Layer.PLUGIN, Layer.SERVICE }) {
            var resource = getTestJars(layer);
            if (resource != null)
                ret.add(resource);
        }
        return ret;
    }

    private static Resource getTestJars(Layer layer) {
        var paths = getPaths(layer);
        if (paths == null)
            return null;

        var jars = new ArrayList<SecureJar>();
        for (var path : paths) {
            var jar = SecureJar.from(path);
            jars.add(jar);
        }
        return new Resource(layer, jars);
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        var resource = getTestJars(Layer.GAME);
        return resource == null ? Collections.emptyList() : List.of(resource);
    }
}
