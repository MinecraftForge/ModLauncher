/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import org.jetbrains.annotations.Nullable;
import java.net.URL;
import java.security.*;
import java.util.HashMap;
import java.util.Map;


public class ProtectionDomainHelper {
    private static final Map<URL, CodeSource> csCache = new HashMap<>();
    public static CodeSource createCodeSource(@Nullable final URL url, final CodeSigner[] signers) {
        synchronized (csCache) {
            return csCache.computeIfAbsent(url, u->new CodeSource(url, signers));
        }
    }

    private static final Map<CodeSource, ProtectionDomain> pdCache = new HashMap<>();
    public static ProtectionDomain createProtectionDomain(CodeSource codeSource, ClassLoader cl) {
        synchronized (pdCache) {
            return pdCache.computeIfAbsent(codeSource, cs->{
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return new ProtectionDomain(codeSource, perms, cl, null);
            });
        }
    }

    public static boolean canHandleSecuredJars() {
        return true;
    }
}
