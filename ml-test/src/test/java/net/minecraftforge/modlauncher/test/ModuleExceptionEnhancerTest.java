/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package net.minecraftforge.modlauncher.test;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.util.ModuleExceptionEnhancer;
import net.minecraftforge.securemodules.SecureModuleFinder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ModuleExceptionEnhancerTest {
    private static final SecureJar[] EMPTY = new SecureJar[0];
    private static SecureJar SIMPLE;
    private static SecureJar SIMPLE2;
    private static SecureJar REQUIES_A;
    private static SecureJar CYCLE_A;
    private static SecureJar CYCLE_B;
    private static SecureJar USES_WITHOUT_READ;
    private static SecureJar PACKAGE_OVERLAP;
    private static SecureJar PACKAGE_OVERLAP_TRANSITIVE;

    @BeforeAll
    public static void setUpBeforeClass(@TempDir Path tempDir) throws Exception {
        SIMPLE = makeJar(tempDir, "simple", b -> b
            .exports("pkg.a")
        );
        SIMPLE2 = makeJar(tempDir, "simple2", b -> b
            .exports("pkg.a")
        );

        REQUIES_A = makeJar(tempDir, "requires.simple", b -> b
            .requires("simple")
        );

        CYCLE_A = makeJar(tempDir, "cycle.a", b -> b
            .requires(EnumSet.of(Requires.Modifier.TRANSITIVE), "cycle.b")
        );

        CYCLE_B = makeJar(tempDir, "cycle.b", b -> b
            .requires("cycle.a")
        );

        USES_WITHOUT_READ = makeJar(tempDir, "use.without.read", b -> b
            .provides("service.pkg.name", List.of("my.impl"))
        );

        PACKAGE_OVERLAP = makeJar(tempDir, "pkg.overlap", b -> b
            .requires("simple")
            .exports("pkg.a")
        );

        PACKAGE_OVERLAP_TRANSITIVE = makeJar(tempDir, "pkg.reader", b -> b
            .requires("simple")
            .requires("simple2")
        );
    }

    private static SecureJar makeJar(Path path, String name, Consumer<ModuleDescriptor.Builder> consumer) throws Exception {
        return makeJar(path, name, name, consumer);
    }

    private static SecureJar makeJar(Path path, String folder, String name, Consumer<ModuleDescriptor.Builder> consumer) throws Exception {
        var builder = ModuleDescriptor.newOpenModule(name);
        consumer.accept(builder);
        var desc = builder.build();
        var root = path.resolve(folder + ".jar");
        Files.createDirectories(root);
        ModuleWriter.write(root.resolve("module-info.class"), desc);
        return SecureJar.from(root);
    }

    private static RuntimeException resolve(SecureJar... jars) {
        try {
            var finder = SecureModuleFinder.of(jars);
            var names = Arrays.stream(jars).map(SecureJar::name).toList();
            Configuration.resolve(finder, List.of(Configuration.empty()), ModuleFinder.ofSystem(), names);
            return fail("Expected exception to be thrown");
        } catch (RuntimeException e) {
            return e;
        }
    }

    @Test
    void not_found() {
        try {
            Configuration.resolve(SecureModuleFinder.of(), List.of(Configuration.empty()), ModuleFinder.of(), List.of("not.found"));
            fail("Expected exception to be thrown");
        } catch (RuntimeException e) {
            var newException = ModuleExceptionEnhancer.enhance(e, EMPTY);
            assertTrue(e == newException, "Enhanced exception with no extra info: " + e.getMessage());
            assertEquals("Module not.found not found", e.getMessage(), "Unexpected exception message: " + e.getMessage());
        }
    }

    @Test
    void not_found_required() {
        var jars = new SecureJar[] { REQUIES_A };
        var e = resolve(jars);
        var newException = ModuleExceptionEnhancer.enhance(e, jars);
        assertFalse(e == newException, "Failed to enhance exception: " + e.getMessage());
        var msg = newException.getMessage();
        assertTrue(msg.contains("Module simple not found, required by requires.simple"), "Vanilla message not found in enhanced exception: " + msg);
        assertTrue(msg.contains("requires.simple.jar"), "Path not found in message: " + msg);
    }

    @Test
    void cycle() {
        var jars = new SecureJar[] { CYCLE_A, CYCLE_B };
        var e = resolve(jars);
        var newException = ModuleExceptionEnhancer.enhance(e, jars);
        assertFalse(e == newException, "Failed to enhance exception: " + e.getMessage());
        var msg = newException.getMessage();
        assertTrue(msg.contains("Cycle detected: cycle.a -> cycle.b -> cycle.a"), "Vanilla message not found in enhanced exception: " + msg);
        assertTrue(msg.contains("cycle.a.jar"), "Cycle A path not found in message: " + msg);
        assertTrue(msg.contains("cycle.b.jar"), "Cycle B path not found in message: " + msg);
    }

    @Test
    void uses_without_read() {
        var jars = new SecureJar[] { USES_WITHOUT_READ };
        var e = resolve(jars);
        var newException = ModuleExceptionEnhancer.enhance(e, jars);
        assertFalse(e == newException, "Failed to enhance exception: " + e.getMessage());
        var msg = newException.getMessage();
        assertTrue(msg.contains("Module use.without.read does not read a module that exports service.pkg"), "Vanilla message not found in enhanced exception: " + msg);
        assertTrue(msg.contains("use.without.read.jar"), "Path not found in message: " + msg);
    }

    @Test
    void package_overlap() {
        var jars = new SecureJar[] { SIMPLE, PACKAGE_OVERLAP };
        var e = resolve(jars);
        var newException = ModuleExceptionEnhancer.enhance(e, jars);
        assertFalse(e == newException, "Failed to enhance exception: " + e.getMessage());
        var msg = newException.getMessage();
        assertTrue(msg.contains("Module pkg.overlap contains package pkg.a, module simple exports package pkg.a to pkg.overlap"), "Vanilla message not found in enhanced exception: " + msg);
        assertTrue(msg.contains("simple.jar"), "Simple path not found in message: " + msg);
        assertTrue(msg.contains("pkg.overlap.jar"), "Overlap path not found in message: " + msg);
    }

    @Test
    void reader_package_overlap() {
        var jars = new SecureJar[] { SIMPLE, SIMPLE2, PACKAGE_OVERLAP_TRANSITIVE };
        var e = resolve(jars);
        var newException = ModuleExceptionEnhancer.enhance(e, jars);
        assertFalse(e == newException, "Failed to enhance exception: " + e.getMessage());
        var msg = newException.getMessage();
        // The order is random (uses a hash set with non-determinstic hashes) so we check for both
        assertTrue(
            msg.contains("Modules simple and simple2 export package pkg.a to module pkg.reader") ||
            msg.contains("Modules simple2 and simple export package pkg.a to module pkg.reader"),
            "Vanilla message not found in enhanced exception: " + msg
        );
        assertTrue(msg.contains("simple.jar"), "Simple path not found in message: " + msg);
        assertTrue(msg.contains("simple2.jar"), "Simple2 path not found in message: " + msg);
        assertTrue(msg.contains("pkg.reader.jar"), "Reader path not found in message: " + msg);
    }

    // I don't know how to trigger:
    //  resolveFail("Module %s reads another module named %s", name1, name1);
    //  resolveFail("Module %s reads more than one module named %s", name1, name2);

}
