/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.harness;

import java.util.function.Function;

import org.objectweb.asm.tree.MethodNode;

import cpw.mods.modlauncher.api.ITransformer;

public class SimpleMethodTransformer extends SimpleTransformer<MethodNode> implements ITransformer<MethodNode> {
    public SimpleMethodTransformer(Class<?> cls, String method, String desc, Function<MethodNode, MethodNode> transformer) {
        this(assertNotNull(cls, "cls").getName(), method, desc, transformer);
    }

    public SimpleMethodTransformer(String cls, String method, String desc, Function<MethodNode, MethodNode> transformer) {
        super(Target.targetMethod(assertNotNull(cls, "cls"), assertNotNull(method, "field"), assertNotNull(desc, "desc")), transformer);
    }
}
