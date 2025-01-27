/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Function;

public class EnumerationHelper {
    public static <T> Enumeration<T> merge(Enumeration<T> first, Enumeration<T> second) {
        return new Enumeration<T>() {
            @Override
            public boolean hasMoreElements() {
                return first.hasMoreElements() || second.hasMoreElements();
            }

            @Override
            public T nextElement() {
                return first.hasMoreElements() ? first.nextElement() : second.nextElement();
            }
        };
    }

    public static <T> Function<String, Enumeration<T>> mergeFunctors(Function<String, Enumeration<T>> first, Function<String, Enumeration<T>> second) {
        return input -> merge(first.apply(input), second.apply(input));
    }

    public static <T> T firstElementOrNull(final Enumeration<T> enumeration) {
        return enumeration.hasMoreElements() ? enumeration.nextElement() : null;
    }

    public static <T> Function<String, Enumeration<T>> fromOptional(final Function<String, Optional<T>> additionalClassBytesLocator) {
        return input -> Collections.enumeration(additionalClassBytesLocator.apply(input).stream().toList());
    }
}
