/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.function.Supplier;

/**
 * The internal vote context structure.
 */
record VotingContext(
        String getClassName,
        boolean doesClassExist,
        Supplier<byte[]> sha256,
        List<ITransformerActivity> getAuditActivities,
        String reason,
        NodeHolder node
) implements ITransformerVotingContext {
    private static final Object[] EMPTY = new Object[0];

    VotingContext(String className, boolean classExists, Supplier<byte[]> sha256sum, List<ITransformerActivity> activities, String reason) {
        this(className, classExists, sha256sum, activities, reason, new NodeHolder());
    }

    @Override
    public byte[] getInitialClassSha256() {
        return sha256.get();
    }

    @Override
    public String getReason() {
        return reason;
    }

    <T> void setNode(final T node) {
        this.node.value = node;
    }

    @Override
    public boolean applyFieldPredicate(FieldPredicate fieldPredicate) {
        FieldNode fn = (FieldNode) this.node.value;
        final PredicateVisitor predicateVisitor = new PredicateVisitor(fieldPredicate);
        fn.accept(predicateVisitor);
        return predicateVisitor.getResult();
    }

    @Override
    public boolean applyMethodPredicate(MethodPredicate methodPredicate) {
        MethodNode mn = (MethodNode) this.node.value;
        final PredicateVisitor predicateVisitor = new PredicateVisitor(methodPredicate);
        mn.accept(predicateVisitor);
        return predicateVisitor.getResult();
    }

    @Override
    public boolean applyClassPredicate(ClassPredicate classPredicate) {
        ClassNode cn = (ClassNode) this.node.value;
        final PredicateVisitor predicateVisitor = new PredicateVisitor(classPredicate);
        cn.accept(predicateVisitor);
        return predicateVisitor.getResult();
    }

    @Override
    public boolean applyInstructionPredicate(InsnPredicate insnPredicate) {
        MethodNode mn = (MethodNode) this.node.value;
        boolean result = false;
        final AbstractInsnNode[] insnNodes = mn.instructions.toArray();
        for (int i = 0; i < insnNodes.length; i++) {
            result |= insnPredicate.test(i, insnNodes[i].getOpcode(), toObjectArray(insnNodes[0]));
        }
        return result;
    }

    private static Object[] toObjectArray(final AbstractInsnNode insnNode) {
        return switch (insnNode) {
            case MethodInsnNode methodInsnNode ->
                    new Object[] { methodInsnNode.name, methodInsnNode.desc, methodInsnNode.owner, methodInsnNode.itf };
            case FieldInsnNode fieldInsnNode ->
                    new Object[] { fieldInsnNode.name, fieldInsnNode.desc, fieldInsnNode.owner };
            default -> EMPTY;
        };
    }

    private static final class NodeHolder {
        private Object value;
    }
}
