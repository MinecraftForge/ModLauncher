/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformerVotingContext;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PredicateVisitor extends ClassVisitor {
    private static final int ASM_API = Opcodes.ASM9;
    
    private final ITransformerVotingContext.MethodPredicate methodPredicate;
    private final ITransformerVotingContext.FieldPredicate fieldPredicate;
    private final ITransformerVotingContext.ClassPredicate classPredicate;
    private boolean result;

    PredicateVisitor(final ITransformerVotingContext.FieldPredicate fieldPredicate) {
        super(ASM_API);
        this.methodPredicate = null;
        this.fieldPredicate = fieldPredicate;
        this.classPredicate = null;
    }

    PredicateVisitor(final ITransformerVotingContext.MethodPredicate methodPredicate) {
        super(ASM_API);
        this.methodPredicate = methodPredicate;
        this.fieldPredicate = null;
        this.classPredicate = null;
    }

    PredicateVisitor(final ITransformerVotingContext.ClassPredicate classPredicate) {
        super(ASM_API);
        this.methodPredicate = null;
        this.fieldPredicate = null;
        this.classPredicate = classPredicate;
    }

    boolean getResult() {
        return result;
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String descriptor, final String signature, final Object value) {
        result = fieldPredicate == null || fieldPredicate.test(access, name, descriptor, signature, value);
        return null;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String descriptor, final String signature, final String[] exceptions) {
        result = methodPredicate == null || methodPredicate.test(access, name, descriptor, signature, exceptions);
        return null;
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        result = classPredicate == null || classPredicate.test(version, access, name, signature, superName, interfaces);
    }

}
