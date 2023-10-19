/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

module cpw.mods.modlauncher {
    requires java.base;
    requires org.apache.logging.log4j;
    requires transitive org.objectweb.asm.tree;
    requires org.apache.logging.log4j.core;
    requires jopt.simple;
    requires transitive cpw.mods.securejarhandler;
    requires static org.jetbrains.annotations;
    exports cpw.mods.modlauncher.log;
    exports cpw.mods.modlauncher.serviceapi;
    exports cpw.mods.modlauncher.api;
    exports cpw.mods.modlauncher.util;
    exports cpw.mods.modlauncher;
    uses cpw.mods.modlauncher.api.ILaunchHandlerService;
    uses cpw.mods.modlauncher.api.INameMappingService;
    uses cpw.mods.modlauncher.api.ITransformationService;
    uses cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
    uses cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService;
    provides cpw.mods.modlauncher.api.ILaunchHandlerService with
            cpw.mods.modlauncher.DefaultLaunchHandlerService, cpw.mods.modlauncher.TestingLaunchHandlerService;
    // for bootstrap launcher to find us without having to expose our Launcher main method
    provides java.util.function.Consumer with cpw.mods.modlauncher.BootstrapLaunchConsumer;
}