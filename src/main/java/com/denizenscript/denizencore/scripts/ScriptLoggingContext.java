package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.FormatScriptContainer;
import com.denizenscript.denizencore.tags.ParseableTag;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public record ScriptLoggingContext(ParseableTag debugFormat, ParseableTag errorFormat) {
    
    // <--[language]
    // @name Script Logging Format
    // @group Script Container System
    // @description
    // Script logging contexts provide the format certain commands will use for their texts.
    // The formats are specified under a 'logging_format' key, and can be either a <@link language Format Script Containers> or a direct format with the same syntax as format scripts.
    // <code>
    // my_project_task:
    //     type: task
    //     logging_format:
    //         # A direct format
    //         debug: [MyProject]: <[text]>
    //         # A separate format script
    //         error: my_project_error
    //     script:
    //     - if <util.real_time_since_start.in_hours> > 20:
    //         # Will be formatted by the 'my_project_error' format script.
    //         - debug error "The system has been running for over 20 hours! Please restart!"
    //     - else:
    //         # Will print "[MyProject]: The system does not need a restart yet."
    //         - debug "The system does not need a restart yet."
    // </code>
    // -->

    public static ScriptLoggingContext parseFromConfiguration(ScriptContainer script) {
        YamlConfiguration loggingConfig = script.getConfigurationSection("logging_format");
        if (loggingConfig == null) {
            return null;
        }
        String rawDebugFormat = loggingConfig.getString("debug"), rawErrorFormat = loggingConfig.getString("error");
        if (rawDebugFormat == null && rawErrorFormat == null) {
            Debug.echoError(script, "Invalid logging config, must specify at least one of 'debug' or 'error'.");
            return null;
        }
        return new ScriptLoggingContext(fromFormatScriptOrParse(rawDebugFormat, script), fromFormatScriptOrParse(rawErrorFormat, script));
    }

    private static ParseableTag fromFormatScriptOrParse(String rawFormat, ScriptContainer owner) {
        FormatScriptContainer formatScript = ScriptRegistry.getScriptContainerAs(rawFormat, FormatScriptContainer.class);
        return formatScript != null && formatScript.getFormatTag() != null ? formatScript.getFormatTag() : TagManager.parseTextToTag(rawFormat, DenizenCore.implementation.getTagContext(owner));
    }

    public boolean hasDebugFormat() {
        return debugFormat != null;
    }

    public String formatDebug(String rawDebug, ScriptEntry entry) {
        return FormatScriptContainer.formatText(debugFormat, rawDebug, entry.getContext(), entry.getScript());
    }

    public boolean hasErrorFormat() {
        return errorFormat != null;
    }

    public String formatError(String rawError, ScriptEntry entry) {
        return FormatScriptContainer.formatText(errorFormat, rawError, entry.getContext(), entry.getScript());
    }
}
