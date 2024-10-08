/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;

record TransformerVote<T>(TransformerVoteResult result, ITransformer<T> transformer) {}
