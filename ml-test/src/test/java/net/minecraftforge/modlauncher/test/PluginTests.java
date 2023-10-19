/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginTests {

    @Test
    void pluginTests() {
        ILaunchPluginService plugin = new ILaunchPluginService() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public void offerResource(final Path resource, final String name) {

            }

            @Override
            public boolean processClass(final Phase phase, final ClassNode classNode, final Type classType) {
                return false;
            }

            @Override
            public String getExtension() {
                return "CHEESE";
            }

            @Override
            public EnumSet<Phase> handlesClass(final Type classType, final boolean isEmpty) {
                return EnumSet.of(Phase.BEFORE);
            }
        };

        String s = plugin.getExtension();
        assertEquals("CHEESE", s);
    }
}
