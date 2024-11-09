/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/**
 * System environment. Global properties relevant to the current environment and lookups to find global artifacts
 * in the environment.
 */
public interface IEnvironment {
    /**
     * Get a property from the Environment
     * @param key to find
     * @param <T> Type of key
     * @return the value
     */
    <T> Optional<T> getProperty(TypesafeMap.Key<T> key);

    /**
     * Compute a new value for insertion into the environment, if not already present.
     *
     * @param key to insert
     * @param valueFunction the function to compute a value
     * @param <T> Type of key
     * @return The value of the key
     */
    <T> T computePropertyIfAbsent(TypesafeMap.Key<T> key, final Function<? super TypesafeMap.Key<T>, ? extends T> valueFunction);

    /**
     * Compute a new value for insertion into the environment, if not already present.
     *
     * @param key to insert
     * @param valueSupplier the supplier of a value
     * @param <T> Type of key
     * @return The value of the key
     */
    default <T> T computePropertyIfAbsent(TypesafeMap.Key<T> key, final Supplier<? extends T> valueSupplier) {
        return computePropertyIfAbsent(key, k -> valueSupplier.get());
    }

    /**
     * Insert a value into the environment if not already present.
     * @param key to insert
     * @param value the value to insert
     * @return The value of the key
     * @param <T> Type of key
     */
    default <T> T putPropertyIfAbsent(TypesafeMap.Key<T> key, T value) {
        return computePropertyIfAbsent(key, k -> value);
    }

    /**
     * Find the named {@link ILaunchPluginService}
     *
     * @param name name to lookup
     * @return the launch plugin
     */
    Optional<ILaunchPluginService> findLaunchPlugin(String name);

    /**
     * Find the named {@link ILaunchHandlerService}
     *
     * @param name name to lookup
     * @return the launch handler
     */
    Optional<ILaunchHandlerService> findLaunchHandler(String name);

    Optional<IModuleLayerManager> findModuleLayerManager();

    /**
     * Find the naming translation for the targetMapping.
     * @param targetMapping the name of the mapping to lookup
     * @return a function mapping names from the current naming domain to the requested one, if available
     */
    Optional<BiFunction<INameMappingService.Domain, String, String>> findNameMapping(String targetMapping);

    final class Keys {
        /**
         * Version passed in through arguments
         */
        public static final Supplier<TypesafeMap.Key<String>> VERSION = buildKey("version", String.class);
        /**
         * The identified game directory (usually passed as an argument)
         */
        public static final Supplier<TypesafeMap.Key<Path>> GAMEDIR = buildKey("gamedir", Path.class);
        /**
         * The identified assets directory (usually passed as an argument)
         */
        public static final Supplier<TypesafeMap.Key<Path>> ASSETSDIR = buildKey("assetsdir", Path.class);
        /**
         * The UUID of the player on the client
         */
        public static final Supplier<TypesafeMap.Key<String>> UUID = buildKey("uuid", String.class);
        /**
         * The name of the identified launch target (passed as an argument)
         */
        public static final Supplier<TypesafeMap.Key<String>> LAUNCHTARGET = buildKey("launchtarget", String.class);
        /**
         * The naming scheme in use. Populated at startup. See: {@link INameMappingService}
         */
        public static final Supplier<TypesafeMap.Key<String>> NAMING = buildKey("naming", String.class);
        /**
         * The audit trail for transformers applied to a class. See {@link ITransformerAuditTrail}
         */
        public static final Supplier<TypesafeMap.Key<ITransformerAuditTrail>> AUDITTRAIL = buildKey("audittrail", ITransformerAuditTrail.class);
        /**
         * A simple List of Maps for Mod data. Map keys should include a "name" and "description". "file" and "type" are
         * populated automatically, as is "name".
         */
        public static final Supplier<TypesafeMap.Key<List<Map<String,String>>>> MODLIST = buildKey("modlist", List.class);
        /**
         * The specification version for ModLauncher.
         */
        public static final Supplier<TypesafeMap.Key<String>> MLSPEC_VERSION = buildKey("mlspecVersion", String.class);
        /**
         * The implementation version for ModLauncher.
         */
        public static final Supplier<TypesafeMap.Key<String>> MLIMPL_VERSION = buildKey("mlimplVersion", String.class);
        /**
         * True if we can compute secured JAR state. JVMs < 8.0.61 do not have this feature because reasons
         */
        public static final Supplier<TypesafeMap.Key<Boolean>> SECURED_JARS_ENABLED = buildKey("securedJarsEnabled", Boolean.class);
    }


    static <T> Supplier<TypesafeMap.Key<T>> buildKey(String name, Class<? super T> clazz) {
        return new TypesafeMap.KeyBuilder<>(name, clazz, IEnvironment.class);
    }
}
