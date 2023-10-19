/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import java.util.function.Consumer;

public class BootstrapLaunchConsumer implements Consumer<String[]> {
    @Override
    public void accept(final String[] strings) {
        Launcher.main(strings);
    }
}
