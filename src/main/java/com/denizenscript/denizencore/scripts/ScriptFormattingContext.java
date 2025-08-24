package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.containers.core.FormatScriptContainer;
import com.denizenscript.denizencore.tags.ParseableTag;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record ScriptFormattingContext(Map<String, ParseableTag> formats, ParseableTag singleFormat) {
    
    // <--[language]
    // @name Script Formats
    // @group Script Container System
    // @description
    // Script formats provide the formats certain commands within that script will use for their texts. Most notably, this includes <@link command debug>.
    // See specific command's documentation for information on which formats they use (for example, the 'debug' command supports a 'debug' format and an 'error' format).
    // The formats are specified under a 'formats' key, and can be either <@link language Format Script Containers> or a direct format with the same syntax as format scripts.
    // When specifying a direct format, use the format name as the key; When specifying a format script, use '<format>_script' as the key (see example below).
    // <code>
    // my_project_task:
    //     type: task
    //     formats:
    //         # A direct format
    //         debug: [MyProject] <[text]>
    //         # A separate format script
    //         error_script: my_project_error
    //     script:
    //     - if <util.real_time_since_start.in_hours> > 20:
    //         # Will be formatted by the 'my_project_error' format script.
    //         - debug error "The system has been running for over 20 hours! Please restart!"
    //     - else:
    //         # Will print "[MyProject] The system does not need a restart yet."
    //         - debug "The system does not need a restart yet."
    // </code>
    // -->

    public static final Set<String> FORMAT_TYPES = new HashSet<>();

    public static String registerFormatType(String name) {
        String nameLower = CoreUtilities.toLowerCase(name);
        if (!FORMAT_TYPES.add(nameLower)) {
            throw new IllegalArgumentException("Tried registering duplicate format type! format '" + name + "' already exists.");
        }
        return nameLower;
    }

    public static ScriptFormattingContext parseFromConfiguration(ScriptContainer script) {
        YamlConfiguration formatsConfig = script.getConfigurationSection("formats");
        if (formatsConfig == null) {
            return null;
        }
        Map<String, ParseableTag> formats = new HashMap<>();
        TagContext context = null;
        for (String formatType : FORMAT_TYPES) {
            String rawFormat = formatsConfig.getString(formatType);
            if (rawFormat != null) {
                if (context == null) {
                    context = DenizenCore.implementation.getTagContext(script);
                }
                formats.put(formatType, TagManager.parseTextToTag(rawFormat, context));
                continue;
            }
            String formatScriptInput = formatsConfig.getString(formatType + "_script");
            if (formatScriptInput == null) {
                continue;
            }
            FormatScriptContainer formatScript = ScriptRegistry.getScriptContainerAs(formatScriptInput, FormatScriptContainer.class);
            if (formatScript == null || formatScript.getFormatTag() == null) {
                Debug.echoError(script, "Invalid format script '" + formatScriptInput + "' specified for format '" + formatType + "'.");
                continue;
            }
            formats.put(formatType, formatScript.getFormatTag());
        }
        if (formats.isEmpty()) {
            Debug.echoError(script, "Invalid formats config, must specify at least one valid format.");
            return null;
        }
        return new ScriptFormattingContext(formats, null);
    }

    public boolean hasFormat(String formatType) {
        return singleFormat != null || formats.containsKey(formatType);
    }

    public String formatOrNull(String formatType, String rawText, ScriptEntry entry) {
        ParseableTag formatTag = singleFormat == null ? formats.get(formatType) : singleFormat;
        return formatTag != null ? FormatScriptContainer.formatText(formatTag, rawText, entry.getContext(), entry.getScript()) : null;
    }

    public String format(String formatType, String rawText, ScriptEntry entry) {
        String formatted = formatOrNull(formatType, rawText, entry);
        return formatted != null ? formatted : rawText;
    }
}
