/*
 * Copyright (c) Forge Development LLC
 * SPDX-License-Identifier: LGPL-3.0-only
 */

package cpw.mods.modlauncher.api;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * A transformer is injected into the modding ClassLoader. It can manipulate any item
 * it is designated to target.
 */
public interface ITransformer<T> {

    String[] DEFAULT_LABEL = {"default"};

    /**
     * Transform the input to the ITransformer's desire. The context from the last vote is
     * provided as well.
     *
     * @param input   The ASM input node, which can be mutated directly
     * @param context The voting context
     * @return An ASM node of the same type as that supplied. It will be used for subsequent
     * rounds of voting.
     */
    @NotNull
    T transform(T input, ITransformerVotingContext context);

    /**
     * Return the {@link TransformerVoteResult} for this transformer.
     * The transformer should evaluate whether or not is is a candidate to apply during
     * the round of voting in progress, represented by the context parameter.
     * How the vote works:
     * <ul>
     * <li>If the transformer wishes to be a candidate, it should return {@link TransformerVoteResult#YES}.</li>
     * <li>If the transformer wishes to exit the voting (the transformer has already
     * has its intended change applied, for example), it should return {@link TransformerVoteResult#NO}</li>
     * <li>If the transformer wishes to wait for future rounds of voting it should return
     * {@link TransformerVoteResult#DEFER}. Note that if there is <em>no</em> YES candidate, but DEFER
     * candidates remain, this is a DEFERRAL stalemate and the game will crash.</li>
     * <li>If the transformer wishes to crash the game, it should return {@link TransformerVoteResult#REJECT}.
     * This is extremely frowned upon, and should not be used except in extreme circumstances. If an
     * incompatibility is present, it should detect and handle it in the {@link ITransformationService#onLoad}
     * </li>
     * </ul>
     * After all votes from candidate transformers are collected, the NOs are removed from the
     * current set of voters, one from the set of YES voters is selected and it's {@link ITransformer#transform(Object, ITransformerVotingContext)}
     * method called. It is then removed from the set of transformers and another round is performed.
     *
     * @param context The context of the vote
     * @return A TransformerVoteResult indicating the desire of this transformer
     */
    @NotNull
    TransformerVoteResult castVote(ITransformerVotingContext context);

    /**
     * Return a set of {@link Target} identifying which elements this transformer wishes to try
     * and apply to. The {@link Target#targetType()} must match the T variable for the transformer
     * as documented in {@link TargetType}, other combinations will be rejected.
     *
     * @return The set of targets this transformer wishes to apply to
     */
    @NotNull
    Set<Target> targets();

    /**
     * @return A string array for uniquely identifying this transformer instance within the service.
     */
    default String[] labels() {
        return DEFAULT_LABEL;
    }
    /**
     * Specifies the target type for the {@link Target}. Note that the type of the transformer T
     * dictates what are acceptable targets for this transformer.
     */
    enum TargetType {
        /**
         * Target a class. The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.ClassNode}
         */
        CLASS,
        /**
         * Target a method. The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.MethodNode}
         */
        METHOD,
        /**
         * Target a field. The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.FieldNode}
         */
        FIELD,
        /**
         * Target a class, before field and method transforms operate. SHOULD ONLY BE USED to "replace" a complete class
         * The {@link ITransformer} T variable must refer to {@link org.objectweb.asm.tree.ClassNode}
         */
        PRE_CLASS;
    }

    /**
     * Simple data holder indicating where the {@link ITransformer} can target.
     * @param className         The name of the class being targeted
     * @param elementName       The name of the element being targeted. This is the field name for a field,
     *                          the method name for a method. Empty string for other types
     * @param elementDescriptor The method's descriptor. Empty string for other types
     * @param targetType        The {@link TargetType} for this target - it should match the ITransformer
     *                          type variable T
     */
    @SuppressWarnings("SameParameterValue")
    record Target(String className, String elementName, String elementDescriptor, TargetType targetType) {
        /**
         * Build a new target. Ensure that the targetType matches the T type for the ITransformer
         * supplying the target.
         * <p>
         * In an obfuscated environment, this will be the obfuscated "notch" naming, in a
         * deobfuscated environment this will be the searge naming.
         */
        public Target {
            Objects.requireNonNull(className, "Class Name cannot be null");
            Objects.requireNonNull(elementName, "Element Name cannot be null");
            Objects.requireNonNull(elementDescriptor, "Element Descriptor cannot be null");
            Objects.requireNonNull(targetType, "Target Type cannot be null");
        }

        /**
         * Convenience method returning a {@link Target} for a class
         *
         * @param className The name of the class
         * @return A target for the named class
         */
        @NotNull
        public static Target targetClass(String className) {
            return new Target(className, "", "", TargetType.CLASS);
        }

        /**
         * Convenience method returning a {@link Target} for a class (prior to other loading operations)
         *
         * @param className The name of the class
         * @return A target for the named class
         */
        @NotNull
        public static Target targetPreClass(String className) {
            return new Target(className, "", "", TargetType.PRE_CLASS);
        }
        /**
         * Convenience method return a {@link Target} for a method
         *
         * @param className        The name of the class containing the method
         * @param methodName       The name of the method
         * @param methodDescriptor The method's descriptor string
         * @return A target for the named method
         */
        @NotNull
        public static Target targetMethod(String className, String methodName, String methodDescriptor) {
            return new Target(className, methodName, methodDescriptor, TargetType.METHOD);
        }

        /**
         * Convenience method returning a {@link Target} for a field
         *
         * @param className The name of the class containing the field
         * @param fieldName The name of the field
         * @return A target for the named field
         */
        @NotNull
        public static Target targetField(String className, String fieldName) {
            return new Target(className, fieldName, "", TargetType.FIELD);
        }

        /**
         * @return The class name for this target
         */
        public String getClassName() {
            return className;
        }

        /**
         * @return The element name for this target, either the field name or the method name
         */
        public String getElementName() {
            return elementName;
        }

        /**
         * @return The element's descriptor. Usually the method descriptor
         */
        public String getElementDescriptor() {
            return elementDescriptor;
        }

        /**
         * @return The target type of this target
         */
        public TargetType getTargetType() {
            return targetType;
        }
    }
}
