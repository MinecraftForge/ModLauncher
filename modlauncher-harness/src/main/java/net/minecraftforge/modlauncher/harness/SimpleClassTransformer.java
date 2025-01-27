/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.harness;

import java.util.function.Function;

import org.objectweb.asm.tree.ClassNode;

import cpw.mods.modlauncher.api.ITransformer;

public class SimpleClassTransformer extends SimpleTransformer<ClassNode> implements ITransformer<ClassNode> {
    public SimpleClassTransformer(Class<?> cls, Function<ClassNode, ClassNode> transformer) {
        this(assertNotNull(cls, "cls").getName(), transformer);
    }

    public SimpleClassTransformer(String cls, Function<ClassNode, ClassNode> transformer) {
        super(Target.targetClass(assertNotNull(cls, "cls")), transformer);
    }
}
