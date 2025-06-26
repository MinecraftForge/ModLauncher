/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.util;

import java.lang.module.FindException;
import java.lang.module.ResolutionException;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cpw.mods.jarhandling.SecureJar;

public class ModuleExceptionEnhancer {
    private static final String NAME = "[^\s]+"; //"[a-zA-Z][a-zA-Z0-9_.-]*";
    // FindException
    private static final Message NOT_FOUND = new Message(1, "^Module (?<m1>" + NAME + ") not found$");
    private static final Message NOT_FOUND_REQUIRED = new Message(2, "^Module (?<m1>" + NAME + ") not found, required by (?<m2>" + NAME + ")$");
    // ResolutionException
    private static final Message SELF_REFERENCE = new Message(2, "^Module (?<m1>" + NAME + ") reads another module named (?<m2>" + NAME + ")$");
    private static final Message MULTIPLE = new Message(2, "^Module (?<m1>" + NAME + ") reads more than one module named (?<m2>" + NAME + ")$");
    private static final Message PACKAGE_NOT_FOUND = new Message(1, "^Module (?<m1>" + NAME + ") does not read a module that exports (?<p1>" + NAME + ")$");
    private static final Message PACKAGE_CLAIMED = new Message(3, "^Module (?<m1>" + NAME + ") contains package (?<p1>" + NAME + "), module (?<m2>" + NAME + ") exports package (?<p2>" + NAME + ") to (?<m3>" + NAME  + ")$");
    private static final Message PACKAGE_SAME = new Message(3, "^Modules (?<m1>" + NAME + ") and (?<m2>" + NAME + ") export package (?<p1>" + NAME + ") to module (?<m3>" + NAME + ")$");


    private record Message(int count, Pattern pattern) {
        Message(int count, String pattern) {
            this(count, Pattern.compile(pattern));
        }
    }
    public static RuntimeException enhance(RuntimeException exception, SecureJar[] jars) {
        var message = exception.getMessage();
        if (exception instanceof FindException) {
            var names = extractNames(message, NOT_FOUND, NOT_FOUND_REQUIRED);
            var msg = buildMessage(message, names, jars);
            if (msg == null)
                return exception;
            return new FindException(msg, exception);
        } else if (exception instanceof ResolutionException) {
            final Collection<String> names;
            if (message.startsWith("Cycle detected: "))
                names = extractCycle(message);
            else
                names = extractNames(message, SELF_REFERENCE, MULTIPLE, PACKAGE_NOT_FOUND, PACKAGE_CLAIMED, PACKAGE_SAME);
            var msg = buildMessage(message, names, jars);
            if (msg == null)
                return exception;
            return new ResolutionException(msg, exception);
        }
        return exception;
    }

    private static Collection<String> extractNames(String message, Message... expected) {
        for (var pattern : expected) {
            var match = pattern.pattern.matcher(message);
            if (match.matches()) {
                var names = new TreeSet<String>();
                for (int x = 1; x <= pattern.count; x++) {
                    var name = match.group("m" + x);
                    if (name != null && !name.isEmpty()) {
                        names.add(name);
                    }
                }
                return names;
            }
        }
        return Collections.emptyList();
    }

    private static Collection<String> extractCycle(String message) {
        var ret = new TreeSet<String>();
        var names = message.substring(16);
        var parts = names.split("\\s*->\\s*");
        for (var part : parts)
            ret.add(part);
        return ret;
    }

    private static String buildMessage(String prefix, Collection<String> names, SecureJar[] jars) {
        var modules = new TreeMap<String, String>();
        for (var jar : jars) {
            if (names.isEmpty() || names.contains(jar.name())) {
                var path = jar.getPrimaryPath().getFileName();
                modules.put(jar.name(), path == null ? "null" : path.toString());
            }
        }

        if (modules.isEmpty())
            return null;

        var builder = new StringBuilder();
        builder.append(prefix).append(System.lineSeparator());
        builder.append("Impacted Modules:").append(System.lineSeparator());
        for (var entry : modules.entrySet())
            builder.append("\t- ").append(entry.getKey()).append(" (").append(entry.getValue()).append(")").append(System.lineSeparator());
        return builder.toString();
    }
}
