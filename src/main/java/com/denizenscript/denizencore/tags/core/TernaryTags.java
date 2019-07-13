package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.SlowWarning;

public class TernaryTags {

    public TernaryTags() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                ternaryTag(event);
            }
        }, "ternary", "tern", "t");
    }

    public SlowWarning ternShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'tern' instead of 't' as a root tag.");

    // <--[tag]
    // @attribute <tern[<condition>].pass[<element>]||<element>>
    // @returns ElementTag
    // @description
    // Returns either the 'pasas' element, or fallback element depending on
    // the outcome of the condition. The 'pass' element will show when the condition returns 'true',
    // otherwise the fallback element will show.
    // Example: '<tern[<player.is_spawned>].pass[Player is spawned!] || Player is not spawned!>'
    // -->
    public void ternaryTag(ReplaceableTagEvent event) {
        if (!event.matches("ternary", "tern", "t")) {
            return;
        }
        if (event.matches("t")) {
            ternShorthand.warn(event.getScriptEntry());
        }

        Attribute attribute = event.getAttributes();

        // Fallback if nothing to evaluate
        if (!attribute.hasContext(1)) {
            return;
        }

        if (attribute.getContext(1).equalsIgnoreCase("true")) {
            ObjectTag passValue;
            if (event.hasValue()) {
                passValue = new ElementTag(event.getValue().trim());
                attribute = attribute.fulfill(1);
            }
            else if (attribute.hasContext(2)) {
                passValue = attribute.getContextObject(2);
                attribute = attribute.fulfill(2);
            }
            else {
                Debug.echoError("Ternary tag missing 'pass' value!");
                return;
            }
            event.setReplacedObject(passValue.getObjectAttribute(attribute));
        }
    }

}
