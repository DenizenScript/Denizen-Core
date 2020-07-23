package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;

public class ReplaceableTagEvent {

    private final TagContext context;

    private boolean wasReplaced = false;

    private ObjectTag alternative_tagged = null;
    private String replaced;
    private String value_tagged = null;
    private Attribute core_attributes = null;

    public String raw_tag;

    public ObjectTag replaced_obj = null;

    public ObjectTag getReplacedObj() {
        if (replaced_obj == null) {
            if (replaced == null) {
                return null;
            }
            replaced_obj = new ElementTag(replaced);
        }
        return replaced_obj;
    }

    ////////////
    // Constructors

    public static class ReferenceData {

        public Attribute attribs = null;

        public String alternative = null;

        public String rawTag = null;

        public String value = null;

        public TagRunnable.RootForm rootFormHandler = null;

        public TagRunnable.BaseInterface tagBaseHandler = null;
    }

    public ReferenceData mainRef = null;

    public static HashMap<String, ReferenceData> refs = new HashMap<>();

    public ReplaceableTagEvent(ReferenceData ref, String tag, TagContext context) {
        // Reference context
        this.context = context;

        // If tag is not replaced, return the tag
        // TODO: Possibly make this return "null" ... might break some
        // scripts using tags incorrectly, but makes more sense overall
        this.replaced = tag;

        if (ref != null) {
            mainRef = ref;
            core_attributes = new Attribute(ref.attribs, context.entry, context);
            raw_tag = ref.rawTag;
        }
    }

    public ReplaceableTagEvent(String tag, TagContext context) {
        this(refs.get(tag), tag, context);
        if (mainRef != null) {
            return;
        }
        String otag = tag;

        mainRef = new ReferenceData();

        // Get alternative text
        int alternativeLoc = locateAlternative(tag);

        if (alternativeLoc >= 0) {
            // get rid of the || at the alternative's start and any trailing spaces
            mainRef.alternative = tag.substring(alternativeLoc + 2).trim();
            // remove found alternative from tag
            tag = tag.substring(0, alternativeLoc);
        }

        // Get value (if present)
        int valueLoc = locateValue(tag);

        if (valueLoc > 0) {
            mainRef.value = tag.substring(valueLoc + 1);
            tag = tag.substring(0, valueLoc);
        }

        // Alternatives are stripped, value is stripped, let's remember the raw tag for the attributer.
        raw_tag = tag.trim();

        // Use Attributes system to get type/subtype/etc. etc. for 'static/legacy' tags.
        core_attributes = new Attribute(raw_tag, context.entry, context);
        core_attributes.setHadAlternative(hasAlternative());

        mainRef.attribs = new Attribute(core_attributes, null, null);
        mainRef.rawTag = raw_tag;

        String startValue = getName();
        mainRef.tagBaseHandler = TagManager.baseHandlers.get(startValue);
        if (mainRef.tagBaseHandler == null) {
            mainRef.rootFormHandler = TagManager.rootFormHandlers.get(startValue);
            if (mainRef.rootFormHandler == null) {
                if (!hasAlternative()) {
                    Debug.echoError("(Initial detection) No tag-base handler for '" + startValue + "'.");
                }
            }
        }
        refs.put(otag, mainRef);
    }

