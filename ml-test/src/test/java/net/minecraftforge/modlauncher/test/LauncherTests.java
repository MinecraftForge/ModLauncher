/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.ITransformationService;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test overall launcher
 */
class LauncherTests {
    private static final String TARGET_MODULE = "net.minecraftforge.modlauncher.testjar";
    private static final String TARGET_CLASS = TARGET_MODULE + ".TestClass";
    private static final String RESOURCE_CLASS = TARGET_MODULE + ".ResourceLoadingClass";

    @Test
    void testLauncher() throws Exception {
        UnsafeHacksUtil.hackPowermock();
        System.setProperty("test.harness", "../ml-test-jar/build/classes/java/main\0../ml-test-jar/build/resources/main");

        Launcher.main("--version", "1.0", "--launchTarget", "mockLaunch", "--test.mods", "A,B,C," + TARGET_CLASS, "--accessToken", "SUPERSECRET!");
        Launcher instance = Launcher.INSTANCE;
        final Map<String, TransformationServiceDecorator> services = Whitebox.getInternalState(Whitebox.getInternalState(instance, "transformationServicesHandler"), "serviceLookup");
        final List<ITransformationService> launcherServices = services.values().stream()
            .map(dec -> Whitebox.<ITransformationService>getInternalState(dec, "service"))
            .toList();
        assertAll("services are present and correct",
                () -> assertEquals(1, launcherServices.size(), "Found 1 service"),
                () -> assertEquals(MockTransformerService.class, launcherServices.get(0).getClass(), "Found Test Launcher Service")
        );

        final ArgumentHandler argumentHandler = Whitebox.getInternalState(instance, "argumentHandler");
        final OptionSet options = Whitebox.getInternalState(argumentHandler, "optionSet");
        Map<String, OptionSpec<?>> optionsMap = options.specs().stream().collect(Collectors.toMap(s -> String.join(",", s.options()), s -> s, (u, u2) -> u));

        assertAll("options are correctly setup",
                () -> assertTrue(optionsMap.containsKey("version"), "Version field is correct"),
                () -> assertTrue(optionsMap.containsKey("test.mods"), "Test service option is correct")
        );

        final MockTransformerService mockTransformerService = (MockTransformerService) launcherServices.get(0);
        assertAll("test launcher service is correctly configured",
                () -> assertIterableEquals(Arrays.asList("A", "B", "C", TARGET_CLASS), Whitebox.getInternalState(mockTransformerService, "modList"), "modlist is configured"),
                () -> assertEquals("INITIALIZED", Whitebox.getInternalState(mockTransformerService, "state"), "Initialized was called")
        );

        assertAll(
                () -> assertNotNull(instance.environment().getProperty(IEnvironment.Keys.VERSION.get()))
        );

        try {
            ClassLoader transformingClassLoader = Whitebox.getInternalState(Launcher.INSTANCE, "classLoader");
            var transformedClass = Class.forName(TARGET_CLASS, true, transformingClassLoader);
            assertDoesNotThrow(() -> transformedClass.getDeclaredField("testfield"), "Transformer failed to run");
            var layerManager = Launcher.INSTANCE.findLayerManager().orElseThrow();
            var pluginLayer = layerManager.getLayer(IModuleLayerManager.Layer.PLUGIN).orElseThrow();
            var rawModule = pluginLayer.findModule(TARGET_MODULE).orElseThrow();
            var rawClass = Class.forName(rawModule, TARGET_CLASS);
            assertThrows(NoSuchFieldException.class, () -> rawClass.getDeclaredField("testfield"), "Raw class had field that was supposed to be injected by transformer");

            var resClass = Class.forName(RESOURCE_CLASS, true, transformingClassLoader);
            assertFindResource(resClass);
        } catch (ClassNotFoundException e) {
            fail("Can't load class", e);
        }
    }

    private void assertFindResource(Class<?> loaded) throws Exception {
        Object instance = loaded.getDeclaredConstructor().newInstance();
        URL resource = (URL) Whitebox.getField(loaded, "resource").get(instance);
        assertNotNull(resource, "Resource not found");
        // assert that we can find something in the resource, so we know it loaded properly
        try (InputStream in = resource.openStream();
             Scanner scanner = new Scanner(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            assertTrue(scanner.nextLine().contains("Loaded successfully!"), "Resource has incorrect content");
        }
    }
}
