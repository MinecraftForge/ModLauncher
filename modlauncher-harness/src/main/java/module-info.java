/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

module net.minecraftforge.modlauncher.harness {
    requires transitive cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;

    requires static org.jetbrains.annotations;
    requires net.minecraftforge.unsafe;
    requires transitive org.objectweb.asm.tree;

    exports net.minecraftforge.modlauncher.harness;

    provides cpw.mods.modlauncher.api.ILaunchHandlerService with
        net.minecraftforge.modlauncher.harness.internal.TestLaunchHandlerService;
    provides cpw.mods.modlauncher.api.ITransformationService with
        net.minecraftforge.modlauncher.harness.internal.TestTransformerService;
}