    private static int locateValue(String tag) {
        int bracks = 0;
        int bracks2 = 0;
        for (int i = 0; i < tag.length(); i++) {
            char c = tag.charAt(i);
            if (c == '<') {
                bracks++;
            }
            else if (c == '>') {
                bracks--;
            }
            else if (bracks == 0 && c == '[') {
                bracks2++;
            }
            else if (bracks == 0 && c == ']') {
                bracks2--;
            }
            else if (c == ':' && bracks == 0 && bracks2 == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int locateAlternative(String tag) {
        int bracks = 0;
        int bracks2 = 0;
        boolean previousWasTarget = false;
        for (int i = 0; i < tag.length(); i++) {
            char c = tag.charAt(i);
            if (c == '<') {
                bracks++;
            }
            else if (c == '>') {
                bracks--;
            }
            else if (bracks == 0 && c == '[') {
                bracks2++;
            }
            else if (bracks == 0 && c == ']') {
                bracks2--;
            }
            else if (c == '|' && bracks == 0 && bracks2 == 0) {
                if (previousWasTarget) {
                    return i - 1;
                }
                else {
                    previousWasTarget = true;
                }
            }
            else {
                previousWasTarget = false;
            }
        }
        return -1;
    }

    public boolean matches(String tagName) {
        return getName().equals(tagName);
    }

    public boolean matches(String... tagNames) {
        String name = getName();
        for (String string : tagNames) {
            if (name.equals(string)) {
                return true;
            }
        }
        return false;
    }

    ////////
    // Replaceable Tag 'Parts'
    // <name.type.subtype.specifier:value>

    // Name

    public String getName() {
        return core_attributes.getAttributeWithoutContext(1);
    }

    public String getNameContext() {
        return core_attributes.getContext(1);
    }

    public boolean hasNameContext() {
        return core_attributes.hasContext(1);
    }

    // Type

    @Deprecated
    public String getType() {
        return core_attributes.getAttributeWithoutContext(2);
    }

    @Deprecated
    public boolean hasType() {
        return core_attributes.getAttribute(2).length() > 0;
    }

    @Deprecated
    public String getTypeContext() {
        return core_attributes.getContext(2);
    }

    @Deprecated
    public boolean hasTypeContext() {
        return core_attributes.hasContext(2);
    }

    // Subtype

    @Deprecated
    public String getSubType() {
        return core_attributes.getAttributeWithoutContext(3);
    }

    @Deprecated
    public boolean hasSubType() {
        return core_attributes.getAttribute(3).length() > 0;
    }

    @Deprecated
    public String getSubTypeContext() {
        return core_attributes.getContext(3);
    }

    @Deprecated
    public boolean hasSubTypeContext() {
        return core_attributes.hasContext(3);
    }

    // Specifier

    @Deprecated
    public String getSpecifier() {
        return core_attributes.getAttributeWithoutContext(4);
    }

    @Deprecated
    public boolean hasSpecifier() {
        return core_attributes.getAttribute(4).length() > 0;
    }

    @Deprecated
    public String getSpecifierContext() {
        return core_attributes.getContext(4);
    }

    @Deprecated
    public boolean hasSpecifierContext() {
        return core_attributes.hasContext(4);
    }

    // Value

    public String getValue() {
        if (value_tagged == null) {
            value_tagged = TagManager.tag(mainRef.value, context);
        }
        return value_tagged;
    }

    public boolean hasValue() {
        return mainRef.value != null;
    }

    // Alternative

    public ObjectTag getAlternative() {
        if (!hasAlternative()) {
            return null;
        }
        if (alternative_tagged != null) {
            return alternative_tagged;
        }
        alternative_tagged = TagManager.tagObject(mainRef.alternative, context);
        return alternative_tagged;
    }

    public boolean hasAlternative() {
        return mainRef.alternative != null;
    }

    // Other internal mechanics

    public TagContext getContext() {
        return context;
    }

    public String getReplaced() {
        if (replaced == null && replaced_obj != null) {
            replaced = replaced_obj.toString();
        }
        return replaced;
    }

    public ScriptTag getScript() {
        return context.script;
    }

    public boolean replaced() {
        return wasReplaced && (replaced != null || replaced_obj != null);
    }

    public void setReplacedObject(ObjectTag obj) {
        replaced_obj = obj;
        replaced = null;
        wasReplaced = obj != null;
    }

    public void setReplaced(String string) {
        if (Debug.verbose) {
            try {
                throw new RuntimeException("Trace");
            }
            catch (Exception ex) {
                Debug.echoError(ex);
            }
            Debug.log("Tag " + raw_tag + " updating to value: " + string);
        }
        replaced = string;
        replaced_obj = null;
        wasReplaced = string != null;
    }

    public boolean hasScriptEntryAttached() {
        return context.entry != null;
    }

    public ScriptEntry getScriptEntry() {
        return context.entry;
    }

    /**
     * Gets an Attribute object for easy parsing/reading
     * of the different tag attributes.
     *
     * @return attributes
     */

    public Attribute getAttributes() {
        return core_attributes;
    }

    @Override
    public String toString() {
        return core_attributes.toString() + (hasValue() ? ":" + mainRef.value : "") + (hasAlternative() ? "||" + mainRef.alternative : "");
    }
}
