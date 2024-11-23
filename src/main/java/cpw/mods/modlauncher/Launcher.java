/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.jarhandling.SecureJar;
import org.apache.logging.log4j.LogManager;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.INameMappingService;
import cpw.mods.modlauncher.api.ITransformationService.Resource;
import cpw.mods.modlauncher.api.TypesafeMap;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Entry point for the ModLauncher.
 */
public class Launcher {
    public static Launcher INSTANCE;
    private static final Logger LOGGER = LogManager.getLogger();
    private static final TypesafeMap BLACKBOARD = new TypesafeMap();
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final LaunchServiceHandler launchService;
    private final LaunchPluginHandler launchPlugins;
    private final ModuleLayerHandler moduleLayerHandler;

    private Launcher() {
        INSTANCE = this;
        LOGGER.info(MODLAUNCHER,"ModLauncher {} starting: java version {} by {}; OS {} arch {} version {}", IEnvironment.class.getPackage().getImplementationVersion(),  System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
        this.moduleLayerHandler = new ModuleLayerHandler();
        this.launchService = new LaunchServiceHandler(this.moduleLayerHandler);
        this.environment = new Environment(this);
        environment.putPropertyIfAbsent(IEnvironment.Keys.MLSPEC_VERSION.get(), IEnvironment.class.getPackage().getSpecificationVersion());
        environment.putPropertyIfAbsent(IEnvironment.Keys.MLIMPL_VERSION.get(), IEnvironment.class.getPackage().getImplementationVersion());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MODLIST.get(), k -> new ArrayList<>());
        environment.putPropertyIfAbsent(IEnvironment.Keys.SECURED_JARS_ENABLED.get(), ProtectionDomainHelper.canHandleSecuredJars());
        this.transformationServicesHandler = new TransformationServicesHandler(new TransformStore(), this.moduleLayerHandler);
        this.nameMappingServiceHandler = new NameMappingServiceHandler(this.moduleLayerHandler);
        this.launchPlugins = new LaunchPluginHandler(this.moduleLayerHandler);
    }

    public static void main(String... args) {
        var props = System.getProperties();
        if (props.getProperty("java.vm.name").contains("OpenJ9")) {
            System.err.printf("""
            WARNING: OpenJ9 is detected. This is definitely unsupported and you may encounter issues and significantly worse performance.
            For support and performance reasons, we recommend installing a temurin JVM from https://adoptium.net/
            JVM information: %s %s %s
            """, props.getProperty("java.vm.vendor"), props.getProperty("java.vm.name"), props.getProperty("java.vm.version"));
        }
        LOGGER.info(MODLAUNCHER,"ModLauncher running: args {}", LaunchServiceHandler.hideAccessToken(args));
        LOGGER.info(MODLAUNCHER, "JVM identified as {} {} {}", props.getProperty("java.vm.vendor"), props.getProperty("java.vm.name"), props.getProperty("java.vm.version"));
        new Launcher().run(args);
    }

    public final TypesafeMap blackboard() {
        return BLACKBOARD;
    }

    private void run(String... args) {
        var argumentHandler = new ArgumentHandler(args);
        transformationServicesHandler.discoverServices(argumentHandler.getDiscoveryData());

        var scanResults = new EnumMap<Layer, List<Resource>>(Layer.class);

        for (var resource : transformationServicesHandler.initializeTransformationServices(argumentHandler, environment, nameMappingServiceHandler))
            scanResults.computeIfAbsent(resource.target(), k -> new ArrayList<>()).add(resource);

        for (var resource : scanResults.getOrDefault(Layer.PLUGIN, List.of())) {
            for (var jar : resource.resources())
                moduleLayerHandler.addToLayer(Layer.PLUGIN, jar);
        }
        moduleLayerHandler.build(Layer.PLUGIN);

        for (var resource : transformationServicesHandler.triggerScanCompletion(moduleLayerHandler))
            scanResults.computeIfAbsent(resource.target(), k -> new ArrayList<>()).add(resource);

        var gameContents = new ArrayList<SecureJar>();
        for (var resource : scanResults.getOrDefault(Layer.GAME, List.of())) {
            for (var jar : resource.resources()) {
                moduleLayerHandler.addToLayer(Layer.GAME, jar);
                gameContents.add(jar);
            }
        }

        this.transformationServicesHandler.initialiseServiceTransformers();
        this.launchPlugins.offerScanResultsToPlugins(gameContents);
        this.launchService.validateLaunchTarget(argumentHandler);
        var classLoaderBuilder = this.launchService.identifyTransformationTargets(argumentHandler);
        TransformingClassLoader classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, classLoaderBuilder, this.environment, this.moduleLayerHandler);
        var oldCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            this.launchService.launch(argumentHandler, this.moduleLayerHandler.getLayer(Layer.GAME).orElseThrow(), classLoader, this.launchPlugins);
        } finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }
    }

    public Environment environment() {
        return this.environment;
    }

    Optional<ILaunchPluginService> findLaunchPlugin(final String name) {
        return launchPlugins.get(name);
    }

    Optional<ILaunchHandlerService> findLaunchHandler(final String name) {
        return launchService.findLaunchHandler(name);
    }

    Optional<BiFunction<INameMappingService.Domain, String, String>> findNameMapping(final String targetMapping) {
        return nameMappingServiceHandler.findNameTranslator(targetMapping);
    }

    public Optional<IModuleLayerManager> findLayerManager() {
        return Optional.ofNullable(this.moduleLayerHandler);
    }
}
