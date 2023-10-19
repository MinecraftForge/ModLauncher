/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.modlauncher.TransformList;
import cpw.mods.modlauncher.TransformStore;
import cpw.mods.modlauncher.TransformTargetLabel;
import cpw.mods.modlauncher.TransformationServiceDecorator;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import net.minecraftforge.unsafe.UnsafeHacks;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationServiceDecoratorTests {
    private final ClassNodeTransformer classNodeTransformer = new ClassNodeTransformer();
    private final MethodNodeTransformer methodNodeTransformer = new MethodNodeTransformer();

    @Test
    void testGatherTransformersNormally() throws Exception {
        UnsafeHacksUtil.hackPowermock();

        MockTransformerService mockTransformerService = new MockTransformerService() {
            @NotNull
            @Override
            public List<ITransformer> transformers() {
                return Stream.of(classNodeTransformer, methodNodeTransformer).collect(Collectors.toList());
            }
        };
        TransformStore store = new TransformStore();

        TransformationServiceDecorator sd = Whitebox.invokeConstructor(TransformationServiceDecorator.class, mockTransformerService);
        sd.gatherTransformers(store);
        EnumMap<TransformTargetLabel.LabelType, TransformList<?>> transformers = Whitebox.getInternalState(store, "transformers");
        Set<String> targettedClasses = Whitebox.getInternalState(store, "classNeedsTransforming");
        assertAll(
                () -> assertTrue(transformers.containsKey(TransformTargetLabel.LabelType.CLASS), "transformers contains class"),
                () -> assertTrue(getTransformers(transformers.get(TransformTargetLabel.LabelType.CLASS)).values().stream().flatMap(Collection::stream).allMatch(s -> Whitebox.getInternalState(s,"wrapped") == classNodeTransformer), "transformers contains classTransformer"),
                () -> assertTrue(targettedClasses.contains("cheese/Puffs"), "targetted classes contains class name cheese/Puffs"),
                () -> assertTrue(transformers.containsKey(TransformTargetLabel.LabelType.METHOD), "transformers contains method"),
                () -> assertTrue(getTransformers(transformers.get(TransformTargetLabel.LabelType.METHOD)).values().stream().flatMap(Collection::stream).allMatch(s -> Whitebox.getInternalState(s,"wrapped") == methodNodeTransformer), "transformers contains methodTransformer"),
                () -> assertTrue(targettedClasses.contains("cheesy/PuffMethod"), "targetted classes contains class name cheesy/PuffMethod")
        );
    }

    private static <T> Map<TransformTargetLabel, List<ITransformer<T>>> getTransformers(TransformList<T> list) {
        try {
            return Whitebox.invokeMethod(list, "getTransformers");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private static class ClassNodeTransformer implements ITransformer<ClassNode> {
        @NotNull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            return input;
        }

        @NotNull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @NotNull
        @Override
        public Set<Target> targets() {
            return Stream.of(Target.targetClass("cheese.Puffs")).collect(Collectors.toSet());
        }
    }

    private static class MethodNodeTransformer implements ITransformer<MethodNode> {
        @NotNull
        @Override
        public MethodNode transform(MethodNode input, ITransformerVotingContext context) {
            return input;
        }

        @NotNull
        @Override
        public TransformerVoteResult castVote(ITransformerVotingContext context) {
            return TransformerVoteResult.YES;
        }

        @NotNull
        @Override
        public Set<Target> targets() {
            return Stream.of(Target.targetMethod("cheesy.PuffMethod", "fish", "()V")).collect(Collectors.toSet());
        }
    }
}
