package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ReplaceableTagEvent;
import com.denizenscript.denizencore.tags.TagManager;

public class ScriptTagBase {

    public ScriptTagBase() {

        // <--[tag]
        // @attribute <script[(<script>)]>
        // @returns ScriptTag
        // @description
        // Returns a script object constructed from the input value.
        // If no input is given, will return the current script that the tag is within.
        // Refer to <@link ObjectType ScriptTag>.
        // -->
        TagManager.registerTagHandler(new TagRunnable.RootForm() {
            @Override
            public void run(ReplaceableTagEvent event) {
                scriptTags(event);
            }
        }, "script");
    }

    public void scriptTags(ReplaceableTagEvent event) {
        if (!event.matches("script") || event.replaced()) {
            return;
        }
        Attribute attribute = event.getAttributes();
        ScriptTag script = null;
        if (attribute.hasContext(1)) {
            script = attribute.contextAsType(1, ScriptTag.class);
            if (script == null) {
                event.getAttributes().echoError("Script '" + attribute.getContext(1) + "' does not exist.");
                return;
            }
        }
        else if (event.getScript() != null) {
            script = event.getScript();
        }
        else if (event.getScriptEntry() == null) {
            attribute.echoError("No applicable script for <script> tag.");
            return;
        }
        else if (event.getScriptEntry().getScript() != null) {
            script = event.getScriptEntry().getScript();
        }
        else if (event.getScriptEntry().hasObject("script")) {
            script = (ScriptTag) event.getScriptEntry().getObject("script");
        }
        if (script == null) {
            if (!event.hasAlternative()) {
                Debug.echoError("No applicable script for <script> tag.");
            }
            return;
        }
        event.setReplacedObject(CoreUtilities.autoAttrib(script, attribute.fulfill(1)));
    }
}
