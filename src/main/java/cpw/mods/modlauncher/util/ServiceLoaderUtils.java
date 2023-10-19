/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.util;

import cpw.mods.niofs.union.UnionFileSystem;

import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class ServiceLoaderUtils {
    public static <T> Stream<T> streamServiceLoader(Supplier<ServiceLoader<T>> slSupplier, Consumer<ServiceConfigurationError> errorConsumer) {
        return streamWithErrorHandling(slSupplier.get(), errorConsumer);
    }

    public static <T> Stream<T> streamWithErrorHandling(ServiceLoader<T> sl, Consumer<ServiceConfigurationError> errorConsumer) {
        return sl.stream().map(p->{
            try {
                return p.get();
            } catch (ServiceConfigurationError sce) {
                errorConsumer.accept(sce);
                return null;
            }
        }).filter(Objects::nonNull);
    }

    public static String fileNameFor(Class<?> clazz) {
        return clazz.getModule().getLayer().configuration()
                .findModule(clazz.getModule().getName())
                .flatMap(rm->rm.reference().location())
                .map(Path::of)
                .map(p -> p.getFileSystem() instanceof UnionFileSystem ufs ? ufs.getPrimaryPath() : p)
                .map(p -> p.getFileName().toString())
                .orElse("MISSING FILE");
    }
}
