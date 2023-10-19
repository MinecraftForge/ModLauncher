/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.*;
import joptsimple.*;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Test Launcher Service
 */
public class MockTransformerService implements ITransformationService {
    private ArgumentAcceptingOptionSpec<String> modsList;
    private ArgumentAcceptingOptionSpec<Integer> modlists;
    private List<String> modList;
    private String state;

    @NotNull
    @Override
    public String name() {
        return "test";
    }

    @Override
    public void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {
        modsList = argumentBuilder.apply("mods", "CSV list of mods to load").withRequiredArg().withValuesSeparatedBy(",").ofType(String.class);
    }

    @Override
    public void argumentValues(OptionResult result) {
        modList = result.values(modsList);
    }

    @Override
    public void initialize(IEnvironment environment) {
        state = "INITIALIZED";
    }

    @Override
    public void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException {

    }

    @Override
    public List<Resource> beginScanning(IEnvironment environment) {
        var jars = getTestJars();
        if (jars.isEmpty())
            return List.of();
        return List.of(new Resource(IModuleLayerManager.Layer.PLUGIN, jars));
    }

    @Override
    public List<Resource> completeScan(IModuleLayerManager layerManager) {
        var jars = getTestJars();
        if (jars.isEmpty())
            return List.of();
        return List.of(new Resource(IModuleLayerManager.Layer.GAME, jars));
    }

    private static List<SecureJar> getTestJars() {
        var harness = System.getProperty("test.harness");
        if (harness == null)
            return List.of();

        var ret = new ArrayList<SecureJar>();
        for (var strings : harness.split(",")) {
            var paths = Arrays.stream(strings.split("\0")).map(p -> Path.of(p)).toArray(Path[]::new);
            var jar = SecureJar.from(paths);
            ret.add(jar);
        }

        return ret;
    }

    @NotNull
    @Override
    public List<ITransformer> transformers() {
        return Stream.of(new ClassNodeTransformer(modList)).collect(Collectors.toList());
    }

    static class ClassNodeTransformer implements ITransformer<ClassNode> {
        private final List<String> classNames;

        ClassNodeTransformer(List<String> classNames) {
            this.classNames = classNames;
        }

        @NotNull
        @Override
        public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
            FieldNode fn = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "testfield", "Ljava/lang/String;", null, "CHEESE!");
            input.fields.add(fn);
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
            return classNames.stream().map(Target::targetClass).collect(Collectors.toSet());
        }
    }

}
