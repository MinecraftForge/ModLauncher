/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.harness;

import java.util.function.Function;

import org.objectweb.asm.tree.FieldNode;

import cpw.mods.modlauncher.api.ITransformer;

public class SimpleFieldTransformer extends SimpleTransformer<FieldNode> implements ITransformer<FieldNode> {
    public SimpleFieldTransformer(Class<?> cls, String field, Function<FieldNode, FieldNode> transformer) {
        this(assertNotNull(cls, "cls").getName(), field, transformer);
    }

    public SimpleFieldTransformer(String cls, String field, Function<FieldNode, FieldNode> transformer) {
        super(Target.targetField(assertNotNull(cls, "cls"), assertNotNull(field, "field")), transformer);
    }
}
