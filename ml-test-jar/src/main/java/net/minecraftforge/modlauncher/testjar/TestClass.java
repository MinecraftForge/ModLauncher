/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.testjar;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Test class loaded by tests and manipulated by transformers
 */
@SuppressWarnings("unused")
public class TestClass {
    private String field;

    public static String method() {
        return "unmodified";
    }
}
