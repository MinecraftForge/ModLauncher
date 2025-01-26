package cpw.mods.modlauncher.internal;

import cpw.mods.modlauncher.api.ITransformationService;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record GuardedOptionResult(String serviceName, OptionSet delegate) implements ITransformationService.OptionResult {
    @Override
    public <V> V value(OptionSpec<V> option) {
        checkOwnership(option);
        return delegate.valueOf(option);
    }

    @Override
    public @NotNull <V> List<V> values(OptionSpec<V> options) {
        checkOwnership(options);
        return delegate.valuesOf(options);
    }

    private <V> void checkOwnership(OptionSpec<V> option) {
        if (!(option.options().stream().allMatch(opt -> opt.startsWith(serviceName + ".") || !opt.contains(".")))) {
            throw new IllegalArgumentException("Cannot process non-arguments");
        }
    }
}
