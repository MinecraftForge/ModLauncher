/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolutionException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.securemodules.SecureModuleFinder;

public final class ModuleLayerHandler implements IModuleLayerManager {
    record LayerInfo(ModuleLayer layer, ClassLoader cl) {}
    private final EnumMap<Layer, List<SecureJar>> layers = new EnumMap<>(Layer.class);
    private final EnumMap<Layer, LayerInfo> completedLayers = new EnumMap<>(Layer.class);
    private static final Logger LOGGER = LogManager.getLogger();
    
    ModuleLayerHandler() {
        var classLoader = getClass().getClassLoader();
        var layer = getClass().getModule().getLayer();
        // If we have not booted into a Module layer, lets stick ourselves in one. This is here for unit tests.
        if (layer == null) {
            var cfg = ModuleLayer.boot().configuration().resolveAndBind(ModuleFinder.of(), ModuleFinder.ofSystem(), List.of());
            var cl = new SecureModuleClassLoader("BOOT", classLoader, cfg, List.of(ModuleLayer.boot()));
            layer = ModuleLayer.boot().defineModules(cfg, m -> cl);
            System.out.println("Making new classloader: " + classLoader);
            classLoader = cl;
        }
        completedLayers.put(Layer.BOOT, new LayerInfo(layer, classLoader));
    }

    @Override
    public Optional<ModuleLayer> getLayer(Layer layer) {
        return Optional.ofNullable(completedLayers.get(layer)).map(LayerInfo::layer);
    }

    void addToLayer(Layer layer, SecureJar jar) {
        if (completedLayers.containsKey(layer))
             throw new IllegalStateException("Layer already populated");
        layers.computeIfAbsent(layer, l -> new ArrayList<>()).add(jar);
    }

    /** TODO: Make package private */
    @SuppressWarnings("exports")
    @Deprecated(since = "10.1")
    public LayerInfo buildLayer(Layer layer, BiFunction<Configuration, List<ModuleLayer>, ClassLoader> classLoaderSupplier) {
        return build(layer, (cfg, layers, loaders) -> classLoaderSupplier.apply(cfg, layers));
    }

    LayerInfo build(Layer layer) {
        return build(layer, (cfg, layers, loaders) -> new SecureModuleClassLoader("LAYER " + layer.name(), null, cfg, layers, loaders));
    }

    LayerInfo build(Layer layer, ClassLoaderFactory classLoaderSupplier) {
        var jars = layers.getOrDefault(layer, List.of()).toArray(SecureJar[]::new);
        var targets = Arrays.stream(jars).map(SecureJar::name).toList();
        var parentLayers = new ArrayList<ModuleLayer>();
        var parentConfigs = new ArrayList<Configuration>();
        var parentLoaders = new ArrayList<ClassLoader>();

        for (var parent : layer.getParents()) {
            var info = completedLayers.get(parent);
            if (info == null)
                throw new IllegalStateException("Attempted to build " + layer + " before it's parent " + parent + " was populated");
            parentLayers.add(info.layer());
            parentConfigs.add(info.layer().configuration());
            parentLoaders.add(info.cl());
        }

        Configuration cfg = null;
        SecureModuleFinder smf = SecureModuleFinder.of(jars);
        try {
        	cfg = Configuration.resolveAndBind(smf, parentConfigs, ModuleFinder.of(), targets);
		} catch (ResolutionException e) {
			String message = e.getMessage();
			resolveFail(smf,message,extractModuleNames(message));
			throw e;
		}

        var classLoader = classLoaderSupplier.create(cfg, parentLayers, parentLoaders);

        // TODO: [ML] This should actually find the correct CL for each module, not just use the newly created one
        var newLayer = ModuleLayer.defineModules(cfg,parentLayers, module -> classLoader).layer();

        var info = new LayerInfo(newLayer, classLoader);
        completedLayers.put(layer, info);
        return info;
    }
    

