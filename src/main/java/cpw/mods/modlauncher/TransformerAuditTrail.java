/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher;

import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerActivity;
import cpw.mods.modlauncher.api.ITransformerAuditTrail;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TransformerAuditTrail implements ITransformerAuditTrail {
    private Map<String, List<ITransformerActivity>> audit = new ConcurrentHashMap<>();

    @Override
    public List<ITransformerActivity> getActivityFor(final String className) {
        return Collections.unmodifiableList(getTransformerActivities(className));
    }

    private static class TransformerActivity implements ITransformerActivity {
        private final Type type;
        private final String[] context;

        private TransformerActivity(Type type, String... context) {
            this.type = type;
            this.context = context;
        }

        @Override
        public String[] getContext() {
            return context;
        }

        @Override
        public Type getType() {
            return type;
        }

        public String getActivityString() {
            return this.type.getLabel() + ":"+ String.join(":",this.context);
        }
    }

    public void addReason(String clazz, String reason) {
        getTransformerActivities(clazz).add(new TransformerActivity(ITransformerActivity.Type.REASON, reason));
    }

    public void addPluginCustomAuditTrail(String clazz, ILaunchPluginService plugin, String... data) {
        getTransformerActivities(clazz).add(new TransformerActivity(ITransformerActivity.Type.PLUGIN, concat(plugin.name(), data)));
    }

    public void addPluginAuditTrail(String clazz, ILaunchPluginService plugin, ILaunchPluginService.Phase phase) {
        getTransformerActivities(clazz).add(new TransformerActivity(ITransformerActivity.Type.PLUGIN, plugin.name(), phase.name().substring(0,1)));
    }

    public void addTransformerAuditTrail(String clazz, ITransformationService transformService, ITransformer<?> transformer) {
        getTransformerActivities(clazz).add(new TransformerActivity(ITransformerActivity.Type.TRANSFORMER, concat(transformService.name(), transformer.labels())));
    }

    private String[] concat(String first, String[] rest) {
        final String[] res = new String[rest.length + 1];
        res[0] = first;
        System.arraycopy(rest, 0, res, 1, rest.length);
        return res;
    }
    private List<ITransformerActivity> getTransformerActivities(final String clazz) {
        return audit.computeIfAbsent(clazz, k->new ArrayList<>());
    }

    @Override
    public String getAuditString(final String clazz) {
        return audit.getOrDefault(clazz, Collections.emptyList()).stream().map(ITransformerActivity::getActivityString).collect(Collectors.joining(","));
    }
}
