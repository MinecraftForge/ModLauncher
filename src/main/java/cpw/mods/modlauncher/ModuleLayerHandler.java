/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolutionException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
        try {
        	cfg = Configuration.resolveAndBind(SecureModuleFinder.of(jars), parentConfigs, ModuleFinder.of(), targets);
		}catch (ResolutionException e) {
			resolveFail(jars,e);
			throw e;
		}
        var classLoader = classLoaderSupplier.create(cfg, parentLayers, parentLoaders);

        // TODO: [ML] This should actually find the correct CL for each module, not just use the newly created one
        var newLayer = ModuleLayer.defineModules(cfg,parentLayers, module -> classLoader).layer();

        var info = new LayerInfo(newLayer, classLoader);
        completedLayers.put(layer, info);
        return info;
    }
    

    static List<String> extractModuleNames(String message) {
    	String msg = message.toLowerCase(Locale.ENGLISH);
    	if(msg.toLowerCase().startsWith("modules ") && msg.contains(" export package ") && msg.contains(" to module ")) {
    		return handleExportsPackageToModule(msg);
    	}else if(msg.startsWith("module ")) {
    		if (msg.contains(" reads another module named ")) {
                return handleReadsAnother(msg);
            } else if (msg.contains(" reads more than one module named ")) {
                return handleReadsMoreThanOne(msg);
            } else if (msg.contains(" does not read a module that exports ")) {
                return handleDoesNotReadExported(msg);
            } else if (msg.contains(" contains package ")) {
                return handleContainsPackage(msg);
            }
    	}else if (msg.startsWith("cycle detected:")) {
            return handleCycleDetected(msg);
    	}
        LOGGER.error("Unsupported error format");
        return Collections.emptyList();
    }
  
    /**
     * Module foo reads another module named bar
     */
    private static List<String> handleReadsAnother(String msg) {
        String[] parts = msg.split("(?i) reads another module named ");
        String source = parts[0].trim().substring("Module ".length()).trim();
        String target = parts[1].trim();
        return Arrays.asList(source, target);
    }

    /**
     * module baz reads more than one module named qux and corge
     */
    private static List<String> handleReadsMoreThanOne(String msg) {
        String[] parts = msg.split("(?i) reads more than one module named ");
        String source = parts[0].trim().substring("module ".length()).trim();
        String tail = parts[1].trim();
        List<String> targets = splitOnAndOrComma(tail);
        List<String> modules = new ArrayList<>();
        modules.add(source);
        modules.addAll(targets);
        return modules;
    }

    /**
     * module grault does not read a module that exports garply
     */
    private static List<String> handleDoesNotReadExported(String msg) {
        String[] parts = msg.split("(?i) does not read a module that exports ");
        String source = parts[0].trim().substring("module ".length()).trim();
        return Collections.singletonList(source);
    }

    /**
     *module fred contains package quux, module plugh exports package corge to thud
     */
    private static List<String> handleContainsPackage(String msg) {
        String[] clauses = msg.split(",");
        String c1 = clauses[0].trim();
        String src1 = c1.split("(?i) contains package ")[0]
                       .substring("module ".length()).trim();
        List<String> modules = new ArrayList<>();
        modules.add(src1);

        if (clauses.length > 1) {
            String c2 = clauses[1].trim();
            if (c2.toLowerCase().contains(" exports package ") && c2.toLowerCase().contains(" to ")) {
                String[] parts2 = c2.split("(?i) exports package ");
                String src2 = parts2[0].substring("module ".length()).trim();
                String[] tail = parts2[1].split("(?i) to ");
                String tgt2 = tail[1].trim();

                modules.add(src2);
                modules.add(tgt2);
            }
        }

        return modules;
    }

    /**
     * Handles error messages where multiple modules export a package to another module.
     * modules xyzzy and plugh export package quux to module frobozz
     */
    private static List<String> handleExportsPackageToModule(String msg) {
        String core = msg.substring("modules ".length()).trim();
        String[] parts = core.split("(?i) export package ");
        String moduleList = parts[0].trim();
        String[] tail = parts[1].split("(?i) to module ");
        String dest = tail[1].trim();
        List<String> sources = splitOnAndOrComma(moduleList);
        List<String> modules = new ArrayList<>(sources);
        modules.add(dest);
        return modules;
    }

    /**
     *Cycle detected: foo -> bar -> baz -> foo
     */
    private static List<String> handleCycleDetected(String msg) {
        String tail = msg.substring("Cycle detected:".length()).trim();
        String[] nodes = tail.split("\\s*->\\s*");
        return new ArrayList<>(Arrays.asList(nodes));
    }

    private static List<String> splitOnAndOrComma(String input) {
        List<String> out = new ArrayList<>();
        String normalised = input.replaceAll("(?i)\\s+and\\s+", ",");
        for (String piece : normalised.split("\\s*,\\s*")) {
            if (!piece.isBlank()) {
                out.add(piece.trim());
            }
        }
        return out;
    }

    
    static void resolveFail(SecureJar[] jars, ResolutionException exception) {
    	List<String> premodules = extractModuleNames(exception.getMessage());
        StringBuilder build = new StringBuilder();
        build.append(exception.getMessage());
        build.append(System.lineSeparator());
        List<String> moduleLines = new ArrayList<>();
        Set<String> modules = new HashSet<String>();
        modules.addAll(premodules);
        for (String module : modules) {
            String location = getModuleLocation(jars, module);
            moduleLines.add("- " + module + " (" + location + ")");
        }
        if (!moduleLines.isEmpty()) {
            build.append("Impacted Modules:").append(System.lineSeparator());
            build.append(String.join(System.lineSeparator(), moduleLines));
        }
        LOGGER.log(Level.FATAL, build.toString());
    }
     


    public static String getModuleLocation(SecureJar[] jars, String name) {
         for(SecureJar jar:jars) {
        	 String mod_name=jar.moduleDataProvider().name();
        	 if(mod_name.equals(name)) {
        		 return jar.getPrimaryPath().toString();
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