    public static List<String> extractModuleNames(String message) {
        // Local denylist to filter out keywords
        Set<String> denylist = Set.of(
            "module", "modules", "named", "that", "and", "or",
            "exports", "requires", "contains", "reads"
        );
        Set<String> modules = new LinkedHashSet<>();

        // This group now allows dotted names: foo, com.example.app, net.minecraftforge.forge, etc.
        String MOD = "[a-z][A-Za-z0-9_]*(?:\\.[a-z][A-Za-z0-9_]*)*";

        // 1) After "module"/"modules" or "named"
        Pattern p1 = Pattern.compile(
            "(?i)(?<=\\b(?:modules?|named)\\s)" +
            "(" + MOD + ")(?:\\s*(?:,|and|or)\\s*(" + MOD + "))*"
        );
        Matcher m1 = p1.matcher(message);
        while (m1.find()) {
            for (int i = 1; i <= m1.groupCount(); i++) {
                String name = m1.group(i);
                if (name != null && !denylist.contains(name.toLowerCase())) {
                    modules.add(name);
                }
            }
        }

        // 2) Cycle detection (usually simple names, but we'll allow dots here too)
        Pattern p2 = Pattern.compile(
            "(?i)Cycle detected:\\s*(" + MOD + "(?:\\s*->\\s*" + MOD + ")+)"
        );
        Matcher m2 = p2.matcher(message);
        if (m2.find()) {
            for (String name : m2.group(1).split("\\s*->\\s*")) {
                if (!denylist.contains(name.toLowerCase())) {
                    modules.add(name);
                }
            }
        }

        return new ArrayList<>(modules);
    }

    
    public static void resolveFail(SecureModuleFinder finder, String message, List<String> modules) {
        StringBuilder build = new StringBuilder();
        build.append(message);
        if (!message.isEmpty() && !message.endsWith("\n")) {
            build.append(System.lineSeparator());
        }

        build.append("Impacted Modules:").append(System.lineSeparator());

        List<String> moduleLines = new ArrayList<>();
        for (String module : modules) {
            String location = getModuleLocation(finder, module);
            moduleLines.add("- " + module + " (" + location + ")");
        }

        if (!moduleLines.isEmpty()) {
            build.append(String.join(System.lineSeparator(), moduleLines));
        } else {
            build.append("  (Could not find)");
        }

        LOGGER.log(Level.FATAL, build.toString());
    }
     


     public static String getModuleLocation(SecureModuleFinder finder, String name) {
         for (ModuleReference reference : finder.findAll()) {
             if (reference.descriptor().name().equals(name)) {
                 Optional<URI> location = reference.location();
                 if (location.isPresent()) {
                     URI uri = location.get();

                     if ("jar".equalsIgnoreCase(uri.getScheme())) {
                         String spec = uri.getSchemeSpecificPart();

                         if (spec == null) {
                             return null;
                         }
                         int exclamationIndex = spec.indexOf('!');
                         if (exclamationIndex != -1) {
                             spec = spec.substring(0, exclamationIndex);
                         }

                         try {
                             // Use Paths.get() to safely parse the file path
                             Path jarPath = Paths.get(new URI(spec));
                             return jarPath.getFileName().toString();
                         } catch (Exception e) {
                             // Fallback: Try parsing directly as a file path (in case of malformed URI)
                             // Remove any leading "file:" prefix before parsing
                             String cleanSpec = spec.replaceFirst("^file:", "");
                             Path fallbackPath = Paths.get(cleanSpec);
                             return fallbackPath.getFileName().toString();
                         }
                     }
                 }
             }
         }
         return null;
     }

    /** TODO: Make package private */
    @SuppressWarnings("exports")
    @Deprecated(since = "10.1")
    public LayerInfo buildLayer(Layer layer) {
        return build(layer);
    }

    /** TODO: Make package private */
    @SuppressWarnings("exports")
    @Deprecated(since = "10.1")
    public void updateLayer(Layer layer, Consumer<LayerInfo> action) {
        action.accept(completedLayers.get(layer));
    }

    interface ClassLoaderFactory {
        ClassLoader create(Configuration config, List<ModuleLayer> parentLayers, List<ClassLoader> parentLoaders);
    }
}
