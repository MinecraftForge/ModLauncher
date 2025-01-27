/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import net.minecraftforge.modlauncher.harness.ModLauncherTest;
import net.minecraftforge.modlauncher.harness.SimpleClassTransformer;
import net.minecraftforge.modlauncher.harness.SimpleFieldTransformer;
import net.minecraftforge.modlauncher.harness.SimpleMethodTransformer;
import net.minecraftforge.modlauncher.testjar.ModLauncherTestMarker;
import net.minecraftforge.modlauncher.testjar.TestClass;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.powermock.reflect.Whitebox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Modifier;

/**
 * Test class loader
 */
class TransformingTests {
    @Test
    void testClassTransformer() {
        if (!ModLauncherTest.isTransformed()) {
            assertThrows(NoSuchFieldException.class, () -> TestClass.class.getDeclaredField("new_field"), "Class already had new_field which is supposed to be added by transformer");

            ModLauncherTest.addPath(Layer.GAME, ModLauncherTest.getPath(ModLauncherTestMarker.class));
            ModLauncherTest.addTransformer(new SimpleClassTransformer(TestClass.class, input -> {
                final var fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "new_field", "Ljava/lang/String;", null, "initial_value");
                input.fields.add(fn);
                return input;
            }));
            ModLauncherTest.launch();
        } else {
            final Class<?> aClass = TestClass.class;
            assertEquals(Whitebox.getField(aClass, "new_field").getType(), String.class);
            assertEquals(Whitebox.getInternalState(aClass, "new_field"), "initial_value");
        }
    }

    @Test
    void testFieldTransformer() throws Exception {
        if (!ModLauncherTest.isTransformed()) {
            var fld = TestClass.class.getDeclaredField("field");
            assertEquals("private", Modifier.toString(fld.getModifiers()), "Test field was already public");

            ModLauncherTest.addPath(Layer.GAME, ModLauncherTest.getPath(ModLauncherTestMarker.class));
            ModLauncherTest.addTransformer(new SimpleFieldTransformer(TestClass.class, "field", input -> {
                input.access = (input.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
                return input;
            }));
            ModLauncherTest.launch();
        } else {
            var fld = TestClass.class.getDeclaredField("field");
            assertEquals("public", Modifier.toString(fld.getModifiers()), "Test field was not made public");
        }
    }

    @Test
    void testMethodTransformer() {
        if (!ModLauncherTest.isTransformed()) {
            assertEquals("unmodified", TestClass.method(), "Test class was already modified");
            ModLauncherTest.addPath(Layer.GAME, ModLauncherTest.getPath(ModLauncherTestMarker.class));
            ModLauncherTest.addTransformer(new SimpleMethodTransformer(TestClass.class, "method", Type.getMethodDescriptor(Type.getType(String.class)).toString(), input -> {
                input.instructions.clear();
                input.instructions.add(new LdcInsnNode("modified"));
                input.instructions.add(new InsnNode(Opcodes.ARETURN));
                return input;
            }));
            ModLauncherTest.launch();
        } else {
            assertEquals("modified", TestClass.method(), "Test method was not modified");
        }
    }
}
