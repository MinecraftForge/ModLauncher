/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.log;

import cpw.mods.modlauncher.api.ITransformerAuditTrail;
import org.apache.logging.log4j.core.pattern.TextRenderer;

@Deprecated(forRemoval = true, since = "10.2.2")
public class ExtraDataTextRenderer implements TextRenderer {
    private final TextRenderer wrapped;
    private final ITransformerAuditTrail trail;
    private final ThreadLocal<TransformerContext> currentClass = new ThreadLocal<>();

    ExtraDataTextRenderer(final TextRenderer wrapped, ITransformerAuditTrail trail) {
        this.wrapped = wrapped;
        this.trail = trail;
    }

    @Override
    public void render(final String input, final StringBuilder output, final String styleName) {
        if ("StackTraceElement.ClassName".equals(styleName)) {
            currentClass.set(new TransformerContext());
            currentClass.get().setClassName(input);
        } else if ("StackTraceElement.MethodName".equals(styleName)) {
            final TransformerContext transformerContext = currentClass.get();
            if (transformerContext != null) {
                transformerContext.setMethodName(input);
            }
        } else if ("Suffix".equals(styleName)) {
            final TransformerContext classContext = currentClass.get();
            currentClass.remove();
            if (classContext != null) {
                final String auditLine = trail == null ? "" : trail.getAuditString(classContext.getClassName());
                wrapped.render(" {" + auditLine + "}", output, "StackTraceElement.Transformers");
            }
            return;
        }
        wrapped.render(input, output, styleName);
    }

    @Override
    public void render(final StringBuilder input, final StringBuilder output) {
        wrapped.render(input, output);
    }

    private static class TransformerContext {

        private String className;
        private String methodName;

        public void setClassName(final String className) {
            this.className = className;
        }

        public String getClassName() {
            return className;
        }

        public void setMethodName(final String methodName) {
            this.methodName = methodName;
        }

        public String getMethodName() {
            return methodName;
        }

        @Override
        public String toString() {
            return getClassName()+"."+getMethodName();
        }
    }
}
