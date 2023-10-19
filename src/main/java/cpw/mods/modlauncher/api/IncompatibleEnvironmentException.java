/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

/**
 * Indicate an incompatible environment to the modlauncher system.
 */
public class IncompatibleEnvironmentException extends Exception {
    public IncompatibleEnvironmentException(String message) {
        super(message);
    }
}
