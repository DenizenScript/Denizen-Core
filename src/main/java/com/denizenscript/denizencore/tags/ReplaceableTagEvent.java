package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.exceptions.TagProcessingException;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;

public class ReplaceableTagEvent {

    private boolean wasReplaced = false;

    private String value_tagged = null;
    private Attribute core_attributes = null;

    public String raw_tag;

    public ObjectTag replaced_obj;

    public ObjectTag getReplacedObj() {
        return replaced_obj;
    }

    public static class ReferenceData {

        public Attribute attribs = null;

        public String alternative = null;

        public String rawTag = null;

        public String value = null;

        public TagManager.TagBaseData tagBase = null;

        public TagRunnable.BaseInterface<? extends ObjectTag> compiledStart;

        public boolean noGenerate = false;

        public int skippable = 0;

        public ObjectTag rawObject = null;
    }

    public ReferenceData mainRef = null;

    public static HashMap<String, ReferenceData> refs = new HashMap<>();

    public ReplaceableTagEvent(ReferenceData ref, String tag, TagContext context) {
        // If tag is not replaced, return the tag
        // TODO: Possibly make this return "null" ... might break some
        // scripts using tags incorrectly, but makes more sense overall
        this.replaced_obj = new ElementTag(tag);
        if (ref != null) {
            mainRef = ref;
            core_attributes = new Attribute(ref.attribs, context.entry, context, ref.skippable);
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
        raw_tag = tag.trim();
        core_attributes = new Attribute(raw_tag, context.entry, context);
        core_attributes.setHadAlternative(hasAlternative());
        mainRef.attribs = new Attribute(core_attributes, null, null, 0);
        mainRef.rawTag = raw_tag;
        String startValue = getName();
        mainRef.tagBase = TagManager.baseTags.get(startValue);
        if (mainRef.tagBase == null) {
            if (!hasAlternative()) {
                // This fires even for cases where it's intended, eg Depenizen optional features, so just trust the VS Code ext to handle bad-tag-base-detection in most cases
                if (CoreConfiguration.debugVerbose) {
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
        return core_attributes.getAttributeWithoutParam(1);
    }

    @Deprecated
    public String getValue() {
        if (value_tagged == null) {
            value_tagged = TagManager.tag(mainRef.value, core_attributes.context);
        }
        return value_tagged;
    }

    @Deprecated
    public boolean hasValue() {
        return mainRef.value != null;
    }

    public TagManager.TagBaseData alternateBase;

    public ObjectTag getAlternative() {
        int index = core_attributes.getFallbackTagIndex();
        if (index != -1) {
            if (core_attributes.filled != null) {
                core_attributes.filled[core_attributes.fulfilled] = 2;
            }
            core_attributes.fulfilled = index;
            alternateBase = Attribute.fallbackTags.get(core_attributes.getAttributeWithoutParam(1));
            return TagManager.readSingleTagObjectNoDebug(core_attributes.context, this);
        }
        if (mainRef.alternative != null) {
            return TagManager.tagObject(mainRef.alternative, core_attributes.context);
        }
        return null;
    }

    public boolean hasAlternative() {
        if (mainRef.alternative != null) {
            return true;
        }
        return core_attributes.hasAlternative();
    }

    public ScriptTag getScript() {
        return core_attributes.context.script;
    }

    public boolean replaced() {
        return wasReplaced && replaced_obj != null;
    }

    public void setReplacedObject(ObjectTag obj) {
        replaced_obj = obj;
        wasReplaced = obj != null;
    }

    public ScriptEntry getScriptEntry() {
        return core_attributes.context.entry;
    }

    public Attribute getAttributes() {
        return core_attributes;
    }

    @Override
    public String toString() {
        return core_attributes.toString() + (hasValue() ? ":" + mainRef.value : "") + (mainRef.alternative != null ? "||" + mainRef.alternative : "");
    }
}
