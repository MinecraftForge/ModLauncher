/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.api.TypesafeMap;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Typesafe map tests
 */
class TypesafeMapTests {
    @Test
    void testTypesafeMapKey() {
        TypesafeMap context = new TypesafeMap();
        assertThrows(IllegalArgumentException.class,
                () ->
                {
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                    TypesafeMap.Key.getOrCreate(context, "testkey1", Integer.class);
                }
        );

        assertAll(
                () ->
                {
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                },
                () ->
                {
                    TypesafeMap.Key.getOrCreate(context, "testkey1", String.class);
                    TypesafeMap.Key.getOrCreate(context, "testkey2", Integer.class);
                }
        );
    }

    @Test
    void testTypesafeMap() {
        TypesafeMap b = new TypesafeMap();
        TypesafeMap.Key<String> mykey = TypesafeMap.Key.getOrCreate(b, "testkey1", String.class);
        assertEquals(b.get(mykey), Optional.empty(), "Key not found");
        b.computeIfAbsent(mykey, s -> "Hello");
        assertEquals(b.get(mykey), Optional.of("Hello"), "Found key");
    }
}
