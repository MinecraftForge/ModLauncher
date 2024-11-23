/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.*;
import cpw.mods.modlauncher.internal.GuardedOptionResult;
import joptsimple.*;
import joptsimple.util.*;

import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public final class ArgumentHandler {
    private static final OptionParser PARSER = new OptionParser();

    private static final OptionSpec<String> PROFILE_OPTION = PARSER
            .accepts("version", "The version we launched with")
            .withRequiredArg();

    private static final OptionSpec<Path> GAME_DIR_OPTION = PARSER
            .accepts("gameDir", "Alternative game directory")
            .withRequiredArg()
            .withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING))
            .defaultsTo(Path.of("."));

    private static final OptionSpec<Path> ASSETS_DIR_OPTION = PARSER
            .accepts("assetsDir", "Assets directory")
            .withRequiredArg()
            .withValuesConvertedBy(new PathConverter(PathProperties.DIRECTORY_EXISTING));

    private static final OptionSpec<String> LAUNCH_TARGET_OPTION = PARSER
            .accepts("launchTarget", "LauncherService target to launch")
            .withRequiredArg();

    private static final OptionSpec<String> UUID_OPTION = PARSER
            .accepts("uuid", "The UUID of the logging in player")
            .withRequiredArg();

    static {
        PARSER.allowsUnrecognizedOptions();
    }

    private final String[] args;
    private final DiscoveryData discoveryData;
    private OptionSet optionSet;

    record DiscoveryData(Path gameDir, String launchTarget, String[] arguments) {}

    ArgumentHandler(String[] args) {
        this.args = args;
        this.optionSet = PARSER.parse(args);
        this.discoveryData = new DiscoveryData(optionSet.valueOf(GAME_DIR_OPTION), optionSet.valueOf(LAUNCH_TARGET_OPTION), args);
    }

    DiscoveryData getDiscoveryData() {
        return this.discoveryData;
    }

    void processArguments(Environment env, Consumer<OptionParser> parserConsumer, BiConsumer<OptionSet, BiFunction<String, OptionSet, ITransformationService.OptionResult>> resultConsumer) {
        parserConsumer.accept(PARSER);
        this.optionSet = PARSER.parse(this.args);
        env.computePropertyIfAbsent(IEnvironment.Keys.VERSION.get(), s -> this.optionSet.valueOf(PROFILE_OPTION));
        env.computePropertyIfAbsent(IEnvironment.Keys.GAMEDIR.get(), f -> this.discoveryData.gameDir);
        env.computePropertyIfAbsent(IEnvironment.Keys.ASSETSDIR.get(), f -> this.optionSet.valueOf(ASSETS_DIR_OPTION));
        env.computePropertyIfAbsent(IEnvironment.Keys.LAUNCHTARGET.get(), f -> this.discoveryData.launchTarget);
        env.computePropertyIfAbsent(IEnvironment.Keys.UUID.get(), f -> this.optionSet.valueOf(UUID_OPTION));
        resultConsumer.accept(this.optionSet, ArgumentHandler::optionResults);
    }

    String getLaunchTarget() {
        return this.discoveryData.launchTarget;
    }

    private static ITransformationService.OptionResult optionResults(String serviceName, OptionSet set) {
        return new GuardedOptionResult(serviceName, set);
    }

    public String[] buildArgumentList() {
        var args = new ArrayList<String>();
        addOptionToString(PROFILE_OPTION, optionSet, args);
        addOptionToString(GAME_DIR_OPTION, optionSet, args);
        addOptionToString(ASSETS_DIR_OPTION, optionSet, args);
        addOptionToString(UUID_OPTION, optionSet, args);
        List<?> nonOptionList = this.optionSet.nonOptionArguments();
        args.addAll(nonOptionList.stream().map(Object::toString).toList());
        return args.toArray(new String[0]);
    }

    private static void addOptionToString(OptionSpec<?> option, OptionSet optionSet, List<String> appendTo) {
        if (optionSet.has(option)) {
            appendTo.add("--" + option.options().getFirst());
            appendTo.add(option.value(optionSet).toString());
        }
    }
}
