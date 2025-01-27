/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.harness;

import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;

class SimpleTransformer<T> implements ITransformer<T> {
    private final Target target;
    private final Function<T, T> transformer;

    protected static <T> T assertNotNull(T value, String name) {
        if (value == null)
            throw new IllegalArgumentException(name + " can not be null");
        return value;
    }

    protected SimpleTransformer(Target target, Function<T, T> transformer) {
        this.target = assertNotNull(target, "target");
        this.transformer = assertNotNull(transformer, "transformer");
    }

    @Override
    public T transform(T input, ITransformerVotingContext context) {
        return this.transformer.apply(input);
    }

    @Override
    public @NotNull TransformerVoteResult castVote(ITransformerVotingContext context) {
        return TransformerVoteResult.YES;
    }

    @Override
    public @NotNull Set<Target> targets() {
        return Set.of(target);
    }

    @Override
    public String toString() {
        String desc = switch (target.getTargetType()) {
            case CLASS, PRE_CLASS -> target.className();
            case FIELD -> target.className() + '.' + target.elementName();
            case METHOD -> target.className() + '.' + target.elementName() + target.elementDescriptor();
        };
        return getClass().getSimpleName() + "[" + desc + "]@" + Integer.toHexString(hashCode());
    }
}
