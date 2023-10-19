/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.testjar;

import java.net.URL;

public class ResourceLoadingClass {

    public final URL resource = getClass().getResource("ResourceLoadingFile");

}