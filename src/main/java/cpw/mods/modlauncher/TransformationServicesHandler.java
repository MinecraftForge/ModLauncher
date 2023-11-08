/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

import static cpw.mods.modlauncher.LogMarkers.*;

class TransformationServicesHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private Map<String, TransformationServiceDecorator> serviceLookup;
    private final TransformStore transformStore;
    private final ModuleLayerHandler layerHandler;

    TransformationServicesHandler(TransformStore transformStore, ModuleLayerHandler layerHandler) {
        this.transformStore = transformStore;
        this.layerHandler = layerHandler;
    }

    List<ITransformationService.Resource> initializeTransformationServices(ArgumentHandler argumentHandler, Environment environment, final NameMappingServiceHandler nameMappingServiceHandler) {
        loadTransformationServices(environment);
        validateTransformationServices();
        processArguments(argumentHandler, environment);
        initialiseTransformationServices(environment);
        // force the naming to "mojang" if nothing has been populated during transformer setup
        environment.computePropertyIfAbsent(IEnvironment.Keys.NAMING.get(), a-> "mojang");
        nameMappingServiceHandler.bindNamingServices(environment.getProperty(Environment.Keys.NAMING.get()).orElse("mojang"));
        return runScanningTransformationServices(environment);
    }

    TransformingClassLoader buildTransformingClassLoader(LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, Environment environment, ModuleLayerHandler layerHandler) {
        return (TransformingClassLoader)layerHandler.build(Layer.GAME,
            (cfg, layers, loaders) -> new TransformingClassLoader(
                "TRANSFORMER", null, cfg, layers, loaders,
                transformStore, pluginHandler, environment
            )
        ).cl();
    }

    private void processArguments(ArgumentHandler argumentHandler, Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Configuring option handling for services");

        argumentHandler.processArguments(environment, this::computeArgumentsForServices, this::offerArgumentResultsToServices);
    }

    private void computeArgumentsForServices(OptionParser parser) {
        serviceLookup.values().stream()
                .map(TransformationServiceDecorator::getService)
                .forEach(service->service.arguments((a, b) -> parser.accepts(service.name() + "." + a, b)));
    }

    private void offerArgumentResultsToServices(OptionSet optionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult> resultHandler) {
        serviceLookup.values().stream()
                .map(TransformationServiceDecorator::getService)
                .forEach(service -> service.argumentValues(resultHandler.apply(service.name(), optionSet)));
    }

    void initialiseServiceTransformers() {
        LOGGER.debug(MODLAUNCHER,"Transformation services loading transformers");

        serviceLookup.values().forEach(s -> s.gatherTransformers(transformStore));
    }

    private void initialiseTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services initializing");

        serviceLookup.values().forEach(s -> s.onInitialize(environment));
    }

    private List<ITransformationService.Resource> runScanningTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services begin scanning");

        return serviceLookup.values()
                .stream()
                .map(s -> s.runScan(environment))
                .<ITransformationService.Resource>mapMulti(Iterable::forEach)
                .toList();
    }

    private void validateTransformationServices() {
        var failed = serviceLookup.values().stream()
            .filter(d -> !d.isValid())
            .map(d -> d.getService().name())
            .toList();

        if (!failed.isEmpty()) {
            var names = String.join(", ", failed);
            LOGGER.error(MODLAUNCHER,"Found {} services that failed to load : [{}]", failed.size(), names);
            throw new InvalidLauncherSetupException("Invalid Services found "+names);
        }
    }

    private void loadTransformationServices(Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Transformation services loading");
        serviceLookup.values().forEach(s -> s.onLoad(environment, serviceLookup.keySet()));
    }

    void discoverServices(final ArgumentHandler.DiscoveryData discoveryData) {
        LOGGER.debug(MODLAUNCHER, "Discovering transformation services");
        var bootLayer = layerHandler.getLayer(Layer.BOOT).orElseThrow();

        var discovery = new ArrayList<ITransformerDiscoveryService>();
        for (var itr = ServiceLoader.load(bootLayer, ITransformerDiscoveryService.class).iterator(); itr.hasNext(); ) {
            try {
                var srvc = itr.next();
                discovery.add(srvc);
                var paths = srvc.candidates(discoveryData.gameDir(), discoveryData.launchTarget());

                if (!paths.isEmpty()) {
                    LOGGER.debug(MODLAUNCHER, "Found additional transformation services from discovery service: {}", srvc.getClass().getName());
                    for (var path : paths) {
                        LOGGER.debug(MODLAUNCHER, "\t{}", path.toString());
                        layerHandler.addToLayer(Layer.SERVICE, SecureJar.from(path.paths()));
                    }
                }
            } catch (ServiceConfigurationError sce) {
                LOGGER.fatal("Encountered serious error loading transformation discoverer service, expect problems", sce);
            }
        }

        var serviceLayer = layerHandler.build(Layer.SERVICE).layer();
        for (var service : discovery)
            service.earlyInitialization(discoveryData.launchTarget(), discoveryData.arguments());

        var transformers = new HashMap<String, TransformationServiceDecorator>();
        var modlist = Launcher.INSTANCE.environment().getProperty(IEnvironment.Keys.MODLIST.get());
        for (var itr = ServiceLoader.load(serviceLayer, ITransformationService.class).iterator(); itr.hasNext(); ) {
            try {
                var srvc = itr.next();
                transformers.put(srvc.name(), new TransformationServiceDecorator(srvc));
                @SuppressWarnings("removal")
                var file = cpw.mods.modlauncher.util.ServiceLoaderUtils.fileNameFor(srvc.getClass());
                LOGGER.debug(MODLAUNCHER, "Found transformer service: {}: {}", srvc.name(), file);
                if (modlist.isPresent()) {
                    modlist.get().add(Map.of(
                        "name", srvc.name(),
                        "type", "TRANSFORMATIONSERVICE",
                        "file", file
                    ));
                }
            } catch (ServiceConfigurationError sce) {
                LOGGER.fatal(MODLAUNCHER, "Encountered serious error loading transformation service, expect problems", sce);
            }
        }
        serviceLookup = transformers;
    }

    public List<ITransformationService.Resource> triggerScanCompletion(IModuleLayerManager moduleLayerManager) {
        return serviceLookup.values().stream()
                .map(tsd->tsd.onCompleteScan(moduleLayerManager))
                .<ITransformationService.Resource>mapMulti(Iterable::forEach)
                .toList();

    }
}
