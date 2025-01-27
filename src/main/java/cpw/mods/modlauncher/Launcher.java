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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.jar.Manifest;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Entry point for the ModLauncher.
 */
public class Launcher {
    public static Launcher INSTANCE;
    private static final Logger LOGGER = LogManager.getLogger();
    private final TypesafeMap blackboard;
    private final TransformationServicesHandler transformationServicesHandler;
    private final Environment environment;
    private final TransformStore transformStore;
    private final NameMappingServiceHandler nameMappingServiceHandler;
    private final ArgumentHandler argumentHandler;
    private final LaunchServiceHandler launchService;
    private final LaunchPluginHandler launchPlugins;
    private final ModuleLayerHandler moduleLayerHandler;
    private TransformingClassLoader classLoader;

    private Launcher(/* this is reflectively called by test as true to shut up some logging*/boolean quiet) {
        INSTANCE = this;
        var version = getVersionInfo();
        if (!quiet)
            LOGGER.info(MODLAUNCHER, "ModLauncher {} starting: java version {} by {}; OS {} arch {} version {}", version.implementation(),  System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("os.version"));
        this.moduleLayerHandler = new ModuleLayerHandler();
        this.launchService = new LaunchServiceHandler(this.moduleLayerHandler, quiet);
        this.blackboard = new TypesafeMap();
        this.environment = new Environment(this);
        environment.putPropertyIfAbsent(IEnvironment.Keys.MLSPEC_VERSION.get(), version.implementation());
        environment.putPropertyIfAbsent(IEnvironment.Keys.MLIMPL_VERSION.get(), version.specification());
        environment.computePropertyIfAbsent(IEnvironment.Keys.MODLIST.get(), k -> new ArrayList<>());
        environment.putPropertyIfAbsent(IEnvironment.Keys.SECURED_JARS_ENABLED.get(), ProtectionDomainHelper.canHandleSecuredJars());
        this.transformStore = new TransformStore();
        this.transformationServicesHandler = new TransformationServicesHandler(this.transformStore, this.moduleLayerHandler);
        this.argumentHandler = new ArgumentHandler();
        this.nameMappingServiceHandler = new NameMappingServiceHandler(this.moduleLayerHandler);
        this.launchPlugins = new LaunchPluginHandler(this.moduleLayerHandler);
    }

    private record VersionInfo(String implementation, String specification) {}
    private static VersionInfo getVersionInfo() {
        var pkg = IEnvironment.class.getPackage();
        var impl = pkg.getImplementationVersion();
        var spec = pkg.getSpecificationVersion();

        // If we're in a SecureModule ClassLoader we can trust the package versions
        if (impl != null && spec != null)
            return new VersionInfo(impl, spec);

        // If we're in the normal AppClassloader we have to read it ourselves as it doesn't attach version into to packages in modules
        var module = IEnvironment.class.getModule();
        try (var is = module.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            if (is != null) {
                var manifst = new Manifest(is);
                var section = manifst.getAttributes(pkg.getName().replace('.', '/') + '/');
                if (section != null) {
                    impl = section.getValue("Implementation-Version");
                    spec = section.getValue("Specification-Version");
                    if (impl != null && spec != null)
                        return new VersionInfo(impl, spec);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // If we can't find it in the manifest, we're in some weird dev workspace that doesn't run our gradle scripts
        return new VersionInfo("0.dev", "0.dev");
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
        new Launcher(false).run(args);
    }

    public final TypesafeMap blackboard() {
        return blackboard;
    }

    private void run(String... args) {
        var discoveryData = argumentHandler.setArgs(args);
        transformationServicesHandler.discoverServices(discoveryData);

        var scanResults = new HashMap<Layer, List<Resource>>();

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
        this.launchService.validateLaunchTarget(this.argumentHandler);
        var classLoaderBuilder = this.launchService.identifyTransformationTargets(this.argumentHandler);
        this.classLoader = this.transformationServicesHandler.buildTransformingClassLoader(this.launchPlugins, classLoaderBuilder, this.environment, this.moduleLayerHandler);
        var oldCL = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(this.classLoader);
            this.launchService.launch(this.argumentHandler, this.moduleLayerHandler.getLayer(Layer.GAME).orElseThrow(), this.classLoader, this.launchPlugins);
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
