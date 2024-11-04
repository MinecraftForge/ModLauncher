/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import org.apache.logging.log4j.*;

final class LogMarkers {
    static final Marker MODLAUNCHER = MarkerManager.getMarker("MODLAUNCHER");
//    static final Marker CLASSLOADING = MarkerManager.getMarker("CLASSLOADING").addParents(MODLAUNCHER);
    static final Marker LAUNCHPLUGIN = MarkerManager.getMarker("LAUNCHPLUGIN").addParents(MODLAUNCHER);

    private LogMarkers() {}
}
