/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassLoaderAPITest {
    @Test
    void testGetResources() throws ClassNotFoundException {
        var testMod = "net.minecraftforge.modlauncher.testjar.TestClass";
        var testServiceLoader = "net.minecraftforge.modlauncher.testjar.ITestServiceLoader";

        Launcher.main("--version", "1.0", "--launchTarget", "mockLaunch", "--test.mods", "A,B,C," + testMod, "--accessToken", "SUPERSECRET!");
        ModuleLayer layer = Launcher.INSTANCE.findLayerManager()
            .flatMap(manager -> manager.getLayer(IModuleLayerManager.Layer.GAME))
            .orElseThrow();
        final Class<?> service = Thread.currentThread().getContextClassLoader().loadClass(testServiceLoader);
        getClass().getModule().addUses(service);

        final ServiceLoader<?> load = ServiceLoader.load(layer, service);
        assertTrue(load.iterator().hasNext());
    }
}
