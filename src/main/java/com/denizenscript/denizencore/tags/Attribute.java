package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.exceptions.TagProcessingException;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.*;

public class Attribute {

    public static class AttributeComponent {

        public final String rawKey;

        public final String key;

        public final String rawParam;

        public ParseableTag paramParsed;

        public ObjectTagProcessor.TagData<? extends ObjectTag, ? extends ObjectTag> data;

        public AttributeComponent(String inp) {
            if (inp.endsWith("]") && CoreUtilities.contains(inp, '[')) {
                int ind = inp.indexOf('[');
                rawKey = inp.substring(0, ind);
                rawParam = inp.substring(ind + 1, inp.length() - 1);
            }
            else {
                rawKey = inp;
                rawParam = null;
            }
            key = CoreUtilities.toLowerCase(rawKey);
        }

        @Override
        public String toString() {
            if (rawParam != null) {
                return key + "[" + rawParam + "]";
            }
            return key;
        }
    }

    public static HashMap<String, AttributeComponent[]> attribsLookup = new HashMap<>();

    private static boolean isNumber(char c) {
        return c >= '0' && c <= '9';
    }

    private static AttributeComponent[] separate_attributes(String attributes) throws TagProcessingException {
        AttributeComponent[] matchesRes = attribsLookup.get(attributes);
        if (matchesRes != null) {
            return matchesRes;
        }
        if (attributes.startsWith(".") || attributes.endsWith(".")) {
            throw new TagProcessingException("The tag '" + attributes + "' is invalid due to a misplaced dot at the start or end of the tag.");
        }
        ObjectTagProcessor<?> proc = null;
        ArrayList<AttributeComponent> matches = new ArrayList<>(attributes.length() / 7);
        int x1 = 0, x2 = -1;
        int braced = 0;
        char[] attrInp = attributes.toCharArray();
        for (int x = 0; x < attrInp.length; x++) {
            char chr = attrInp[x];
            if (chr == '[') {
                braced++;
            }
            if (x == attrInp.length - 1) {
                x2 = x + 1;
            }
            if (chr == ']') {
                if (braced > 0) {
                    braced--;
                }
            }
            else if (chr == '.' && !(x > 0 && isNumber(attrInp[x + 1]) && isNumber(attrInp[x - 1])) && braced == 0) {
                x2 = x;
            }
            if (x2 > -1) {
                if (x2 <= x1) {
                    throw new TagProcessingException("The tag '" + attributes + "' is invalid, likely due to double dots '..' somewhere. Did you forget a sub-tag, or accidentally double-tap the dot key?");
                }
                AttributeComponent component = new AttributeComponent(attributes.substring(x1, x2));
                if (matches.size() == 0) {
                    TagManager.TagBaseData baseTag = TagManager.baseTags.get(component.key);
                    if (baseTag != null && baseTag.processor != null) {
                        proc = baseTag.processor;
                    }
                }
                else if (proc != null) {
                    component.data = proc.registeredObjectTags.get(component.key);
                    proc = component.data == null ? null : component.data.processor;
                }
                matches.add(component);
                x2 = -1;
                x1 = x + 1;
            }
        }
        if (braced != 0) {
            throw new TagProcessingException("The tag '" + attributes + "' is invalid due to misplaced [square brackets]. Did you forget to close some brackets?");
        }
        if (CoreConfiguration.debugVerbose) {
            Debug.log("attribute splitter: '" + attributes + "' becomes: " + matches);
        }
        matchesRes = new AttributeComponent[matches.size()];
        matchesRes = matches.toArray(matchesRes);
        attribsLookup.put(attributes, matchesRes);
        return matchesRes;
    }

    public AttributeComponent[] attributes;
    public ObjectTag[] contexts;

    /**
     * Only present when debug is on.
     * 0 = untouched, 1 = filled, 2 = failed, 3 = preparsed
     */
    public int[] filled;

    ScriptEntry scriptEntry;

    public TagContext context;

    String origin;

    public ArrayList<String> seemingSuccesses = new ArrayList<>(2);

    /* Referenced by TagCodeGenerator */
    public boolean hadManualFulfill = false;

    /**
     * Last valid object while parsing this attribute chain, for debugging purposes.
     */
    public ObjectTag lastValid;

    public boolean hasContextFailed = false;

    int fulfilled = 0;

