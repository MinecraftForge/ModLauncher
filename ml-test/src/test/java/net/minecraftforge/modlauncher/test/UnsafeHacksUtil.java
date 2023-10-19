/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import java.lang.reflect.Method;

import org.powermock.reflect.Whitebox;

import cpw.mods.modlauncher.TransformationServiceDecorator;
import net.minecraftforge.unsafe.UnsafeHacks;

public class UnsafeHacksUtil {
    public static void hackPowermock() throws Exception {
        addOpen(Object.class, Whitebox.class);
        addOpen(TransformationServiceDecorator.class, Whitebox.class);
    }

    private static Method implAddExportsOrOpens;
    private static void addOpen(Class<?> target, Class<?> reader) throws Exception {
        if (implAddExportsOrOpens == null) {
            implAddExportsOrOpens = Module.class.getDeclaredMethod("implAddExportsOrOpens", String.class, Module.class, boolean.class, boolean.class);
            UnsafeHacks.setAccessible(implAddExportsOrOpens);
        }
        implAddExportsOrOpens.invoke(target.getModule(), target.getPackageName(), reader.getModule(), /*open*/true, /*syncVM*/true);
    }
}
