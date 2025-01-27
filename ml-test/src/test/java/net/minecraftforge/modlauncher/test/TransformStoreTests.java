/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformList;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformTargetLabel;
import cpw.mods.modlauncher.TransformTargetLabel.LabelType;
import cpw.mods.modlauncher.api.ITransformer;
import net.minecraftforge.modlauncher.harness.ModLauncherTest;
import net.minecraftforge.modlauncher.harness.SimpleClassTransformer;
import net.minecraftforge.modlauncher.harness.SimpleFieldTransformer;
import net.minecraftforge.modlauncher.harness.SimpleMethodTransformer;
import net.minecraftforge.modlauncher.testjar.TestClass;
import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.powermock.reflect.Whitebox;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformStoreTests {
    private static final ThreadLocal<Transformers> TRANSFORMERS = new ThreadLocal<>();
    private record Transformers(ITransformer<ClassNode> cls, ITransformer<FieldNode> field, ITransformer<MethodNode> method) {};

    @Test
    void testGatherTransformersNormally() throws Exception {
        if (!ModLauncherTest.isTransformed()) {
            var transformers = new Transformers(
                new SimpleClassTransformer(TestClass.class, Function.identity()),
                new SimpleFieldTransformer(TestClass.class, "field", Function.identity()),
                new SimpleMethodTransformer(TestClass.class, "method", Type.getMethodDescriptor(Type.VOID_TYPE), Function.identity())
            );
            TRANSFORMERS.set(transformers);
            ModLauncherTest.addTransformer(transformers.cls);
            ModLauncherTest.addTransformer(transformers.field);
            ModLauncherTest.addTransformer(transformers.method);
            ModLauncherTest.launch();
        } else {
            UnsafeHacksUtil.hackPowermock();

            var expected = getTransformers();

            TransformStore store = Whitebox.getInternalState(Launcher.INSTANCE, "transformStore");
            EnumMap<LabelType, TransformList<?>> transformers = Whitebox.getInternalState(store, "transformers");
            Set<String> targettedClasses = Whitebox.getInternalState(store, "classNeedsTransforming");
            assertTrue(transformers.containsKey(LabelType.CLASS), "No Class Transformers not found");
            assertTrue(transformers.containsKey(LabelType.FIELD), "No Field Transformers not found");
            assertTrue(transformers.containsKey(LabelType.METHOD), "No Method Transformers not found");
            assertTrue(targettedClasses.contains(Type.getInternalName(TestClass.class)), "TestClass was not found in classes needing transformed");

            assertNotNull(expected, "Expected test transformers were not found");
            check(LabelType.CLASS.getFromMap(transformers), expected.cls, "Class Transformer not found");
            check(LabelType.FIELD.getFromMap(transformers), expected.field, "Field Transformer not found");
            check(LabelType.METHOD.getFromMap(transformers), expected.method, "Method Transformer not found");
        }
    }

    private static <T> void check(TransformList<T> list, ITransformer<T> transformer, String message) {
        var transformers = getTransformers(list).values().stream().flatMap(List::stream).map(t -> unwrap(t)).toList();
        if (transformers.contains(transformer))
            return;

        AssertionFailureBuilder.assertionFailure()
            .expected(transformer)
            .actual(transformers)
            .message(message)
            .buildAndThrow();
    }

    private static <T> Map<TransformTargetLabel, List<ITransformer<T>>> getTransformers(TransformList<T> list) {
        return Whitebox.getInternalState(list, "transformers");
    }

    private static <T> ITransformer<T> unwrap(ITransformer<T> wrapper) {
        return Whitebox.getInternalState(wrapper, "wrapped");
    }

    // We want the same instances as outside the Launcher so read up to the system class loader
    private Transformers getTransformers() throws Exception {
        var cls = Class.forName(TransformStoreTests.class.getName(), true, ClassLoader.getSystemClassLoader());
        assertNotNull(cls, "Could not find ourselves on system class loader");
        final ThreadLocal<?> thread = Whitebox.getInternalState(cls, "TRANSFORMERS");
        assertNotNull(thread, "Could not find ThreadLocal TRANSFORMERS in " + cls);
        var inst = thread.get();
        return new Transformers(
            Whitebox.getInternalState(inst, "cls"),
            Whitebox.getInternalState(inst, "field"),
            Whitebox.getInternalState(inst, "method")
        );
    }
}