    public void resetErrorTrack() {
        if (CoreConfiguration.debugVerbose) {
            Debug.echoError("(Verbose) Attribute - error track reset");
        }
        if (!seemingSuccesses.isEmpty()) {
            seemingSuccesses.clear();
        }
        hasContextFailed = false;
    }

    public ScriptEntry getScriptEntry() {
        return scriptEntry;
    }

    public String getOrigin() {
        return origin;
    }


    public Attribute(Attribute ref, ScriptEntry scriptEntry, TagContext context) {
        this(ref, scriptEntry, context, 0);
    }

    private void setContext(TagContext context) {
        if (context == null) {
            context = CoreUtilities.basicContext;
        }
        this.context = context.clone();
        this.context.showErrors = () -> !hasAlternative();
    }

    public Attribute(Attribute ref, ScriptEntry scriptEntry, TagContext context, int skippable) {
        origin = ref.origin;
        this.scriptEntry = scriptEntry;
        setContext(context);
        attributes = ref.attributes;
        contexts = new ObjectTag[attributes.length];
        setHadAlternative(ref.hadAlternative);
        if (this.context.debug) {
            filled = new int[attributes.length];
            for (int i = 0; i < skippable; i++) {
                filled[i] = 3;
            }
        }
        fulfilled = skippable;
    }

    public Attribute(String attributes, ScriptEntry scriptEntry, TagContext context) throws TagProcessingException {
        origin = attributes;
        this.scriptEntry = scriptEntry;
        setContext(context);
        this.attributes = separate_attributes(attributes);
        contexts = new ObjectTag[this.attributes.length];
        if (this.context.debug) {
            filled = new int[this.attributes.length];
        }
    }

    public final boolean matches(String string) {
        if (fulfilled >= attributes.length) {
            return false;
        }
        return attributes[fulfilled].key.equals(string);
    }

    public final boolean startsWith(String string) {
        if (fulfilled >= attributes.length) {
            return false;
        }
        if (CoreConfiguration.debugVerbose) {
            Debug.log("Trying tag startsWith " + string + " on tag " + this);
        }
        if (string.indexOf('.') >= 0) {
            List<String> tmp = CoreUtilities.split(string, '.');
            if (tmp.size() + fulfilled > attributes.length) {
                return false;
            }
            for (int i = 0; i < tmp.size(); i++) {
                if (!attributes[fulfilled + i].key.equals(tmp.get(i))) {
                    return false;
                }
            }
            if (CoreConfiguration.debugVerbose) {
                Debug.log("Chain-Tag found!");
            }
            seemingSuccesses.add(string);
            return true;
        }
        if (attributes[fulfilled].key.equals(string)) {
            if (CoreConfiguration.debugVerbose) {
                Debug.log("Sub-tag found!");
            }
            seemingSuccesses.add(string);
            return true;
        }
        return false;
    }

    public final boolean startsWith(String string, int attribute) {
        return CoreUtilities.toLowerCase(getAttributeWithoutParam(attribute)).equals(string);
    }

    public final boolean isComplete() {
        return fulfilled >= attributes.length;
    }

    public final Attribute fulfill(int attributes) {
        hadManualFulfill = true;
        resetErrorTrack();
        if (filled != null) {
            for (int i = 0; i < attributes; i++) {
                if (fulfilled + i < filled.length) {
                    filled[fulfilled + i] = 1;
                }
            }
        }
        fulfilled += attributes;
        return this;
    }

    /* Referenced by TagCodeGenerator */
    public final void fulfillOne(ObjectTag obj) {
        lastValid = obj;
        resetErrorTrack();
        if (filled != null && fulfilled < filled.length) {
            filled[fulfilled] = 1;
        }
        fulfilled++;
    }

    /* Referenced by TagCodeGenerator */
    public final void trackLastTagFailure() {
        if (fulfilled < attributes.length) {
            seemingSuccesses.add(attributes[fulfilled].key);
            if (filled != null) {
                filled[fulfilled] = 2;
            }
        }
    }

    public final boolean hasParam() {
        if (fulfilled >= attributes.length) {
            return false;
        }
        if (attributes[fulfilled].rawParam != null) {
            return true;
        }
        if (CoreConfiguration.debugVerbose) {
            Debug.log("Attribute " + fulfilled + " is missing param, hasParamFailed");
        }
        hasContextFailed = true;
        return false;
    }

