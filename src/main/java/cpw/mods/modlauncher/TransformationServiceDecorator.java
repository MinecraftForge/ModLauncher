/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.TransformTargetLabel.LabelType;
import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Decorates {@link cpw.mods.modlauncher.api.ITransformationService} to track state and other runtime metadata.
 */
public class TransformationServiceDecorator {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ITransformationService service;
    private boolean isValid;

    TransformationServiceDecorator(ITransformationService service) {
        this.service = service;
    }

    void onLoad(IEnvironment env, Set<String> otherServices) {
        try {
            LOGGER.debug(MODLAUNCHER,"Loading service {}", this.service::name);
            this.service.onLoad(env, otherServices);
            this.isValid = true;
            LOGGER.debug(MODLAUNCHER,"Loaded service {}", this.service::name);
        } catch (IncompatibleEnvironmentException e) {
            LOGGER.error(MODLAUNCHER,"Service failed to load {}", this.service.name(), e);
            this.isValid = false;
        }
    }

    boolean isValid() {
        return isValid;
    }

    void onInitialize(IEnvironment environment) {
        LOGGER.debug(MODLAUNCHER,"Initializing transformation service {}", this.service::name);
        this.service.initialize(environment);
        LOGGER.debug(MODLAUNCHER,"Initialized transformation service {}", this.service::name);
    }

    public void gatherTransformers(TransformStore transformStore) {
        LOGGER.debug(MODLAUNCHER, "Initializing transformers for transformation service {}", this.service::name);
        var transformers = this.service.transformers();
        Objects.requireNonNull(transformers, "The transformers list should not be null");

        for (ITransformer<?> transformer : transformers) {
            Type type = null;
            var genericInterfaces = transformer.getClass().getGenericInterfaces();
            for (var typ : genericInterfaces) {
                if (typ instanceof ParameterizedType pt && pt.getRawType().equals(ITransformer.class)) {
                    type = pt.getActualTypeArguments()[0];
                    break;
                }
            }

            if (type == null) {
                LOGGER.error(MODLAUNCHER, "Invalid Transformer, could not determine generic type {}", transformer.getClass().getSimpleName());
                throw new IllegalArgumentException("Invalid Transformer, could not determine generic type " + transformer.getClass().getSimpleName());
            }

            LabelType seen = null;
            for (var target : transformer.targets()) {
                var label = new TransformTargetLabel(target);
                if (
                    (seen != null && label.getLabelType() != seen) ||
                    !label.getLabelType().getNodeType().getName().equals(type.getTypeName())
                ) {
                    LOGGER.info(MODLAUNCHER, "Invalid target {} for transformer {}", label.getLabelType(), transformer);
                    throw new IllegalArgumentException("Invalid target " + label.getLabelType() + " for transformer " + transformer);
                }

                seen = label.getLabelType();
                transformStore.addTransformer(label, transformer, service);
            }
        }
        LOGGER.debug(MODLAUNCHER, "Initialized transformers for transformation service {}", this.service::name);
    }

    ITransformationService getService() {
        return service;
    }

    List<ITransformationService.Resource> runScan(final Environment environment) {
        LOGGER.debug(MODLAUNCHER,"Beginning scan trigger - transformation service {}", this.service::name);
        final List<ITransformationService.Resource> scanResults = this.service.beginScanning(environment);
        LOGGER.debug(MODLAUNCHER,"End scan trigger - transformation service {}", this.service::name);
        return scanResults;
    }

    public List<ITransformationService.Resource> onCompleteScan(IModuleLayerManager moduleLayerManager) {
        return this.service.completeScan(moduleLayerManager);
    }
}
