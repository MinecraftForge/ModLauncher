/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

import java.nio.file.Path;

public record NamedPath(String name, Path... paths) {
}