    @Deprecated
    public final boolean hasContext(int attribute) {
        attribute += fulfilled - 1;
        if (attribute < 0 || attribute >= attributes.length) {
            return false;
        }
        if (attributes[attribute].rawParam != null) {
            return true;
        }
        if (CoreConfiguration.debugVerbose) {
            Debug.log("Attribute " + attribute + " is missing context, hasContextFailed");
        }
        hasContextFailed = true;
        return false;
    }

    public static class OverridingDefinitionProvider implements DefinitionProvider {
        public DefinitionProvider originalProvider;
        public MapTag altDefs = new MapTag();
        public OverridingDefinitionProvider(DefinitionProvider original) {
            originalProvider = original;
        }
        @Override
        public void addDefinition(String definition, String value) {
            originalProvider.addDefinition(definition, value);
        }
        @Override
        public void addDefinition(String definition, ObjectTag value) {
            originalProvider.addDefinition(definition, value);
        }
        @Override
        public MapTag getAllDefinitions() {
            return originalProvider.getAllDefinitions();
        }
        @Override
        public ObjectTag getDefinitionObject(String definition) {
            ObjectTag result = altDefs.getDeepObject(CoreUtilities.toLowerCase(definition));
            if (result != null) {
                return result;
            }
            return originalProvider.getDefinitionObject(definition);
        }

        @Override
        public String getDefinition(String definition) {
            ObjectTag result = altDefs.getDeepObject(CoreUtilities.toLowerCase(definition));
            if (result != null) {
                return result.toString();
            }
            return originalProvider.getDefinition(definition);
        }

        @Override
        public boolean hasDefinition(String definition) {
            ObjectTag result = altDefs.getDeepObject(CoreUtilities.toLowerCase(definition));
            if (result != null) {
                return true;
            }
            return originalProvider.hasDefinition(definition);
        }

        @Override
        public void removeDefinition(String definition) {
            originalProvider.removeDefinition(definition);
        }
    }

    public final String getRawParam() {
        if (fulfilled >= attributes.length) {
            return null;
        }
        return attributes[fulfilled].rawParam;
    }

    public final ObjectTag parseDynamicParam(OverridingDefinitionProvider customProvider) {
        String inp = getRawParam();
        if (inp == null) {
            return null;
        }
        DefinitionProvider originalProvider = context.definitionProvider;
        context.definitionProvider = customProvider;
        try {
            return TagManager.tagObject(inp, context);
        }
        finally {
            context.definitionProvider = originalProvider;
        }
    }

    public final <T extends ObjectTag> T paramAsType(Class<T> dClass) {
        ObjectTag contextObj = getParamObject();
        if (contextObj == null) {
            return null;
        }
        return CoreUtilities.asType(contextObj, dClass, context);
    }

    @Deprecated
    public final <T extends ObjectTag> T contextAsType(int attribute, Class<T> dClass) {
        ObjectTag contextObj = getContextObject(attribute);
        if (contextObj == null) {
            return null;
        }
        return CoreUtilities.asType(contextObj, dClass, context);
    }

    public final ObjectTag getParamObject() {
        return getContextObject(1); // TODO
    }

    public final ObjectTag getContextObject(int attribute) {
        attribute += fulfilled - 1;
        if (attribute < 0 || attribute >= attributes.length) {
            return null;
        }
        ObjectTag tagged = contexts[attribute];
        if (tagged != null) {
            return tagged;
        }
        AttributeComponent component = attributes[attribute];
        if (component.paramParsed == null) {
            String inp = attributes[attribute].rawParam;
            if (inp == null) {
                return null;
            }
            component.paramParsed = TagManager.parseTextToTag(component.rawParam, context);
        }
        if (component.paramParsed == null) {
            return null;
        }
        tagged = component.paramParsed.parse(context);
        contexts[attribute] = tagged;
        return tagged;
    }

    public final String getParam() {
        return CoreUtilities.stringifyNullPass(getParamObject());
    }

    @Deprecated
    public final String getContext(int attribute) {
        return CoreUtilities.stringifyNullPass(getContextObject(attribute));
    }

    public final ElementTag getParamElement() {
        ObjectTag obj = getParamObject();
        if (obj == null) {
            return null;
        }
        return obj.asElement();
    }

