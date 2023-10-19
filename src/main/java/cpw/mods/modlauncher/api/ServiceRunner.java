/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

/**
 * A type of Runnable that throws a Throwable. Allows for tidier implementations than Callable<Void>
 */
public interface ServiceRunner {
    void run() throws Throwable;

    ServiceRunner NOOP = () -> {};
}
