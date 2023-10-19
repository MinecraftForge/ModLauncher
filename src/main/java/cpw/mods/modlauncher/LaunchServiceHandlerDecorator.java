/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

/**
 * Decorates {@link ILaunchHandlerService} for use by the system
 */
record LaunchServiceHandlerDecorator(ILaunchHandlerService service) {

    public void launch(String[] arguments, ModuleLayer gameLayer) {
        try {
            this.service.launchService(arguments, gameLayer).run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void configureTransformationClassLoaderBuilder(ITransformingClassLoaderBuilder builder) {
        this.service.configureTransformationClassLoader(builder);
    }
}
