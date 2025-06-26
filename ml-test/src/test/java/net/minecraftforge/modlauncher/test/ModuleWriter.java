/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public class ModuleWriter {
    public static void write(Path path, ModuleDescriptor desc) throws IOException {
        try (var out = Files.newOutputStream(path)) {
            out.write(bytes(desc));
        }
    }

    public static byte[] bytes(ModuleDescriptor desc) {
        var writer = new ClassWriter(0);

        writer.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null);

        var module = writer.visitModule(desc.name(), flags(desc), version(desc.version(), desc.rawVersion()));

        desc.mainClass().ifPresent(module::visitMainClass);

        for (var pkg : sorted(desc.packages(), Function.identity()))
            module.visitPackage(binary(pkg));

        for (var req : sorted(desc.requires(), Requires::name))
            module.visitRequire(req.name(), flags(req), version(req.compiledVersion(), req.rawCompiledVersion()));

        for (var exp : sorted(desc.exports(), Exports::source))
            module.visitExport(binary(exp.source()), flags(exp), array(exp.targets()));

        for (var open : sorted(desc.opens(), Opens::source))
            module.visitOpen(binary(open.source()), flags(open), array(open.targets()));

        for (var uses : sorted(desc.uses(), Function.identity()))
            module.visitUse(binary(uses));

        for (var provide : sorted(desc.provides(), Provides::service)) {
            var providers = new ArrayList<String>();
            for (var provider : provide.providers())
                providers.add(binary(provider));

            module.visitProvide(provide.service(), array(providers));
        }

        module.visitEnd();

        return writer.toByteArray();
    }

    private static int flags(ModuleDescriptor desc) {
        int access = 0;
        for (var flag : desc.modifiers()) {
            var mask = switch (flag) {
                case SYNTHETIC -> Opcodes.ACC_SYNTHETIC;
                case MANDATED -> Opcodes.ACC_MANDATED;
                case OPEN -> Opcodes.ACC_OPEN;
                case AUTOMATIC -> 0;
            };
            access |= mask;
        }
        return access;
    }

    private static int flags(ModuleDescriptor.Requires req) {
        int access = 0;
        for (var flag : req.modifiers()) {
            var mask = switch (flag) {
                case TRANSITIVE -> Opcodes.ACC_TRANSITIVE;
                case STATIC -> Opcodes.ACC_STATIC_PHASE;
                case SYNTHETIC -> Opcodes.ACC_SYNTHETIC;
                case MANDATED -> Opcodes.ACC_MANDATED;
            };
            access |= mask;
        }
        return access;
    }

    private static int flags(ModuleDescriptor.Exports export) {
        int access = 0;
        for (var flag : export.modifiers()) {
            var mask = switch (flag) {
                case SYNTHETIC -> Opcodes.ACC_SYNTHETIC;
                case MANDATED -> Opcodes.ACC_MANDATED;
            };
            access |= mask;
        }
        return access;
    }

    private static int flags(ModuleDescriptor.Opens export) {
        int access = 0;
        for (var flag : export.modifiers()) {
            var mask = switch (flag) {
                case SYNTHETIC -> Opcodes.ACC_SYNTHETIC;
                case MANDATED -> Opcodes.ACC_MANDATED;
            };
            access |= mask;
        }
        return access;
    }

    private static String[] array(Collection<String> lst) {
        return lst.stream().toArray(String[]::new);
    }

    private static String binary(String cls) {
        return cls.replace('.', '/');
    }

    private static String version(Optional<Version> ver, Optional<String> str) {
        var version = ver.map(Version::toString).orElse(null);
        if (version == null)
            return str.orElse(null);
        return version;
    }

    private static <T> List<T> sorted(Collection<T> data, Function<T, String> toString) {
        var ret = new ArrayList<T>();
        ret.addAll(data);
        Collections.sort(ret, (a, b) -> toString.apply(a).compareTo(toString.apply(b)));
        return ret;
    }
}
