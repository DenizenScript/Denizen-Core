package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.Mechanism;
import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.ObjectType;
import com.denizenscript.denizencore.objects.properties.PropertyParser;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.codegen.TagNamer;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ObjectTagProcessor<T extends ObjectTag> {

    public static class TagData<T extends ObjectTag, R extends ObjectTag> {

        public String name;

        public TagRunnable.ObjectInterface<T, R> runner;

        public Class<R> returnType;

        public ObjectTagProcessor<R> processor;

        public ObjectTagProcessor<T> source;

        public boolean isStatic;

        public TagData(ObjectTagProcessor<T> source, String name, TagRunnable.ObjectInterface<T, R> runner, Class<R> returnType, boolean isStatic) {
            this.source = source;
            this.name = name;
            this.runner = runner;
            this.returnType = returnType;
            this.isStatic = isStatic;
            ObjectType<R> type = ObjectFetcher.getType(returnType);
            this.processor = type == null ? null : type.tagProcessor;
        }
    }

    public HashMap<String, TagData<T, ? extends ObjectTag>> registeredObjectTags = new HashMap<>();

    public HashMap<String, MechanismData<T>> registeredMechanisms = new HashMap<>();

    @FunctionalInterface
    public interface CustomMatcher<T extends ObjectTag> {
        /**
         * Try a custom advanced matcher.
         * Return 'true' to match, 'false' to strictly not match, or 'null' if this matcher doesn't apply.
         * <p>
         * Example usage:
         * <pre>
         * // NOTE: REGISTER NOT-SWITCHES WITH CAUTION. "notSwitches" is a temporary hack and has side effects! Make sure your matcher name is definitely never a switch!
         * // <--[data]
         * // @name not_switches
         * // @values example_matcher
         * // -->
         * ScriptEvent.ScriptPath.notSwitches.add("example_matcher");
         * // The example matcher impl. "true" to match, "false" to definitely fail the match, "null" if text isn't relevant to your matcher (or not a match, but might match something else).
         * MaterialTag.tagProcessor.custommatchers.add(((object, matcherText) -> {
         *     if (matcherText.startsWith("example_matcher:")) {
         *         return CoreUtilities.equalsIgnoreCase(matcherText.substring("example_matcher:".length()), object.getMaterial().name());
         *     }
         *     return null;
         * }));
         * </pre>
         */
        Boolean tryMatch(T object, String matcherText);
    }

    public List<CustomMatcher<T>> custommatchers = new ArrayList<>();

    public Class<T> type;

    public void registerFutureTagDeprecation(String name, String... deprecatedVariants) {
        TagData properTag = registeredObjectTags.get(name);
        for (String variant : deprecatedVariants) {
            TagRunnable.ObjectInterface<T, ?> newRunnable = (attribute, object) -> {
                if (CoreConfiguration.futureWarningsEnabled) {
                    Debug.echoError(attribute.context,  "Using deprecated form of tag '" + name + "': '" + variant + "'.");
                }
                return properTag.runner.run(attribute, object);
            };
            registeredObjectTags.put(variant, new TagData(this, variant, newRunnable, properTag.returnType, false));
        }
    }

    public <R extends ObjectTag, P extends ObjectTag> void registerStaticTag(Class<R> returnType, Class<P> paramType, String name, TagRunnable.ObjectWithParamInterface<T, R, P> runnable, String... deprecatedVariants) {
        registerTagInternal(returnType, paramType, name, runnable, true, deprecatedVariants);
    }

    public <R extends ObjectTag, P extends ObjectTag> void registerTag(Class<R> returnType, Class<P> paramType, String name, TagRunnable.ObjectWithParamInterface<T, R, P> runnable, String... deprecatedVariants) {
        registerTagInternal(returnType, paramType, name, runnable, false, deprecatedVariants);
    }

    public <R extends ObjectTag, P extends ObjectTag> void registerTagInternal(Class<R> returnType, Class<P> paramType, String name, TagRunnable.ObjectWithParamInterface<T, R, P> runnable, boolean isStatic, String[] deprecatedVariants) {
        ObjectType<P> paramObjType = ObjectFetcher.getType(paramType);
        registerTagInternal(returnType, name, (Attribute attribute, T obj) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            ObjectTag param = attribute.getParamObject();
            if (TagManager.isStaticParsing && paramObjType != null && !paramObjType.canConvertStatic) {
                return null;
            }
            P result = param.asType(paramType, attribute.context);
            if (result == null) {
                if (!TagManager.isStaticParsing) {
                    attribute.echoError("Tag '<Y>" + name + "<W>' requires input of type '<Y>" + DebugInternals.getClassNameOpti(paramType) + "<W>' but received input '<LR>" + param + "<W>'.");
                }
                return null;
            }
            return runnable.run(attribute, obj, result);
        }, isStatic, deprecatedVariants);
    }

    public <R extends ObjectTag> void registerStaticTag(Class<R> returnType, String name, TagRunnable.ObjectInterface<T, R> runnable, String... deprecatedVariants) {
        registerTagInternal(returnType, name, runnable, true, deprecatedVariants);
    }

    public <R extends ObjectTag> void registerTag(Class<R> returnType, String name, TagRunnable.ObjectInterface<T, R> runnable, String... deprecatedVariants) {
        registerTagInternal(returnType, name, runnable, false, deprecatedVariants);
    }

    public <R extends ObjectTag> void registerTagInternal(Class<R> returnType, String name, TagRunnable.ObjectInterface<T, R> runnable, boolean isStatic, String[] deprecatedVariants) {
        final TagRunnable.ObjectInterface<T, R> namedRunnable = TagNamer.nameTagInterface(type, name, runnable);
        for (String variant : deprecatedVariants) {
            TagRunnable.ObjectInterface<T, R> newRunnable = TagNamer.nameTagInterface(type, variant, (attribute, object) -> {
                Debug.echoError(attribute.context, "Using deprecated form of tag '" + name + "': '" + variant + "'.");
                return namedRunnable.run(attribute, object);
            });
            registeredObjectTags.put(variant, new TagData<>(this, variant, newRunnable, returnType, false));
        }
        registeredObjectTags.put(name, new TagData<>(this, name, namedRunnable, returnType, isStatic));
    }

    public final ObjectTag getObjectAttribute(T object, Attribute attribute) {
        if (attribute == null) {
            if (CoreConfiguration.debugVerbose) {
                Debug.verboseLog("TagProcessor - Attribute null!");
            }
            return null;
        }
        attribute.lastValid = object;
        if (attribute.isComplete()) {
            if (CoreConfiguration.debugVerbose) {
                Debug.verboseLog("TagProcessor - Attribute complete! Self return!");
            }
            return object;
        }
        Attribute.AttributeComponent nextComponent = attribute.attributes[attribute.fulfilled];
        ObjectTag returned;
        TagData data = nextComponent.data;
        if (data == null) {
            data = registeredObjectTags.get(nextComponent.key);
        }
        if (data != null) {
            if (CoreConfiguration.debugVerbose) {
                Debug.verboseLog("TagProcessor - Sub-tag found for " + nextComponent.key);
            }
            returned = data.runner.run(attribute, object);
            if (returned == null) {
                if (CoreConfiguration.debugVerbose) {
                    Debug.verboseLog("TagProcessor - result was null");
                }
                attribute.trackLastTagFailure();
                return null;
            }
            if (data.processor != null) {
                return data.processor.getObjectAttribute(returned, attribute.fulfill(1));
            }
            return returned.getObjectAttribute(attribute.fulfill(1));
        }
        returned = CoreUtilities.autoPropertyTagObject(object, attribute);
        if (returned == null) {
            returned = object.specialTagProcessing(attribute);
        }
        if (returned != null) {
            return returned;
        }
        return object.getNextObjectTypeDown().getObjectAttribute(attribute);
    }

    public static class MechanismData<T extends ObjectTag> {

        public String name;

        public Mechanism.GenericMechRunnerInterface<T> runner;

        public boolean allowProperty;
    }

    public final void processMechanism(T object, Mechanism mechanism) {
        MechanismData<T> mechData = registeredMechanisms.get(mechanism.getName());
        if (mechData == null) {
            CoreUtilities.autoPropertyMechanism(object, mechanism);
            return;
        }
        mechanism.fulfill();
        if (mechanism.isProperty && !mechData.allowProperty) {
            mechanism.echoError("Error: mechanism '" + mechData.name + "' may not be used as property input.");
            return;
        }
        mechData.runner.run(object, mechanism);
    }

    public void registerMechanism(String name, boolean allowProperty, Mechanism.GenericMechRunnerInterface<T> runner, String... deprecatedVariants) {
        MechanismData<T> data = new MechanismData<>();
        data.allowProperty = allowProperty;
        data.name = name;
        data.runner = runner;
        PropertyParser.allMechanismsEver.add(name);
        registeredMechanisms.put(name, data);
        for (String variant : deprecatedVariants) {
            MechanismData<T> variantData = new MechanismData<>();
            variantData.allowProperty = allowProperty;
            variantData.name = variant;
            variantData.runner = (object, mechanism) -> {
                mechanism.echoError("Using deprecated form of mechanism '" + name + "': '" + variant + "'.");
                runner.run(object, mechanism);
            };
            registeredMechanisms.put(variant, variantData);
        }
    }

    public <P extends ObjectTag> void registerMechanism(String name, boolean allowProperty, Class<P> paramType, Mechanism.ObjectInputMechRunnerInterface<T, P> runner, String... deprecatedVariants) {
        registerMechanism(name, allowProperty, (object, mechanism) -> {
            if (mechanism.value == null) {
                mechanism.echoError("Error: mechanism '" + name + "' must have input of type '" + DebugInternals.getClassNameOpti(paramType) + "', but none was given.");
                return;
            }
            P input = mechanism.value.asType(paramType, mechanism.context);
            if (input == null) {
                mechanism.echoError("Error: mechanism '" + name + "' must have input of type '" + DebugInternals.getClassNameOpti(paramType) + "', but value '" + mechanism.value + "' cannot be converted to the required type.");
                return;
            }
            runner.run(object, mechanism, input);
        }, deprecatedVariants);
    }
}
