/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

open module net.minecraftforge.modlauncher.testjar {
    exports net.minecraftforge.modlauncher.testjar;

    provides net.minecraftforge.modlauncher.testjar.ITestServiceLoader with
        net.minecraftforge.modlauncher.testjar.TestServiceLoader;
}
