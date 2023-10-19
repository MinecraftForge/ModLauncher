/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.TypesafeMap;
import net.minecraftforge.securemodules.SecureModuleFinder;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class loader
 */
class TransformingClassLoaderTests {
    private static final String TARGET_CLASS = "net.minecraftforge.modlauncher.testjar.TestClass";

    @Test
    void testClassLoader() throws Exception {
        UnsafeHacksUtil.hackPowermock();
        MockTransformerService mockTransformerService = new MockTransformerService() {
            @NotNull
            @Override
            public List<ITransformer> transformers() {
                return Stream.of(new ClassNodeTransformer(List.of(TARGET_CLASS))).collect(Collectors.toList());
            }
        };

        TransformStore transformStore = new TransformStore();
        ModuleLayerHandler layerHandler = Whitebox.invokeConstructor(ModuleLayerHandler.class);
        LaunchPluginHandler lph = new LaunchPluginHandler(layerHandler);
        TransformationServiceDecorator sd = Whitebox.invokeConstructor(TransformationServiceDecorator.class, mockTransformerService);
        sd.gatherTransformers(transformStore);

        Environment environment = Whitebox.invokeConstructor(Environment.class, new Class[]{ Launcher.class }, new Object[]{ null });
        new TypesafeMap(IEnvironment.class);
        Class<?> builderClass = Class.forName("cpw.mods.modlauncher.TransformingClassLoaderBuilder");
        Constructor<TransformingClassLoader> constructor = Whitebox.getConstructor(TransformingClassLoader.class, TransformStore.class, LaunchPluginHandler.class, builderClass, Environment.class, Configuration.class, List.class);
        Configuration configuration = createTestJarsConfiguration();
        TransformingClassLoader tcl = constructor.newInstance(transformStore, lph, null, environment, configuration, List.of(ModuleLayer.boot()));
        ModuleLayer.boot().defineModules(configuration, s -> tcl);

        final Class<?> aClass = Class.forName(TARGET_CLASS, true, tcl);
        assertEquals(Whitebox.getField(aClass, "testfield").getType(), String.class);
        assertEquals(Whitebox.getField(aClass, "testfield").get(null), "CHEESE!");

        final Class<?> newClass = tcl.loadClass(TARGET_CLASS);
        assertEquals(aClass, newClass, "Class instance is the same from Class.forName and tcl.loadClass");
    }

    private Configuration createTestJarsConfiguration() {
        var testJars = SecureJar.from(Path.of("../ml-test-jar/build/classes/java/main/"));
        var finder = SecureModuleFinder.of(testJars);
        return ModuleLayer.boot().configuration().resolveAndBind(finder, ModuleFinder.ofSystem(), Set.of("net.minecraftforge.modlauncher.testjar"));
    }
}
