/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.*;

import java.util.*;

import static cpw.mods.modlauncher.LogMarkers.*;

/**
 * Transformer store - holds all the transformers
 */
public class TransformStore {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Set<String> classNeedsTransforming = new HashSet<>();
    private final EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers;

    public TransformStore() {
        transformers = new EnumMap<>(TransformTargetLabel.LabelType.class);
        for (TransformTargetLabel.LabelType type : TransformTargetLabel.LabelType.values())
            transformers.put(type, new TransformList<>(type.getNodeType()));
    }

    List<ITransformer<FieldNode>> getTransformersFor(String className, FieldNode field) {
        TransformTargetLabel tl = new TransformTargetLabel(className, field.name);
        TransformList<FieldNode> transformerlist = TransformTargetLabel.LabelType.FIELD.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    List<ITransformer<MethodNode>> getTransformersFor(String className, MethodNode method) {
        TransformTargetLabel tl = new TransformTargetLabel(className, method.name, method.desc);
        TransformList<MethodNode> transformerlist = TransformTargetLabel.LabelType.METHOD.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    List<ITransformer<ClassNode>> getTransformersFor(String className, TransformTargetLabel.LabelType classType) {
        TransformTargetLabel tl = new TransformTargetLabel(className, classType);
        TransformList<ClassNode> transformerlist = classType.getFromMap(this.transformers);
        return transformerlist.getTransformersForLabel(tl);
    }

    @SuppressWarnings("unchecked")
    <T> void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer, ITransformationService service) {
        LOGGER.debug(MODLAUNCHER,"Adding transformer {} to {}", () -> transformer, () -> targetLabel);
        classNeedsTransforming.add(targetLabel.getClassName().getInternalName());
        final TransformList<T> transformList = (TransformList<T>) this.transformers.get(targetLabel.getLabelType());
        transformList.addTransformer(targetLabel, new TransformerHolder<>(transformer, service));
    }

    /**
     * Requires internal class name (using '/' instead of '.')
     */
    boolean needsTransforming(String internalClassName) {
        return classNeedsTransforming.contains(internalClassName);
    }
}
