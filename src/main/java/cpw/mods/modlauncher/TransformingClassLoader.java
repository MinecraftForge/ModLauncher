/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.modlauncher.api.*;

import java.lang.module.Configuration;
import java.util.*;

/**
 * Module transforming class loader
 */
public class TransformingClassLoader extends ModuleClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    private final ClassTransformer classTransformer;

    public TransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, ModuleLayerHandler moduleLayerHandler) {
        super("TRANSFORMER", moduleLayerHandler.getLayer(IModuleLayerManager.Layer.GAME).orElseThrow().configuration(), List.of(moduleLayerHandler.getLayer(IModuleLayerManager.Layer.SERVICE).orElseThrow()));
        this.classTransformer = new ClassTransformer(transformStore, pluginHandler, this);
    }

    TransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, TransformingClassLoaderBuilder builder, final Environment environment, final Configuration configuration, List<ModuleLayer> parentLayers) {
        super("TRANSFORMER", configuration, parentLayers);
        TransformerAuditTrail tat = new TransformerAuditTrail();
        environment.computePropertyIfAbsent(IEnvironment.Keys.AUDITTRAIL.get(), v->tat);
        this.classTransformer = new ClassTransformer(transformStore, pluginHandler, this, tat);
    }

    @Override
    protected byte[] maybeTransformClassBytes(final byte[] bytes, final String name, final String context) {
        return classTransformer.transform(bytes, name, context != null ? context : ITransformerActivity.CLASSLOADING_REASON);
    }

    public Class<?> getLoadedClass(String name) {
        return findLoadedClass(name);
    }

    byte[] buildTransformedClassNodeFor(final String className, final String reason) throws ClassNotFoundException {
        return super.getMaybeTransformedClassBytes(className, reason);
    }
}
