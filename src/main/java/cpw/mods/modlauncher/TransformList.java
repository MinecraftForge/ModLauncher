/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cpw.mods.modlauncher.api.ITransformer;

/**
 * Holds onto a specific list of transformers targeting a particular node type
 */
@SuppressWarnings("WeakerAccess")
public class TransformList<T> {
    private final Map<TransformTargetLabel, List<ITransformer<T>>> transformers = new ConcurrentHashMap<>();

    TransformList(/*parameter so generic can be inferred*/Class<T> nodeType) {
    }

    void addTransformer(TransformTargetLabel targetLabel, ITransformer<T> transformer) {
        // thread safety - compute if absent to insert the list
        transformers.computeIfAbsent(targetLabel, v -> new ArrayList<>());
        // thread safety - compute if present to mutate the list under the protection of the CHM
        transformers.computeIfPresent(targetLabel, (k,l)-> { l.add(transformer); return l;});
    }

    List<ITransformer<T>> getTransformersForLabel(TransformTargetLabel label) {
        // thread safety - compute if absent to insert the list
        return transformers.computeIfAbsent(label, v-> new ArrayList<>());
    }
}
