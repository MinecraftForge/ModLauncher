/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import net.minecraftforge.bootstrap.api.BootstrapEntryPoint;

/**
 * Internal Service so that Bootstrap can find us.
 * Considered internal API, so may break at any time do not reference.
 */
public class BootstrapEntry implements BootstrapEntryPoint {
    @Override
    public void main(String... strings) {
        Launcher.main(strings);
    }
}