    private boolean hadAlternative = false;

    // <--[language]
    // @name Tag Fallbacks
    // @group Tag System
    // @description
    // Tag fallbacks (AKA "tag alternatives") are a system designed to allow scripters to automatically handle tag errors.
    //
    // Fallbacks are implemented as special "magic tags" that look like any other tag-part, but override the error handler. These are "if_null", "exists", and "is_truthy".
    //
    // A tag without a fallback might look like "<player.name>".
    // This tag works fine as long as there's a linked player, but what if a player isn't always available?
    // Normally, this situation would display an error in the console debug logs, and return plaintext "player.name" in the script.
    // A fallback can help us handle the problem more gracefully.
    // That same tag with a fallback would look like "<player.name.if_null[Steve]>".
    // Now, when there isn't a player available, there will not be an error, and the tag will simply return "Steve".
    //
    // This format is the same for basically all tags. "<main.tag.here.if_null[Fallback here]>".
    // For another example, "<player.flag[myflag].if_null[0]>" returns either the value of the flag, or "0" if the flag is not present (or if there's no player).
    //
    // The "exists" fallback-tag is available for checking whether an object exists and is valid.
    // What if we want to check if there even is a linked player? We don't have a "<has_player>" tag to do that, so what can we do?
    // <code>
    // - if <player.exists>:
    // </code>
    // The above example demonstrates using a fallback to check if a player is valid.
    // The if block will run only if there is not a player valid (you might, for example, place the "stop" command inside).
    //
    // "Exists" is useful when you *only* need a check, however you often need to grab a value and verify it after.
    // Consider the following example, often found in command scripts:
    // <code>
    // - define target <server.match_player[<context.args.get[1]>].if_null[null]>
    // - if <[target]> == null:
    //     - narrate "<&[error]>Invalid player!"
    //     - stop
    // - narrate "<&[base]>You chose <&[emphasis]><[target].name><&[base]>!"
    // </code>
    //
    // We use the word "null" in the above example, as well as in the tag name itself. This is a common programming term that means "no object is present".
    // "if_null" is the actual tag name, however the input value of "null" isn't actually a functionality of Denizen, it's just a word we choose for clarity.
    // You could just as easily do "- if <player.if_null[nothing]> == nothing:", or for that matter "- if <player.if_null[cheese]> == cheese:".
    // A player object takes the form "p@uuid", so it will therefore never exactly match any simple word, so there's no coincidental match edge-case to worry about.
    // Note that this won't work so perfect for things like a user input or fully dynamic value,
    // so in those cases you may want to use the "exists" tag explicitly to guarantee no potential conflict.
    //
    // Fallbacks can be tags themselves. So, for example, if we want either a custom flag-based display name, or if not available, the player's base name,
    // we can do: "<player.flag[display_name].if_null[<player.name>]>".
    // You can as well chain these: "<player.flag[good_name].if_null[<player.flag[bad_name].if_null[<player.name>]>]>".
    //
    // Note that fallbacks will *hide errors*. Generally, the only errors you should ever hide are ones you're expecting that are fine.
    // Don't use a fallback on a "<player.name>" tag, for example, if there should always be a player present when the script runs.
    // That tag should only ever have a fallback when the script is meant to still work without a player attached.
    // If you carelessly apply fallbacks to all tags, you might end up not realizing there's a problem in your script until it's affecting real players.
    // You want to solve errors in testing, not ten months later when a player mentions to you "that shop NPC let me buy things even when I had $0"!
    //
    // Prior to Denizen 1.2.0, fallbacks exclusively worked with a special "||" syntax, like "- if <player||null> == null:"
    // This syntax is still fully supported at time of writing, however the newer tag-based format is considered clearer and easier to learn.
    //
    // -->
    public static final HashMap<String, TagManager.TagBaseData> fallbackTags = new HashMap<>();

