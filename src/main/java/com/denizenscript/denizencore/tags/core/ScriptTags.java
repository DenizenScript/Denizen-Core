package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.TagRunnable;
import com.denizenscript.denizencore.objects.dScript;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.SlowWarning;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

public class ScriptTags {

    public ScriptTags() {
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                scriptTags(event);
            }
        }, "script", "s");
    }

    public SlowWarning scriptShorthand = new SlowWarning("Short-named tags are hard to read. Please use 'script' instead of 's' as a root tag.");

    public void scriptTags(ReplaceableTagEvent event) {

        if (!event.matches("script", "s") || event.replaced()) {
            return;
        }

        if (event.matches("s")) {
            scriptShorthand.warn(event.getScriptEntry());
        }

        // Stage the location
        dScript script = null;

        // Check name context for a specified script, or check
        // the ScriptEntry for a 'script' context
        if (event.hasNameContext() && dScript.matches(event.getNameContext())) {
            script = dScript.valueOf(event.getNameContext(), event.getAttributes().context);
        }
        else if (event.getScript() != null) {
            script = event.getScript();
        }
        else if (event.getScriptEntry() == null) {
            return;
        }
        else if (event.getScriptEntry().getScript() != null) {
            script = event.getScriptEntry().getScript();
        }
        else if (event.getScriptEntry().hasObject("script")) {
            script = (dScript) event.getScriptEntry().getObject("script");
        }

        // Build and fill attributes
        Attribute attribute = event.getAttributes();

        // Check if location is null, return null if it is
        if (script == null) {
            return;
        }

        // Else, get the attribute from the script
        event.setReplacedObject(CoreUtilities.autoAttrib(script, attribute.fulfill(1)));

    }
}
