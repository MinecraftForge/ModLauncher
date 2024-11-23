/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformingClassLoaderBuilder;

import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static cpw.mods.modlauncher.api.LambdaExceptionUtils.rethrowFunction;

final class TransformingClassLoaderBuilder implements ITransformingClassLoaderBuilder {
    private final List<Path> transformationPaths = new ArrayList<>();
    private Function<String, Enumeration<URL>> resourcesLocator;

    URL[] getSpecialJarsAsURLs() {
        return transformationPaths.stream().map(rethrowFunction(path->path.toUri().toURL())).toArray(URL[]::new);
    }

    @Override
    public void addTransformationPath(final Path path) {
        transformationPaths.add(path);
    }

    @Override
    public void setClassBytesLocator(final Function<String, Optional<URL>> additionalClassBytesLocator) {
        this.resourcesLocator = EnumerationHelper.fromOptional(additionalClassBytesLocator);
    }

    @Override
    public void setResourceEnumeratorLocator(final Function<String, Enumeration<URL>> resourceEnumeratorLocator) {
        this.resourcesLocator = resourceEnumeratorLocator;
    }

    Function<String, Enumeration<URL>> getResourceEnumeratorLocator() {
        return this.resourcesLocator != null ? this.resourcesLocator : input -> Collections.emptyEnumeration();
    }
}
