/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.jmh;

import cpw.mods.modlauncher.*;
import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import net.minecraftforge.unsafe.UnsafeHacks;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.openjdk.jmh.annotations.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@State(Scope.Benchmark)
public class TransformBenchmark {
    private static final String TARGET_CLASS = "net.minecraftforge.modlauncher.testjar.TestClass";
    private volatile ClassTransformer classTransformer;
    private volatile Transform transform;
    private volatile byte[] classBytes;

    @Setup
    public void setup() throws Exception {
        final TransformStore transformStore = new TransformStore();
        final ModuleLayerHandler layerHandler = createLayerHandler();
        final LaunchPluginHandler lph = new LaunchPluginHandler(layerHandler);
        classTransformer = createClassTransformer(transformStore, lph, null);
        transform = getTransform(classTransformer);

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(TARGET_CLASS.replace('.', '/') + ".class")) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[2048];
            while (is.read(buf) >= 0) {
                bos.write(buf);
            }
            classBytes = bos.toByteArray();
        } catch (IOException e) {
            sneak(e);
        }

        var plugins = UnsafeHacks.<LaunchPluginHandler, Map<String, ILaunchPluginService>>findField(LaunchPluginHandler.class, "plugins").get(lph);
        plugins.put("dummy1", new ILaunchPluginService() {
            @Override
            public String name() {
                return "dummy1";
            }

            @Override
            public boolean processClass(final Phase phase, final ClassNode classNode, final Type classType) {
                return true;
            }

            @Override
            public <T> T getExtension() {
                return null;
            }

            @Override
            public EnumSet<Phase> handlesClass(final Type classType, final boolean isEmpty) {
                return EnumSet.of(Phase.BEFORE, Phase.AFTER);
            }
        });
    }

    @Benchmark
    public int transformNoop() throws Exception {
        byte[] result = transform.transform(new byte[0], "test.MyClass", "jmh");
        return result.length + 1;
    }

    @TearDown(Level.Iteration)
    public void clearLog() {
        var auditTrail = UnsafeHacks.<ClassTransformer, TransformerAuditTrail>findField(ClassTransformer.class, "auditTrail").get(classTransformer);
        var map = UnsafeHacks.<TransformerAuditTrail, Map<String, List<ITransformerActivity>>>findField(TransformerAuditTrail.class, "audit").get(auditTrail);
        map.clear();
    }

    @Benchmark
    public int transformDummyClass() throws Exception {
        byte[] result = transform.transform(classBytes, TARGET_CLASS, "jmh");
        return result.length + 1;
    }

    private static ModuleLayerHandler createLayerHandler() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        var ctr = ModuleLayerHandler.class.getDeclaredConstructor();
        UnsafeHacks.setAccessible(ctr);
        return ctr.newInstance();
    }

    private static ClassTransformer createClassTransformer(TransformStore store, LaunchPluginHandler handler, TransformingClassLoader loader) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        var ctr = ClassTransformer.class.getDeclaredConstructor(TransformStore.class, LaunchPluginHandler.class, TransformingClassLoader.class);
        UnsafeHacks.setAccessible(ctr);
        return ctr.newInstance(store, handler, loader);
    }

    private interface Transform {
        byte[] transform(byte[] bytes, String name, String context) throws Exception;
    }

    private static Transform getTransform(ClassTransformer transformer) throws NoSuchMethodException, SecurityException {
        var mtd = ClassTransformer.class.getDeclaredMethod("transform", byte[].class, String.class, String.class);
        UnsafeHacks.setAccessible(mtd);
        return (bytes, name, context) -> (byte[])mtd.invoke(transformer, bytes, name, context);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }
}
