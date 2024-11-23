/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import net.minecraftforge.securemodules.SecureModuleClassLoader;

import java.lang.module.Configuration;
import java.util.*;

/**
 * Module transforming class loader
 */
public class TransformingClassLoader extends SecureModuleClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }
    private final ClassTransformer classTransformer;

    private static ModuleLayer get(ModuleLayerHandler layers, Layer layer) {
        var moduleLayer = layers.getLayer(layer).orElse(null);
        if (moduleLayer == null)
            throw new NullPointerException("Failed to find " + layer.name() + " layer");

        return moduleLayer;
    }

    public TransformingClassLoader(TransformStore transformStore, LaunchPluginHandler pluginHandler, ModuleLayerHandler layers) {
        super("TRANSFORMER", null, get(layers, Layer.GAME).configuration(), List.of(get(layers, Layer.SERVICE)), List.of(), true);
        this.classTransformer = new ClassTransformer(transformStore, pluginHandler, this);
    }

    TransformingClassLoader(String name, ClassLoader parent, Configuration config, List<ModuleLayer> parentLayers, List<ClassLoader> parentLoaders,
            TransformStore transformStore, LaunchPluginHandler pluginHandler, Environment environment) {
        super(name, parent, config, parentLayers, parentLoaders, true);
        TransformerAuditTrail tat = new TransformerAuditTrail();
        environment.putPropertyIfAbsent(IEnvironment.Keys.AUDITTRAIL.get(), tat);
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