    static {
        fallbackTags.put("if_null", new TagManager.TagBaseData("if_null", ObjectTag.class, (attribute) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            return attribute.getParamObject();
        }, false));
        fallbackTags.put("exists", new TagManager.TagBaseData("exists", ElementTag.class, (attribute) -> {
            return new ElementTag(false);
        }, false));
        fallbackTags.put("is_truthy", new TagManager.TagBaseData("is_truthy", ElementTag.class, (attribute) -> {
            return new ElementTag(false);
        }, false));
    }

    public boolean hasAlternative() {
        if (hadAlternative) {
            return true;
        }
        return getFallbackTagIndex() != -1;
    }

    public int getFallbackTagIndex() {
        for (int i = fulfilled + 1; i < attributes.length; i++) {
            if (fallbackTags.containsKey(attributes[i].key)) {
                return i;
            }
        }
        return -1;
    }

    public void setHadAlternative(boolean hadAlternative) {
        this.hadAlternative = hadAlternative;
        if (context != null && context.debug && hadAlternative) {
            context = context.clone();
            context.debug = false;
        }
    }

    public final long getLongParam() {
        try {
            if (hasParam()) {
                return Long.parseLong(getParam());
            }
        }
        catch (Exception ex) {
            if (!hasAlternative()) {
                Debug.echoError("Tag <" + this + "<W>> has invalid input - expected a (non-decimal) number, got '<A>" + getParam() + "<W>'...: " + ex.getMessage());
            }
        }
        return 0;
    }

    public final int getIntParam() {
        return getIntContext(1); // TODO
    }

    @Deprecated
    public final int getIntContext(int attribute) {
        try {
            if (hasContext(attribute)) {
                return Integer.parseInt(getContext(attribute));
            }
        }
        catch (Exception ex) {
            if (!hasAlternative()) {
                Debug.echoError("Tag <" + this + "<W>> has invalid input - expected a (non-decimal) number, got '<A>" + getContext(attribute) + "<W>'...: " + ex.getMessage());
            }
        }
        return 0;
    }

    public final double getDoubleParam() {
        return getDoubleContext(1); // TODO
    }

    @Deprecated
    public double getDoubleContext(int attribute) {
        try {
            if (hasContext(attribute)) {
                return Double.parseDouble(getContext(attribute));
            }
        }
        catch (NumberFormatException ex) {
            if (!hasAlternative()) {
                Debug.echoError("Tag <" + this + "<W>> has invalid input - expected a decimal number, got '<A>" + getContext(attribute) + "<W>'...: " + ex.getMessage());
            }
        }
        return 0;
    }

    public void echoError(Throwable ex) {
        if (!hasAlternative()) {
            Debug.echoError(ex);
        }
    }

    public void echoError(String message) {
        if (!hasAlternative()) {
            Debug.echoError(context, message);
        }
    }

    public String getAttribute(int num) {
        num += fulfilled - 1;
        if (num < 0 || num >= attributes.length) {
            return "";
        }
        return attributes[num].toString();
    }

    public String getAttributeWithoutParam(int num) {
        num += fulfilled - 1;
        if (num < 0 || num >= attributes.length) {
            return "";
        }
        return attributes[num].key;
    }

    public String filledString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fulfilled; i++) {
            if (contexts[i] != null) {
                sb.append(attributes[i].key).append("[").append(contexts[i]).append("].");
            }
            else {
                sb.append(attributes[i].toString()).append(".");
            }
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    public String unfilledString() {
        StringBuilder sb = new StringBuilder();
        for (int i = fulfilled; i < attributes.length; i++) {
            if (contexts[i] != null) {
                sb.append(attributes[i].key).append("[").append(contexts[i]).append("].");
            }
            else {
                sb.append(attributes[i].toString()).append(".");
            }
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attributes.length; i++) {
            if (filled != null) {
                switch (filled[i]) {
                    case 0:
                        sb.append("<Y>");
                        break;
                    case 1:
                        sb.append("<GR>");
                        break;
                    case 2:
                        sb.append("<R>");
                        break;
                    case 3:
                        sb.append("<LG>");
                        break;
                }
            }
            else {
                sb.append(i < fulfilled ? "<GR>" : (i == fulfilled ? "<R>" : "<Y>"));
            }
            sb.append(attributes[i].key);
            if (contexts[i] != null) {
                sb.append("<LG>[<A>").append(contexts[i]).append("<LG>].");
            }
            else if (attributes[i].rawParam != null) {
                sb.append("<LG>[").append(filled == null || filled[i] != 3 ? "<Y>" : "").append(attributes[i].rawParam).append("<LG>].");
            }
            else {
                sb.append("<LG>.");
            }
        }
        if (sb.length() > 0) {
            return sb.substring(0, sb.length() - 1);
        }
        return "";
    }
}
