package com.denizenscript.denizencore.tags.core;

import com.denizenscript.denizencore.objects.core.ScriptTag;
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
        TagManager.registerStaticTagBaseHandler(ScriptTag.class, "script", (attribute) -> {
            ScriptTag script = null;
            if (attribute.hasParam()) {
                script = attribute.paramAsType(ScriptTag.class);
                if (script == null) {
                    attribute.echoError("Script '" + attribute.getParam() + "' does not exist.");
                    return null;
                }
            }
            else if (attribute.context.script != null) {
                script = attribute.context.script;
            }
            else if (attribute.context.entry == null) {
                attribute.echoError("No applicable script for <script> tag.");
                return null;
            }
            else if (attribute.context.entry.getScript() != null) {
                script = attribute.context.entry.getScript();
            }
            else if (attribute.context.entry.hasObject("script")) {
                script = (ScriptTag) attribute.context.entry.getObject("script");
            }
            if (script == null) {
                attribute.echoError("No applicable script for <script> tag.");
                return null;
            }
            return script;
        });
    }
}
