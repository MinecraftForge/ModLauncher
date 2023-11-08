/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.util;

import java.nio.file.Path;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Deprecated(forRemoval = true, since = "10.1")
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
        var module = clazz.getModule();
        var ref = module.getLayer().configuration().findModule(module.getName());
        if (!ref.isPresent())
            return "MISSING FILE";
        var location = ref.get().reference().location();
        if (!location.isPresent())
            return "MISSING FILE";

        var path = Path.of(location.get());
        return path.toString();
    }
}
