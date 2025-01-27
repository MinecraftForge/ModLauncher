/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

open module net.minecraftforge.modlauncher.test {
    requires cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;

    requires org.junit.jupiter.api;
    requires powermock.core;
    requires powermock.reflect;
    requires jopt.simple;

    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires static org.jetbrains.annotations;
    requires net.minecraftforge.unsafe;
    requires net.minecraftforge.modlauncher.harness;
    requires net.minecraftforge.modlauncher.testjar;

    provides cpw.mods.modlauncher.api.ITransformationService with
        net.minecraftforge.modlauncher.test.LauncherTests.TransformationService;
}
