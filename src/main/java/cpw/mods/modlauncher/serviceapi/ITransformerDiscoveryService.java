/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.serviceapi;

import cpw.mods.modlauncher.api.NamedPath;

import java.nio.file.Path;
import java.util.List;

/**
 * Called early in setup, to allow pluggable "discovery" of additional transformer services.
 * FML uses this to identify transformers in the mods directory (e.g. Optifine) for loading into ModLauncher.
 */
public interface ITransformerDiscoveryService {
    /**
     * Return a list of additional paths to be added to transformer service discovery during loading.
     * @param gameDirectory The root game directory
     * @return The list of services
     */
    List<NamedPath> candidates(final Path gameDirectory);

    /**
     * Return a list of additional paths to be added to transformer service discovery during loading.
     *
     * Defaults to calling {@link #candidates(Path)}
     *
     * @param gameDirectory The root game directory
     * @param launchTarget The launch target
     * @return The list of services
     */
    default List<NamedPath> candidates(final Path gameDirectory, final String launchTarget) {
        return candidates(gameDirectory);
    }

    /**
     * An opportunity for early transformer services to do something once the service layer has
     * been built.
     *
     * @param launchTarget The launch target
     * @param arguments The full command arguments to the game
     */
    default void earlyInitialization(final String launchTarget, final String[] arguments) {

    }
}
