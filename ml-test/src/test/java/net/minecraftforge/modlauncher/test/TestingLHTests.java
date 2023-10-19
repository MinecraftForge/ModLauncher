/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.ServiceRunner;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingLHTests {
    boolean calledback;

    @Test
    void testTestingLaunchHandler() {
        System.setProperty("test.harness", "../ml-test-jar/build/classes/java/main");
        System.setProperty("test.harness.callable", "net.minecraftforge.modlauncher.test.TestingLHTests$TestCallback");
        calledback = false;
        TestCallback.callable = () -> {
            calledback = true;
            LogManager.getLogger().info("Hello", new Throwable());
        };
        Launcher.main("--version", "1.0", "--launchTarget", "testharness");
        assertTrue(calledback, "We got called back");
    }

    public static class TestCallback {
        private static ServiceRunner callable;
        public static ServiceRunner supplier() {
            return callable;
        }
    }
}
