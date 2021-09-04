package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.exceptions.TagProcessingException;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.MapTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.*;

public class Attribute {

    public static class AttributeComponent {

        public final String rawKey;

        public final String key;

        public final String context;

        public AttributeComponent(String inp) {
            if (inp.endsWith("]") && CoreUtilities.contains(inp, '[')) {
                int ind = inp.indexOf('[');
                rawKey = inp.substring(0, ind);
                context = inp.substring(ind + 1, inp.length() - 1);
            }
            else {
                rawKey = inp;
                context = null;
            }
            key = CoreUtilities.toLowerCase(rawKey);
        }

        @Override
        public String toString() {
            if (context != null) {
                return key + "[" + context + "]";
            }
            return key;
        }
    }

    private static HashMap<String, AttributeComponent[]> attribsLookup = new HashMap<>();

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
                matches.add(new AttributeComponent(attributes.substring(x1, x2)));
                x2 = -1;
                x1 = x + 1;
            }
        }
        if (braced != 0) {
            throw new TagProcessingException("The tag '" + attributes + "' is invalid due to misplaced [square brackets]. Did you forget to close some brackets?");
        }
        if (Debug.verbose) {
            Debug.log("attribute splitter: '" + attributes + "' becomes: " + matches);
        }
        matchesRes = new AttributeComponent[matches.size()];
        matchesRes = matches.toArray(matchesRes);
        attribsLookup.put(attributes, matchesRes);
        return matchesRes;
    }

    public AttributeComponent[] attributes;
    public ObjectTag[] contexts;
    public Boolean[] filled;

    ScriptEntry scriptEntry;
    public TagContext context;

    String origin;

    public List<String> seemingSuccesses = new ArrayList<>(2);

    /**
     * Last valid object while parsing this attribute chain, for debugging purposes.
     */
    public ObjectTag lastValid;

    public boolean hasContextFailed = false;

    public void resetErrorTrack() {
        if (Debug.verbose) {
            Debug.echoError("(Verbose) Attribute - error track reset");
        }
        seemingSuccesses.clear();
        hasContextFailed = false;
    }

    public ScriptEntry getScriptEntry() {
        return scriptEntry;
    }

    public String getOrigin() {
        return origin;
    }

    public Attribute(Attribute ref, ScriptEntry scriptEntry, TagContext context) {
        origin = ref.origin;
        this.scriptEntry = scriptEntry;
        this.context = context;
        attributes = ref.attributes;
        contexts = new ObjectTag[attributes.length];
        setHadAlternative(ref.hadAlternative);
        if (context == null || context.debug) {
            filled = new Boolean[attributes.length];
        }
    }

    public Attribute(String attributes, ScriptEntry scriptEntry, TagContext context) throws TagProcessingException {
        origin = attributes;
        this.scriptEntry = scriptEntry;
        this.context = context;
        this.attributes = separate_attributes(attributes);
        contexts = new ObjectTag[this.attributes.length];
        if (context == null || context.debug) {
            filled = new Boolean[this.attributes.length];
        }
    }

    public boolean matches(String string) {
        if (fulfilled >= attributes.length) {
            return false;
        }
        return attributes[fulfilled].key.equals(string);
    }

    public boolean startsWith(String string) {
        if (fulfilled >= attributes.length) {
            return false;
        }
        if (Debug.verbose) {
            Debug.log("Trying tag startsWith " + string + " on tag " + toString());
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
            if (Debug.verbose) {
                Debug.log("Chain-Tag found!");
            }
            seemingSuccesses.add(string);
            return true;
        }
        if (attributes[fulfilled].key.equals(string)) {
            if (Debug.verbose) {
                Debug.log("Sub-tag found!");
            }
            seemingSuccesses.add(string);
            return true;
        }
        return false;
    }

    public boolean startsWith(String string, int attribute) {
        return CoreUtilities.toLowerCase(getAttributeWithoutContext(attribute)).equals(string);
    }

    int fulfilled = 0;

    public boolean isComplete() {
        return fulfilled >= attributes.length;
    }

    public Attribute fulfill(int attributes) {
        resetErrorTrack();
        if (filled != null) {
            for (int i = 0; i < attributes; i++) {
                if (fulfilled + i < filled.length) {
                    filled[fulfilled + i] = Boolean.TRUE;
                }
            }
        }
        fulfilled += attributes;
        return this;
    }

    public boolean hasContext(int attribute) {
        attribute += fulfilled - 1;
        if (attribute < 0 || attribute >= attributes.length) {
            return false;
        }
        if (attributes[attribute].context != null) {
            return true;
        }
        if (Debug.verbose) {
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

    public String getRawContext(int attribute) {
        attribute += fulfilled - 1;
        if (attribute < 0 || attribute >= attributes.length) {
            return null;
        }
        return attributes[attribute].context;
    }

    public ObjectTag parseDynamicContext(int attribute, OverridingDefinitionProvider customProvider) {
        String inp = getRawContext(attribute);
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

    public <T extends ObjectTag> T contextAsType(int attribute, Class<T> dClass) {
        ObjectTag contextObj = getContextObject(attribute);
        if (contextObj == null) {
            return null;
        }
        return CoreUtilities.asType(contextObj, dClass, context);
    }

    public ObjectTag getContextObject(int attribute) {
        attribute += fulfilled - 1;
        if (attribute < 0 || attribute >= attributes.length) {
            return null;
        }
        ObjectTag tagged = contexts[attribute];
        if (tagged != null) {
            return tagged;
        }
        String inp = attributes[attribute].context;
        if (inp == null) {
            return null;
        }
        tagged = TagManager.tagObject(inp, context);
        contexts[attribute] = tagged;
        return tagged;
    }

    public String getContext(int attribute) {
        return CoreUtilities.stringifyNullPass(getContextObject(attribute));
    }

    private boolean hadAlternative = false;

    // <--[language]
    // @name Tag Fallbacks
    // @group Tag System
    // @description
    // Tag fallbacks (AKA "tag alternatives") are a system designed to allow scripters to automatically handle tag errors.
    //
    // Fallbacks are implemented as special "magic tags" that look like any other tag-part, but override the error handler. These are "if_null" and "exists".
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
    public static final HashMap<String, TagRunnable.BaseInterface> fallbackTags = new HashMap<>();

    static {
        fallbackTags.put("if_null", (attribute) -> {
            if (!attribute.hasContext(1)) {
                return null;
            }
            return attribute.getContextObject(1);
        });
        fallbackTags.put("exists", (attribute) -> {
            return new ElementTag(false);
        });
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

    public long getLongContext(int attribute) {
        try {
            if (hasContext(attribute)) {
                return Long.parseLong(getContext(attribute));
            }
        }
        catch (Exception ex) {
            if (!hasAlternative()) {
                Debug.echoError("Tag <" + toString() + "> has invalid input - expected a number, got '" + getContext(attribute) + "'...: " + ex.getMessage());
            }
        }
        return 0;
    }

    public int getIntContext(int attribute) {
        try {
            if (hasContext(attribute)) {
                return Integer.parseInt(getContext(attribute));
            }
        }
        catch (Exception ex) {
            if (!hasAlternative()) {
                Debug.echoError("Tag <" + toString() + "> has invalid input - expected a number, got '" + getContext(attribute) + "'...: " + ex.getMessage());
            }
        }
        return 0;
    }

    public double getDoubleContext(int attribute) {
        try {
            if (hasContext(attribute)) {
                return Double.parseDouble(getContext(attribute));
            }
        }
        catch (NumberFormatException ex) {
            if (!hasAlternative()) {
                Debug.echoError("Tag <" + toString() + "> has invalid input - expected a decimal number, got '" + getContext(attribute) + "'...: " + ex.getMessage());
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

    public String getAttributeWithoutContext(int num) {
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
                sb.append(filled[i] == null ? "<Y>" : (filled[i] ? "<GR>" : "<R>"));
            }
            else {
                sb.append(i < fulfilled ? "<GR>" : (i == fulfilled ? "<R>" : "<Y>"));
            }
            sb.append(attributes[i].key);
            if (contexts[i] != null) {
                sb.append("<LG>[<A>").append(contexts[i]).append("<LG>].");
            }
            else if (attributes[i].context != null) {
                sb.append("<LG>[<Y>").append(attributes[i].context).append("<LG>].");
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
