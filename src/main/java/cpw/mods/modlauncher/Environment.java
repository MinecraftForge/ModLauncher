/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Environment implementation class
 */
public final class Environment implements IEnvironment {
    private final TypesafeMap environment;
    private final Launcher launcher;

    Environment(Launcher launcher) {
        environment = new TypesafeMap(IEnvironment.class);
        this.launcher = launcher;
    }

    @Override
    public final <T> Optional<T> getProperty(TypesafeMap.Key<T> key) {
        return environment.get(key);
    }

    @Override
    public Optional<ILaunchPluginService> findLaunchPlugin(final String name) {
        return launcher.findLaunchPlugin(name);
    }

    @Override
    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return launcher.findLaunchHandler(name);
    }

    @Override
    public Optional<IModuleLayerManager> findModuleLayerManager() {
        return launcher.findLayerManager();
    }

    @Override
    public Optional<BiFunction<INameMappingService.Domain, String, String>> findNameMapping(final String targetMapping) {
        return launcher.findNameMapping(targetMapping);
    }

    @Override
    public <T> T computePropertyIfAbsent(final TypesafeMap.Key<T> key, final Function<? super TypesafeMap.Key<T>, ? extends T> valueFunction) {
        return environment.computeIfAbsent(key, valueFunction);
    }

    @Override
    public <T> T putPropertyIfAbsent(final TypesafeMap.Key<T> key, final T value) {
        return environment.putIfAbsent(key, value);
    }
}
