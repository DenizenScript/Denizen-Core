package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.exceptions.TagProcessingException;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;

public class ReplaceableTagEvent {

    private final TagContext context;

    private boolean wasReplaced = false;

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

    public ReplaceableTagEvent(String tag, TagContext context) throws TagProcessingException {
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
                    Debug.echoError(context.entry, "(Initial detection) No tag-base handler for '" + startValue + "'.");
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

    public String getName() {
        return core_attributes.getAttributeWithoutContext(1);
    }

    @Deprecated
    public String getValue() {
        if (value_tagged == null) {
            value_tagged = TagManager.tag(mainRef.value, context);
        }
        return value_tagged;
    }

    @Deprecated
    public boolean hasValue() {
        return mainRef.value != null;
    }

    public TagRunnable.BaseInterface alternateBase;

    public ObjectTag getAlternative() {
        int index = core_attributes.getFallbackTagIndex();
        if (index != -1) {
            if (core_attributes.filled != null) {
                core_attributes.filled[core_attributes.fulfilled] = Boolean.FALSE;
            }
            core_attributes.fulfilled = index;
            alternateBase = Attribute.fallbackTags.get(core_attributes.getAttributeWithoutContext(1));
            return TagManager.readSingleTagObjectNoDebug(context, this);
        }
        if (mainRef.alternative != null) {
            return TagManager.tagObject(mainRef.alternative, context);
        }
        return null;
    }

    public boolean hasAlternative() {
        if (mainRef.alternative != null) {
            return true;
        }
        return core_attributes.hasAlternative();
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
        return core_attributes.toString() + (hasValue() ? ":" + mainRef.value : "") + (mainRef.alternative != null ? "||" + mainRef.alternative : "");
    }
}
