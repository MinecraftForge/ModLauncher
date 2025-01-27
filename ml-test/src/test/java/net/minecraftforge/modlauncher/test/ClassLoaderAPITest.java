/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import net.minecraftforge.modlauncher.harness.ModLauncherTest;
import net.minecraftforge.modlauncher.testjar.ITestServiceLoader;
import net.minecraftforge.modlauncher.testjar.ModLauncherTestMarker;

import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassLoaderAPITest {
    @Test
    void testServiceLoader() {
        if (!ModLauncherTest.isTransformed()) {
            ModLauncherTest.addPath(Layer.GAME, ModLauncherTest.getPath(ModLauncherTestMarker.class));
            ModLauncherTest.launch();
        } else {
            var manager = Launcher.INSTANCE.findLayerManager().orElse(null);
            assertTrue(manager != null, "Failed to find layer manager");
            var layer = manager.getLayer(Layer.GAME).orElse(null);
            assertTrue(layer != null, "Failed to find game layer");
            // We need to add uses in case our module info does't already
            getClass().getModule().addUses(ITestServiceLoader.class);

            var load = ServiceLoader.load(layer, ITestServiceLoader.class);
            var services = load.stream().toList();
            assertTrue(services.size() == 1, "Failed to load service");
        }
    }
}
