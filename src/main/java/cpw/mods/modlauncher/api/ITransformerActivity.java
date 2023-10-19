/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

public interface ITransformerActivity {
    /**
     * reason will be set to this value when TransformerClassWriter is trying to compute frames by loading the class
     * hierarchy for a class. No real classloading will be done when this reason is submitted.
     */
    String COMPUTING_FRAMES_REASON = "computing_frames";

    /**
     * reason will be set to this value when we're attempting to actually classload the class
     */
    String CLASSLOADING_REASON = "classloading";

    String[] getContext();

    Type getType();

    String getActivityString();

    enum Type {
        PLUGIN("pl"), TRANSFORMER("xf"), REASON("re");

        private final String label;

        Type(final String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
