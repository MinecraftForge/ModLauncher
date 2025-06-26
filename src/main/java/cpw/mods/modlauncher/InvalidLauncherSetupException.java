/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

public class InvalidLauncherSetupException extends IllegalStateException {
    private static final long serialVersionUID = 6030083272490759567L;

    InvalidLauncherSetupException(final String s) {
        super(s);
    }
}
