package net.aufdemrand.denizencore.tags.core;

import net.aufdemrand.denizencore.objects.TagRunnable;
import net.aufdemrand.denizencore.objects.dScript;
import net.aufdemrand.denizencore.tags.Attribute;
import net.aufdemrand.denizencore.tags.ReplaceableTagEvent;
import net.aufdemrand.denizencore.tags.TagManager;
import net.aufdemrand.denizencore.utilities.CoreUtilities;
import net.aufdemrand.denizencore.utilities.debugging.SlowWarning;

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
