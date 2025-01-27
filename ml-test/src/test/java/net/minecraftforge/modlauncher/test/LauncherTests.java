/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.IncompatibleEnvironmentException;
import joptsimple.OptionSpec;
import joptsimple.OptionSpecBuilder;
import net.minecraftforge.modlauncher.harness.ModLauncherTest;
import net.minecraftforge.modlauncher.testjar.ModLauncherTestMarker;
import net.minecraftforge.modlauncher.testjar.ResourceLoadingClass;

/**
 * Test overall launcher
 */
public class LauncherTests {
    @Test
    void testLauncher() throws Exception {
        if (!ModLauncherTest.isTransformed()) {
            ModLauncherTest.addPath(Layer.GAME, ModLauncherTest.getPath(ModLauncherTestMarker.class));
            ModLauncherTest.launch("--" + TransformationService.name + ".test", "test arg");
            return;
        }

        UnsafeHacksUtil.hackPowermock();
        Launcher instance = Launcher.INSTANCE;

        final Map<String, TransformationServiceDecorator> services = Whitebox.getInternalState(Whitebox.getInternalState(instance, "transformationServicesHandler"), "serviceLookup");
        final List<ITransformationService> launcherServices = services.values().stream()
            .map(dec -> Whitebox.<ITransformationService>getInternalState(dec, "service"))
            .toList();
        var service = launcherServices.stream()
            .filter(s -> s.name().equals(TransformationService.name))
            .findFirst().orElse(null);
        assertNotNull(service, "Failed to locate test TransformationService");
        assertNotNull(Whitebox.getInternalState(service, "testArg"), "TransformationService custom arguments wernt created");
        assertEquals("test arg", Whitebox.getInternalState(service, "testArgValue"), "TransformationService did not get the correct argument");
        assertTrue(Whitebox.<Boolean>getInternalState(service, "initalized"), "TransforamtionService initalize was never called");

        assertEquals("test.harness", instance.environment().getProperty(IEnvironment.Keys.LAUNCHTARGET.get()).orElse(null), "Launcher environment was not set");

        URL resource = new ResourceLoadingClass().resource;
        assertNotNull(resource, "Resource not found");
        // assert that we can find something in the resource, so we know it loaded properly
        try (InputStream in = resource.openStream()) {
            var data = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("Loaded successfully!", data, "Resource has incorrect content");
        }
    }

    public static class TransformationService implements ITransformationService {
        public static final String name = (LauncherTests.class.getSimpleName() + "." + TransformationService.class.getSimpleName()).toLowerCase(Locale.ENGLISH);
        public boolean initalized = false;
        public OptionSpec<String> testArg;
        public String testArgValue;


        @Override
        public @NotNull String name() {
            return name;
        }

        @Override
        public void initialize(IEnvironment environment) {
            initalized = true;
        }

        @Override
        public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {
        }

        @Override
        public void arguments(BiFunction<String, String, OptionSpecBuilder> builder) {
            testArg = builder.apply("test", "A test argument requiring a string value")
                .withRequiredArg().ofType(String.class);
        }

        @Override
        public void argumentValues(OptionResult result) {
            testArgValue = result.value(testArg);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public @NotNull List<ITransformer> transformers() {
            return Collections.emptyList();
        }
    }
}
