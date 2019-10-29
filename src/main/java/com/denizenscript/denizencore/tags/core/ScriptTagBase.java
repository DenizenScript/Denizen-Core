package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.Deprecations;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

public class ScriptTagBase {

    public ScriptTagBase() {

        // <--[tag]
        // @attribute <script[<script>]>
        // @returns ScriptTag
        // @description
        // Returns a script object constructed from the input value.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                scriptTags(event);
            }
        }, "script", "s");
    }

    public void scriptTags(ReplaceableTagEvent event) {

        if (!event.matches("script", "s") || event.replaced()) {
            return;
        }

        if (event.matches("s")) {
            Deprecations.scriptShorthand.warn(event.getScriptEntry());
        }

        // Stage the location
        ScriptTag script = null;

        // Check name context for a specified script, or check
        // the ScriptEntry for a 'script' context
        if (event.hasNameContext()) {
            if (!ScriptTag.matches(event.getNameContext())) {
                if (!event.hasAlternative()) {
                    Debug.echoError("Script '" + event.getNameContext() + "' does not exist.");
                }
                return;
            }
            script = ScriptTag.valueOf(event.getNameContext(), event.getAttributes().context);
        }
        else if (event.getScript() != null) {
            script = event.getScript();
        }
        else if (event.getScriptEntry() == null) {
            if (!event.hasAlternative()) {
                Debug.echoError("No applicable script for <script> tag.");
            }
            return;
        }
        else if (event.getScriptEntry().getScript() != null) {
            script = event.getScriptEntry().getScript();
        }
        else if (event.getScriptEntry().hasObject("script")) {
            script = (ScriptTag) event.getScriptEntry().getObject("script");
        }

        // Build and fill attributes
        Attribute attribute = event.getAttributes();

        if (script == null) {
            if (!event.hasAlternative()) {
                Debug.echoError("No applicable script for <script> tag.");
            }
            return;
        }

        // Else, get the attribute from the script
        event.setReplacedObject(CoreUtilities.autoAttrib(script, attribute.fulfill(1)));

    }
}
