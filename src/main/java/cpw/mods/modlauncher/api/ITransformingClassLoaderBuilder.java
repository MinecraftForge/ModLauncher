/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Function;
import java.util.jar.Manifest;

public interface ITransformingClassLoaderBuilder {
    void addTransformationPath(Path path);

    void setClassBytesLocator(Function<String, Optional<URL>> additionalClassBytesLocator);

    void setResourceEnumeratorLocator(Function<String, Enumeration<URL>> resourceEnumeratorLocator);
}
