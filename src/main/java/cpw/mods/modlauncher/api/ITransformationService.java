/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.ArgumentHandler;
import cpw.mods.modlauncher.internal.GuardedOptionResult;
import joptsimple.*;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Users who wish to provide a mod service which plugs into this API
 * should implement this interface, and provide a {@link java.util.ServiceLoader}
 * configuration file pointing at their implementation.
 */
public interface ITransformationService {
    /**
     * The name of this mod service. It will be used throughout the system. It should be lower case,
     * the first character should be alphanumeric and it should only consist of standard alphanumeric
     * characters
     *
     * @return the name of the mod service
     */
    @NotNull
    String name();

    /**
     * Define command line arguments for your mod service. These will be prefixed by your {@link #name()}
     * to prevent collisions.
     *
     * @param argumentBuilder a function mapping name, description to a set of JOptSimple properties for that argument
     */
    default void arguments(BiFunction<String, String, OptionSpecBuilder> argumentBuilder) {}

    /**
     * Access the values of your arguments defined in {@link #arguments(BiFunction)}
     * @param option The result view of the parsed arguments
     */
    default void argumentValues(OptionResult option) {}

    /**
     * Initialize your service.
     *
     * @param environment environment - query state from here to determine viability
     */
    void initialize(IEnvironment environment);

    record Resource(IModuleLayerManager.Layer target, List<SecureJar> resources) {}
    /**
     * Scan for mods (but don't classload them), identify metadata that might drive
     * game functionality, return list of elements and target module layer (One of PLUGIN or GAME)
     *
     * @param environment environment
     */
    default List<Resource> beginScanning(IEnvironment environment) {
        return List.of();
    }

    default List<Resource> completeScan(IModuleLayerManager layerManager) {
        return List.of();
    }

    /**
     * Load your service. Called immediately on loading with a list of other services found.
     * Use to identify and immediately indicate incompatibilities with other services, and environment
     * configuration. This is to try and immediately abort a guaranteed bad environment.
     *
     * @param env           environment - query state from here
     * @param otherServices other services loaded with the system
     * @throws IncompatibleEnvironmentException if there is an incompatibility detected. Identify specifics in
     *                                          the exception message
     */
    void onLoad(IEnvironment env, Set<String> otherServices) throws IncompatibleEnvironmentException;

    /**
     * The {@link ITransformer} is the fundamental operator of the system.
     *
     * @return A list of transformers for your ITransformationService. This is called after {@link #onLoad(IEnvironment, Set)}
     * and {@link #initialize(IEnvironment)}, so you can return an appropriate Transformer set for the environment
     * you find yourself in.
     */
    @NotNull
    List<ITransformer> transformers();

    /**
     * A guarded partial view of {@link OptionSet} that only allows access to options intended for your service
     * @see ITransformationService#arguments(BiFunction)
     * @see ITransformationService#argumentValues(OptionResult)
     */
    sealed interface OptionResult permits GuardedOptionResult {
        /**
         * @see OptionSet#valueOf(OptionSpec)
         */
        <V> V value(OptionSpec<V> option);

        /**
         * @see OptionSet#valuesOf(OptionSpec)
         */
        @NotNull
        <V> List<V> values(OptionSpec<V> options);
    }
}
