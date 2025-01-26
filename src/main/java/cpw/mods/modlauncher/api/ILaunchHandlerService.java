/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

/**
 * A singleton instance of this is loaded by the system to designate the launch target
 */
public interface ILaunchHandlerService {
    String name();

    void launchService(String[] arguments, ModuleLayer gameLayer);
}
