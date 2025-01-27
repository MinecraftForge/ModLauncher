/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.harness;

import static net.minecraftforge.modlauncher.harness.internal.TestLaunchHandlerService.TARGET;
import static net.minecraftforge.modlauncher.harness.internal.TestTransformerService.TRANSFORMERS;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.function.Consumer;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import cpw.mods.modlauncher.api.IModuleLayerManager.Layer;
import cpw.mods.modlauncher.api.ITransformer;
import net.minecraftforge.modlauncher.harness.internal.TestTransformerService;
import net.minecraftforge.modlauncher.harness.internal.TestLaunchHandlerService.Target;
import net.minecraftforge.unsafe.UnsafeHacks;

public class ModLauncherTest {
    private static Consumer<String[]> RELAUNCH = getRelaunch();
    private static final ThreadLocal<Boolean> TRANSFORMED = ThreadLocal.withInitial(() -> false);

    private static Consumer<String[]> getRelaunch() {
        try {
            // Bypass some logging, if log4j.xml doesn't override, cuz it feels like being stupid.
            var ctr = Launcher.class.getDeclaredConstructor(boolean.class);
            UnsafeHacks.setAccessible(ctr);
            var run = Launcher.class.getDeclaredMethod("run", String[].class);
            UnsafeHacks.setAccessible(run);
            return (custom) -> {
                try {
                    var args = new String[] {"--version", "1.0", "--launchTarget", "test.harness"};
                    if (custom.length > 0) {
                        var tmp = new String[args.length + custom.length];
                        System.arraycopy(args, 0, tmp, 0, args.length);
                        System.arraycopy(custom, 0, tmp, args.length, custom.length);
                        args = tmp;
                    }
                    run.invoke(ctr.newInstance(true), (Object)args);
                } catch (Throwable e) {
                    sneak(e);
                }
            };
        } catch (Throwable e) {
            return sneak(e);
        }
    }

    /**
     * Sets the callback method, which must be non-static and take no arguments, and return void.
     * A new instance of this class will be created in the transformed ClassLoader.
     * Using the default no-argument constructor.
     * And the method invoked.
     */
    public static void setCallback(String clazz, String method) {
        TARGET.set(new Target(clazz, method));
    }

    /**
     * Sets the callback method, which must be non-static and take no arguments, and return void.
     * A new instance of this class will be created in the transformed ClassLoader.
     * Using the default no-argument constructor.
     * And the method invoked.
     */
    public static void setCallback(Method method) {
        setCallback(method.getDeclaringClass().getName(), method.getName());
    }

    /**
     * Sets the callback method to the method that invokes this function, using the current
     * stack. Only use if you know what you're doing.
     */
    public static void setCallback() {
        setCallback(3);
    }

    private static void setCallback(int depth) {
        var stack = Thread.currentThread().getStackTrace();
        var parent = stack[depth];
        setCallback(parent.getClassName(), parent.getMethodName());
    }

    /**
     * Clears the callback method.
     */
    public static void clearCallback() {
        TARGET.remove();
    }

    /**
     * Clears all paths for the layer.
     */
    public static void clearPaths(IModuleLayerManager.Layer layer) {
        TestTransformerService.getPaths(layer).clear();
    }

    /**
     * Adds a path to the layer.
     */
    public static void addPath(IModuleLayerManager.Layer layer, Path jar) {
        TestTransformerService.getPaths(layer).add(jar);
    }

    /**
     * Returns a modifiable collection of all paths for the layer.
     */
    public static Collection<Path> getPaths(IModuleLayerManager.Layer layer) {
        return TestTransformerService.getPaths(layer);
    }

    public static void addTransformer(@SuppressWarnings("rawtypes") ITransformer transformer) {
        TRANSFORMERS.get().add(transformer);
    }

    /**
     * Checks if we are currently in the transformed ClassLoader.
     */
    public static boolean isTransformed() {
        return TRANSFORMED.get();
    }

    /**
     * Runs ModLauncher with set transformers/target/paths.
     * If the target is not set, it will attempt to set it to the method calling this function.
     * The state (paths and callback) will be cleared when this method returns.
     *
     * @param args Extra arguments to pass to Launcher
     */
    public static void launch(String... args) {
        if (isTransformed())
            throw new IllegalStateException("Can not relaunch ModLauncher");

        try {
            if (TARGET.get() == null)
                setCallback(3);

            var target = TARGET.get();
            var targetCls = Class.forName(target.clazz());
            // Add the target to the game layer so it will be in the transformed ClassLoader
            addPath(Layer.GAME, getPath(targetCls));

            TRANSFORMED.set(true);
            System.out.println(target.clazz() + "." + target.method());
            RELAUNCH.accept(args);
        } catch (Throwable e) {
            sneak(e);
        } finally {
            TRANSFORMED.set(false);
            clearCallback();
            for (var layer : new Layer[] { Layer.GAME, Layer.PLUGIN, Layer.SERVICE })
                clearPaths(layer);
        }
    }

    /**
     * Returns the folder or jar file that contains the class.
     */
    public static Path getPath(Class<?> clz) {
        try {
            return Path.of(clz.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
        } catch (Throwable e) {
            return sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }
}
