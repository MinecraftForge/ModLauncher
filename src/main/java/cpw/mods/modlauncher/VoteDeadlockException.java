/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import java.util.*;

/**
 * Exception thrown when a vote impasse occurs
 */
public class VoteDeadlockException extends RuntimeException {
    private static final long serialVersionUID = 181787525265522023L;

    <T> VoteDeadlockException(List<TransformerVote<T>> votes, Class<?> aClass) {
    }
}
