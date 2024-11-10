/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.log;

import joptsimple.internal.Strings;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.pattern.*;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformerAuditTrail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Started as a copy of {@link org.apache.logging.log4j.core.pattern.ExtendedThrowablePatternConverter} because
 * there is no mechanism to hook additional data into that class, which is very rubbish.
 */
@Plugin(name = "TransformingThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "tEx" })
public class TransformingThrowablePatternConverter extends ThrowablePatternConverter {
    // Logger needs a suffix to trigger our 'enhancement' so pass in something that isn't empty.
    @SuppressWarnings("unused")
    private static final String SUFFIXFLAG = "{}";
    /**
     * @param name    Name of converter.
     * @param style   CSS style for output.
     * @param options options, may be null.
     * @param config config.
     */
    protected TransformingThrowablePatternConverter(final Configuration config, final String[] options) {
        super("TransformingThrowable", "throwable", options, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(@SuppressWarnings("exports") final LogEvent event, final StringBuilder buf) {
        var proxy = event.getThrownProxy();
        if (!options.anyLines())
            return;

        if (proxy == null) {
            super.format(event, buf);
            return;
        }

        var len = buf.length();
        if (len > 0 && !Character.isWhitespace(buf.charAt(len - 1)))
            buf.append(' ');

        var trail = Optional.ofNullable(Launcher.INSTANCE)
            .map(Launcher::environment)
            .flatMap(env -> env.getProperty(IEnvironment.Keys.AUDITTRAIL.get()))
            .orElse(null);

        var nl = options.getSeparator();
        var renderer = options.getTextRenderer();
        var suffix = "";
        //renderer = new ExtraDataTextRenderer(renderer, trail);
        //suffix = SUFFIXFLAG;
        proxy.formatExtendedStackTraceTo(buf, options.getIgnorePackages(), renderer, suffix, nl);

        if (trail == null)
            return;

        final Map<String, List<String>> audit = new TreeMap<>();
        buildAuditMap(trail, audit, proxy);

        line(buf, "Transformer Audit:");
        for (var cls : audit.keySet()) {
            line(buf, "  ", cls);
            for (var line : audit.get(cls))
                line(buf, "    ", line);
        }
    }

    private void line(StringBuilder sb, String... parts) {
        var renderer = options.getTextRenderer();
        for (var pt : parts)
            renderer.render(pt, sb, "Text");
        renderer.render(options.getSeparator(), sb, "Text");
    }

    private static void buildAuditMap(ITransformerAuditTrail trail, Map<String, List<String>> map, ThrowableProxy proxy) {
        if (proxy == null)
            return;

        for (var element : proxy.getExtendedStackTrace()) {
            var cls = element.getClassName();
            if (map.containsKey(cls))
                continue;

            var lines = new ArrayList<String>();
            for (var activity : trail.getActivityFor(cls))
                lines.add(activity.getType().name() + ": " + String.join(":", activity.getContext()));

            if (!lines.isEmpty())
                map.put(cls, lines);
        }

        for (final ThrowableProxy suppressed : proxy.getSuppressedProxies())
            buildAuditMap(trail, map, suppressed);

        buildAuditMap(trail, map, proxy.getCauseProxy());
    }

    /**
     * Gets an instance of the class.
     *
     * @param config The current Configuration.
     * @param options pattern options, may be null.  If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static TransformingThrowablePatternConverter newInstance(@SuppressWarnings("exports") final Configuration config, final String[] options) {
        return new TransformingThrowablePatternConverter(config, options);
    }

    @Deprecated(forRemoval = true, since = "10.2.2")
    public static String generateEnhancedStackTrace(final Throwable throwable) {
        var proxy = new ThrowableProxy(throwable);
        var buf = new StringBuilder();

        var trail = Optional.ofNullable(Launcher.INSTANCE)
            .map(Launcher::environment)
            .flatMap(env -> env.getProperty(IEnvironment.Keys.AUDITTRAIL.get()))
            .orElse(null);

        var nl = Strings.LINE_SEPARATOR;
        var renderer = PlainTextRenderer.getInstance();
        var suffix = "";
        //renderer = new ExtraDataTextRenderer(renderer, trail);
        //suffix = SUFFIXFLAG;
        proxy.formatExtendedStackTraceTo(buf, Collections.emptyList(), renderer, suffix, nl);

        if (trail != null) {
            final Map<String, List<String>> audit = new TreeMap<>();
            buildAuditMap(trail, audit, proxy);

            buf.append("Transformer Audit:").append(nl);
            for (var cls : audit.keySet()) {
                buf.append("  ").append(cls).append(nl);
                for (var line : audit.get(cls))
                    buf.append("    ").append(line).append(nl);
            }
        }

        return buf.toString();
    }
}
