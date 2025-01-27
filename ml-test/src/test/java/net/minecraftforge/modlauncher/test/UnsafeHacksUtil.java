/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import net.minecraftforge.unsafe.UnsafeHacks;

public class UnsafeHacksUtil {
    @SuppressWarnings("unchecked")
    public static <T> T getInternalState(Object obj, String fieldName) {
        @SuppressWarnings("rawtypes")
        Class clazz = (Class)obj.getClass();
        var access = UnsafeHacks.<Object, T>findField(clazz, fieldName);
        return access.get(obj);
    }
}
