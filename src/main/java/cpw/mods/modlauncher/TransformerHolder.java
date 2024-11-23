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

public record TransformerHolder<T>(ITransformer<T> delegate, ITransformationService owner) implements ITransformer<T> {
    @NotNull
    @Override
    public T transform(final T input, final ITransformerVotingContext context) {
        return delegate.transform(input, context);
    }

    @NotNull
    @Override
    public TransformerVoteResult castVote(final ITransformerVotingContext context) {
        return delegate.castVote(context);
    }

    @NotNull
    @Override
    public Set<Target> targets() {
        return delegate.targets();
    }

    @Override
    public String[] labels() {
        return delegate.labels();
    }
}
