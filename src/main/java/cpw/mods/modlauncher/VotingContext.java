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
final class VotingContext implements ITransformerVotingContext {
    private static final Object[] EMPTY = new Object[0];
    private final String className;
    private final boolean classExists;
    private final Supplier<byte[]> sha256;
    private final List<ITransformerActivity> auditActivities;
    private final String reason;
    private Object node;

    VotingContext(String className, boolean classExists, Supplier<byte[]> sha256sum, final List<ITransformerActivity> activities, final String reason) {
        this.className = className;
        this.classExists = classExists;
        this.sha256 = sha256sum;
        this.auditActivities = activities;
        this.reason = reason;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean doesClassExist() {
        return classExists;
    }

    @Override
    public byte[] getInitialClassSha256() {
        return sha256.get();
    }

    @Override
    public List<ITransformerActivity> getAuditActivities() {
        return auditActivities;
    }

    @Override
    public String getReason() {
        return reason;
    }

    <T> void setNode(final T node) {
        this.node = node;
    }

    @Override
    public boolean applyFieldPredicate(FieldPredicate fieldPredicate) {
        FieldNode fn = (FieldNode) this.node;
        final PredicateVisitor predicateVisitor = new PredicateVisitor(fieldPredicate);
        fn.accept(predicateVisitor);
        return predicateVisitor.getResult();
    }

    @Override
    public boolean applyMethodPredicate(MethodPredicate methodPredicate) {
        MethodNode mn = (MethodNode) this.node;
        final PredicateVisitor predicateVisitor = new PredicateVisitor(methodPredicate);
        mn.accept(predicateVisitor);
        return predicateVisitor.getResult();
    }

    @Override
    public boolean applyClassPredicate(ClassPredicate classPredicate) {
        ClassNode cn = (ClassNode) this.node;
        final PredicateVisitor predicateVisitor = new PredicateVisitor(classPredicate);
        cn.accept(predicateVisitor);
        return predicateVisitor.getResult();
    }

    @Override
    public boolean applyInstructionPredicate(InsnPredicate insnPredicate) {
        MethodNode mn = (MethodNode) this.node;
        boolean result = false;
        final AbstractInsnNode[] insnNodes = mn.instructions.toArray();
        for (int i = 0; i < insnNodes.length; i++) {
            result |= insnPredicate.test(i, insnNodes[i].getOpcode(), toObjectArray(insnNodes[0]));
        }
        return result;
    }

    private static Object[] toObjectArray(final AbstractInsnNode insnNode) {
        if (insnNode instanceof MethodInsnNode methodInsnNode) {
            return new Object[] {methodInsnNode.name, methodInsnNode.desc, methodInsnNode.owner, methodInsnNode.itf};
        }
        if (insnNode instanceof FieldInsnNode fieldInsnNode) {
            return new Object[] {fieldInsnNode.name, fieldInsnNode.desc, fieldInsnNode.owner};
        }
        return EMPTY;
    }
}
