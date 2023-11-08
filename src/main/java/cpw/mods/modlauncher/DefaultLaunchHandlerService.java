/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

import java.lang.reflect.*;
import java.nio.file.*;

/**
 * This has not worked in years
 */
@Deprecated(forRemoval = true, since = "10.1")
public class DefaultLaunchHandlerService implements ILaunchHandlerService {
    public static final String LAUNCH_PROPERTY = "minecraft.client.jar";
    public static final String LAUNCH_PATH_STRING = System.getProperty(LAUNCH_PROPERTY);

    @Override
    public String name() {
        return "minecraft";
    }

    @Override
    public void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder) {
        if (LAUNCH_PATH_STRING == null) {
            throw new IllegalStateException("Missing "+ LAUNCH_PROPERTY +" environment property. Update your launcher!");
        }
        builder.addTransformationPath(FileSystems.getDefault().getPath(LAUNCH_PATH_STRING));
    }

    @Override
    public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {

        return () -> {
            final Class<?> mcClass = Class.forName(gameLayer.findModule("minecraft").orElseThrow(), "net.minecraft.client.main.Main");
            final Method mcClassMethod = mcClass.getMethod("main", String[].class);
            mcClassMethod.invoke(null, (Object) arguments);
        };
    }

    @Override
    public NamedPath[] getPaths() {
        return new NamedPath[] {new NamedPath("launch",FileSystems.getDefault().getPath(LAUNCH_PATH_STRING))};
    }
}
