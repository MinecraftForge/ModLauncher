/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

import java.util.List;
import java.util.Optional;

public interface IModuleLayerManager {
    Optional<ModuleLayer> getLayer(Layer layer);

    enum Layer {
        BOOT(),
        SERVICE(BOOT),
        PLUGIN(BOOT),
        GAME(PLUGIN, SERVICE);

        private final List<Layer> parents;

        Layer(Layer parent) {
            this.parents = List.of(parent);
        }

        Layer(Layer... parent) {
            this.parents = List.of(parent);
        }

        /** Returning a raw array is unsafe, it used to return the backing array directly which would allow others to screw with it. */
        @Deprecated(forRemoval = true, since = "10.1")
        public Layer[] getParent() {
            return this.parents.toArray(Layer[]::new);
        }

        /** Returns a potentially empty, immutable list of parent layers. */
        public List<Layer> getParents() {
            return this.parents;
        }
    }
}
