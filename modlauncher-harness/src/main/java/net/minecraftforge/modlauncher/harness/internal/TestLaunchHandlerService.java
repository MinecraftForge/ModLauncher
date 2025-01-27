/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.harness.internal;

import cpw.mods.modlauncher.api.ILaunchHandlerService;
import cpw.mods.modlauncher.api.ServiceRunner;
import net.minecraftforge.unsafe.UnsafeHacks;

public class TestLaunchHandlerService implements ILaunchHandlerService {
    public static final ThreadLocal<Target> TARGET = new ThreadLocal<>();
    public record Target(String clazz, String method) {}

    @Override
    public String name() {
        return "test.harness";
    }

    public ServiceRunner launchService(String[] arguments, ModuleLayer gameLayer) {
        try {
            var ccl = Thread.currentThread().getContextClassLoader();
            var target = TARGET.get();
            if (target == null)
                throw new IllegalStateException("Not target method set");
            var cls = Class.forName(target.clazz(), true, ccl);
            var ctr = cls.getDeclaredConstructor();
            UnsafeHacks.setAccessible(ctr);
            var mtd = cls.getDeclaredMethod(target.method());
            UnsafeHacks.setAccessible(mtd);
            var inst = ctr.newInstance();
            return () -> mtd.invoke(inst);
        } catch (Throwable throwable) {
            return sneak(throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }
}
