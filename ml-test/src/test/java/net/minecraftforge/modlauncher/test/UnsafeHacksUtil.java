/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import net.minecraftforge.unsafe.UnsafeHacks;

public class UnsafeHacksUtil {
    @SuppressWarnings("unchecked")
    public static <T> T getInternalState(Object obj, String fieldName) {
        try {
            var fld = obj.getClass().getDeclaredField(fieldName);
            UnsafeHacks.setAccessible(fld);
            return (T)fld.get(obj);
        } catch (Exception e) {
            return sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T getInternalState(Class<?> cls, String fieldName) {
        try {
            var fld = cls.getDeclaredField(fieldName);
            UnsafeHacks.setAccessible(fld);
            return (T)fld.get(null);
        } catch (Exception e) {
            return sneak(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, R> R sneak(Throwable e) throws E {
        throw (E)e;
    }
}
