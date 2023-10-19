/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

import java.util.Optional;

public interface IModuleLayerManager {
    Optional<ModuleLayer> getLayer(Layer layer);

    enum Layer {
        BOOT(),
        SERVICE(BOOT),
        PLUGIN(BOOT),
        GAME(PLUGIN, SERVICE);

        private final Layer[] parent;

        Layer(final Layer... parent) {
            this.parent = parent;
        }

        public Layer[] getParent() {
            return parent;
        }
    }
}
