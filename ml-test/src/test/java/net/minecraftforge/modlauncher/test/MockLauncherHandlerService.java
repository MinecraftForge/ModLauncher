/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;
import cpw.mods.modlauncher.api.ServiceRunner;

/**
 * Mock launch handler for testing
 */
public class MockLauncherHandlerService implements ILaunchHandlerService {
    @Override
    public String name() {
        return "mockLaunch";
    }

    @Override
    public void configureTransformationClassLoader(final ITransformingClassLoaderBuilder builder) {

    }

    @Override
    public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {
        return ServiceRunner.NOOP;
    }
}
