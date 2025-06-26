/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import java.util.*;

/**
 * Exception thrown when a voter rejects the entire configuration
 */
public class VoteRejectedException extends RuntimeException {
    private static final long serialVersionUID = 7693823673309984399L;

    <T> VoteRejectedException(List<TransformerVote<T>> votes, Class<?> aClass) {
    }
}
