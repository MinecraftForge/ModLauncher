/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;

import org.jetbrains.annotations.NotNull;
import java.util.Set;

public class TransformerHolder<T> implements ITransformer<T> {
    private final ITransformer<T> wrapped;
    private final ITransformationService owner;

    public TransformerHolder(final ITransformer<T> wrapped, ITransformationService owner) {
        this.wrapped = wrapped;
        this.owner = owner;
    }

    @NotNull
    @Override
    public T transform(final T input, final ITransformerVotingContext context) {
        return wrapped.transform(input, context);
    }

    @NotNull
    @Override
    public TransformerVoteResult castVote(final ITransformerVotingContext context) {
        return wrapped.castVote(context);
    }

    @NotNull
    @Override
    public Set<Target> targets() {
        return wrapped.targets();
    }

    @Override
    public String[] labels() {
        return wrapped.labels();
    }

    public ITransformationService owner() {
        return owner;
    }
}
