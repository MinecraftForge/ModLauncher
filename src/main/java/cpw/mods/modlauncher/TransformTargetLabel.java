/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;


import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import cpw.mods.modlauncher.api.ITransformer;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static cpw.mods.modlauncher.TransformTargetLabel.LabelType.*;

/**
 * Detailed targetting information
 */
public final class TransformTargetLabel {

    private final Type className;
    private final String elementName;
    private final Type elementDescriptor;
    private final LabelType labelType;
    TransformTargetLabel(ITransformer.Target target) {
        this(target.getClassName(), target.getElementName(), target.getElementDescriptor(), LabelType.valueOf(target.getTargetType().name()));
    }
    private TransformTargetLabel(String className, String elementName, String elementDescriptor, LabelType labelType) {
        this.className = Type.getObjectType(className.replace('.', '/'));
        this.elementName = elementName;
        this.elementDescriptor = elementDescriptor.length() > 0 ? Type.getMethodType(elementDescriptor) : Type.VOID_TYPE;
        this.labelType = labelType;
    }
    public TransformTargetLabel(String className, String fieldName) {
        this(className, fieldName, "", FIELD);
    }

    TransformTargetLabel(String className, String methodName, String methodDesc) {
        this(className, methodName, methodDesc, METHOD);
    }

    @Deprecated
    public TransformTargetLabel(String className) {
        this(className, "", "", CLASS);
    }

    public TransformTargetLabel(String className, LabelType type) {
        this(className, "", "", type);
        if (type.nodeType != ClassNode.class)
            throw new IllegalArgumentException("Invalid type " + type + ", must be for class!");
    }

    final Type getClassName() {
        return this.className;
    }

    public final String getElementName() {
        return this.elementName;
    }

    public final Type getElementDescriptor() {
        return this.elementDescriptor;
    }

    final LabelType getLabelType() {
        return this.labelType;
    }

    public int hashCode() {
        return Objects.hash(this.className, this.elementName, this.elementDescriptor);
    }

    @Override
    public boolean equals(Object obj) {
        try {
            TransformTargetLabel tl = (TransformTargetLabel) obj;
            return Objects.equals(this.className, tl.className)
                    && Objects.equals(this.elementName, tl.elementName)
                    && Objects.equals(this.elementDescriptor, tl.elementDescriptor);
        } catch (ClassCastException cce) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Target : " + Objects.toString(labelType) + " {" + Objects.toString(className) + "} {" + Objects.toString(elementName) + "} {" + Objects.toString(elementDescriptor) + "}";
    }

    public enum LabelType {
        FIELD(FieldNode.class),
        METHOD(MethodNode.class),
        CLASS(ClassNode.class),
        PRE_CLASS(ClassNode.class);

        private final Class<?> nodeType;

        LabelType(Class<?> nodeType) {
            this.nodeType = nodeType;
        }

        public Class<?> getNodeType() {
            return nodeType;
        }

        @SuppressWarnings("unchecked")
        public <V> TransformList<V> getFromMap(EnumMap<LabelType, TransformList<?>> transformers) {
            return get(transformers, (Class<V>) this.nodeType);
        }

        @SuppressWarnings("unchecked")
        private <V> TransformList<V> get(EnumMap<LabelType, TransformList<?>> transformers, Class<V> type) {
            return (TransformList<V>) transformers.get(this);
        }

        @SuppressWarnings("unchecked")
        public <T> Supplier<TransformList<T>> mapSupplier(EnumMap<LabelType, TransformList<?>> transformers) {
            return () -> (TransformList<T>) transformers.get(this);
        }

        /**
         * Only here for backwards compatibility with < 10.0, Nobody should ever use this.
         */
        @Deprecated(forRemoval = true, since = "10.0")
        public static List<LabelType> getTypeFor(java.lang.reflect.Type type) {
            switch (type.getTypeName()) {
                case "org.objectweb.asm.tree.FieldNode": return List.of(FIELD);
                case "org.objectweb.asm.tree.MethodNode": return List.of(METHOD);
                case "org.objectweb.asm.tree.ClassNode": return List.of(CLASS, PRE_CLASS);
            }
            return Collections.emptyList();
        }
    }
}
