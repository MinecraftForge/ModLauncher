/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.ServiceRunner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cpw.mods.modlauncher.LogMarkers.MODLAUNCHER;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Identifies the launch target and dispatches to it
 */
final class LaunchServiceHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, ILaunchHandlerService> handlers = new HashMap<>();
    private final boolean quiet;

    public LaunchServiceHandler(ModuleLayerHandler layerHandler) {
        this(layerHandler, false);
    }
    public LaunchServiceHandler(ModuleLayerHandler layerHandler, boolean quiet) {
        var services = ServiceLoader.load(layerHandler.getLayer(Layer.BOOT).orElseThrow(), ILaunchHandlerService.class);
        for (var loader = services.iterator(); loader.hasNext(); ) {
            try {
                var srvc  = loader.next();
                handlers.put(srvc.name(), srvc);
            } catch (ServiceConfigurationError sce) {
                LOGGER.fatal("Encountered serious error loading transformation service, expect problems", sce);
            }
        }
        this.quiet = quiet;
        if (!quiet)
            LOGGER.debug(MODLAUNCHER, "Found launch services [{}]", () -> handlers.keySet().stream().sorted().collect(Collectors.joining(", ")));
    }

    public Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return Optional.ofNullable(handlers.getOrDefault(name, null));
    }

    private void launch(String target, String[] arguments, ModuleLayer gameLayer, TransformingClassLoader classLoader, LaunchPluginHandler launchPluginHandler) {
        var service = handlers.get(target);
        var paths = service.getPaths();
        launchPluginHandler.announceLaunch(classLoader, paths);
        if (!quiet)
            LOGGER.info(MODLAUNCHER, "Launching target '{}' with arguments {}", target, hideAccessToken(arguments));

        ServiceRunner runner = null;

        try {
            runner = service.launchService(arguments, gameLayer);
        } catch (AbstractMethodError e) {
            var lookup = MethodHandles.lookup();
            var type = MethodType.methodType(Callable.class, String[].class, ModuleLayer.class);
            try {
                var virtual = lookup.findVirtual(service.getClass(), "launchService", type);
                Callable<Void> callable = (Callable<Void>)virtual.invokeExact(arguments, gameLayer);
                runner = callable::call;
            } catch (Throwable t) {
                sneak(t);
            }
        }

        try {

            runner.run();
        } catch (Throwable e) {
            sneak(e);
        }
    }

    static List<String> hideAccessToken(String[] arguments) {
        var output = new ArrayList<String>();
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0 && "--accessToken".equals(arguments[i - 1]))
                output.add("**********");
            else
                output.add(arguments[i]);
        }
        return output;
    }

    public void launch(ArgumentHandler argumentHandler, ModuleLayer gameLayer, TransformingClassLoader classLoader, final LaunchPluginHandler launchPluginHandler) {
        var launchTarget = argumentHandler.getLaunchTarget();
        var args = argumentHandler.buildArgumentList();
        launch(launchTarget, args, gameLayer, classLoader, launchPluginHandler);
    }

    TransformingClassLoaderBuilder identifyTransformationTargets(final ArgumentHandler argumentHandler) {
        var builder = new TransformingClassLoaderBuilder();
        for (var path : argumentHandler.getSpecialJars())
            builder.addTransformationPath(path);
        return builder;
    }

    void validateLaunchTarget(final ArgumentHandler argumentHandler) {
        var target = argumentHandler.getLaunchTarget();
        if (!handlers.containsKey(target)) {
            LOGGER.error(MODLAUNCHER, "Cannot find launch target {}, unable to launch", target);
            throw new IllegalArgumentException("Cannot find launch target " + target + " Known: " + String.join(",", handlers.keySet()));
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable exception) throws E {
        throw (E)exception;
    }
}
