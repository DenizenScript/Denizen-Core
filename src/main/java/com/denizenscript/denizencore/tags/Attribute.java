package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static AttributeComponent[] separate_attributes(String attributes) {
        AttributeComponent[] matchesRes = attribsLookup.get(attributes);
        if (matchesRes != null) {
            return matchesRes;
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
                matches.add(new AttributeComponent(attributes.substring(x1, x2)));
                x2 = -1;
                x1 = x + 1;
            }
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

    ScriptEntry scriptEntry;
    public TagContext context;

    String origin;

    public List<String> seemingSuccesses = new ArrayList<>(2);

    public boolean hasContextFailed = false;

    public void resetErrorTrack() {
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
    }

    public Attribute(String attributes, ScriptEntry scriptEntry, TagContext context) {
        origin = attributes;
        this.scriptEntry = scriptEntry;
        this.context = context;
        this.attributes = separate_attributes(attributes);
        contexts = new ObjectTag[this.attributes.length];
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
        if (string.indexOf('.') >= 0) {
            if (Debug.verbose) {
                Debug.log("Trying tag startsWith " + string + " on tag " + toString());
            }
            List<String> tmp = CoreUtilities.split(string, '.');
            if (tmp.size() + fulfilled > attributes.length) {
                return false;
            }
            for (int i = 0; i < tmp.size(); i++) {
                if (!attributes[fulfilled + i].key.equals(tmp.get(i))) {
                    return false;
                }
            }
            seemingSuccesses.add(string);
            return true;
        }
        if (attributes[fulfilled].key.equals(string)) {
            seemingSuccesses.add(string);
            return true;
        }
        return false;
    }

    public boolean startsWith(String string, int attribute) {
        return CoreUtilities.toLowerCase(getAttribute(attribute)).startsWith(string);
    }

    int fulfilled = 0;

    public boolean isComplete() {
        return fulfilled >= attributes.length;
    }

    public Attribute fulfill(int attributes) {
        resetErrorTrack();
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
        hasContextFailed = true;
        return false;
    }

    public static class OverridingDefinitionProvider implements DefinitionProvider {
        public DefinitionProvider originalProvider;
        public HashMap<String, ObjectTag> altDefs = new HashMap<>();
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
        public Map<String, ObjectTag> getAllDefinitions() {
            return originalProvider.getAllDefinitions();
        }
        @Override
        public ObjectTag getDefinitionObject(String definition) {
            ObjectTag result = altDefs.get(CoreUtilities.toLowerCase(definition));
            if (result != null) {
                return result;
            }
            return originalProvider.getDefinitionObject(definition);
        }

        @Override
        public String getDefinition(String definition) {
            ObjectTag result = altDefs.get(CoreUtilities.toLowerCase(definition));
            if (result != null) {
                return result.toString();
            }
            return originalProvider.getDefinition(definition);
        }

        @Override
        public boolean hasDefinition(String definition) {
            ObjectTag result = altDefs.get(CoreUtilities.toLowerCase(definition));
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
    // A tag without a fallback might look like "<player.name>".
    // This tag works fine as long as there's a linked player, but what if a player isn't always available?
    // Normally, this situation would display an error in the console debug logs, and return plaintext "player.name" in the script.
    // A fallback can help us handle the problem more gracefully.
    // That same tag with a fallback would look like "<player.name||Steve>".
    // Now, when there isn't a player available, there will not be an error, and the tag will simply return "Steve".
    //
    // This format is the same for basically all tags. "<main.tag.here||Fallback here>".
    // For another example, "<player.flag[myflag]||0>" returns either the value of the flag, or "0" if the flag is not present (or if there's no player).
    //
    // This is particularly useful for things like checking whether an object exists / is valid.
    // What if we want to check if there even is a linked player? We don't have a "<has_player>" tag to do that, so what can we do?
    // <code>
    // - if <player||null> == null:
    // </code>
    // The above example demonstrates using a fallback to check if a player is valid.
    // The if block will run only if there is not a player valid (you might, for example, place the "stop" command inside).
    //
    // We use the word "null" in the above example. This is a common programming term that means "no object is present".
    // In this case, that term isn't actually a functionality of Denizen, it's just a word we choose for clarity.
    // You could just as easily do "- if <player||nothing> == nothing:", or for that matter "- if <player||cheese> == cheese:".
    // A player object takes the form "p@uuid", so it will therefore never exactly match any simple word, so there's no coincidental match edge-case to worry about.
    // Note that this won't work so perfect for things like a user input or fully dynamic value,
    // so in those cases you may want to use a more specialized check. For example, with flags, the "has_flag" tag is available for this purpose.
    //
    // Fallbacks can be tags themselves. So, for example, if we want either a custom flag-based display name, or if not available, the player's base name,
    // we can do: "<player.flag[display_name]||<player.name>>".
    // You can as well chain these, though that starts to get ugly pretty fast: "<player.flag[good_name]||<player.flag[bad_name]||<player.name>>>".
    //
    // Note that fallbacks will *hide errors*. Generally, the only errors you should ever hide are ones you're expecting that are fine.
    // Don't use a fallback on a "<player.name>" tag, for example, if there should always be a player present when the script runs.
    // That tag should only ever have a fallback when the script is meant to still work without a player attached.
    // If you carelessly apply fallbacks to all tags, you might end up not realizing there's a problem in your script until it's affecting real players.
    // You want to solve errors in testing, not ten months later when a player mentions to you "that shop NPC let me buy things even when I had $0"!
    //
    // -->
    public boolean hasAlternative() {
        return hadAlternative;
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
            Debug.echoError(message);
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
}
