/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

/**
 * Indicate an incompatible environment to the modlauncher system.
 */
public class IncompatibleEnvironmentException extends Exception {
    private static final long serialVersionUID = -5825852226061088576L;

    public IncompatibleEnvironmentException(String message) {
        super(message);
    }
}
