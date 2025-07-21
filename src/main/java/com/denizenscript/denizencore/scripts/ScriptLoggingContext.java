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

public record ScriptLoggingContext(Map<Key, ParseableTag> formats, ParseableTag singleFormat) {
    
    // <--[language]
    // @name Script Logging Format
    // @group Script Container System
    // @description
    // Script logging contexts provide the format certain commands will use for their texts. Most notably, this includes <@link command debug>.
    // See specific command's documentation for information on which formats they use (for example, the 'debug' command supports a 'debug' format and an 'error' format).
    // The formats are specified under a 'formats' key, and can be either a <@link language Format Script Containers> or a direct format with the same syntax as format scripts.
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
    //         # Will print "[MyProject]: The system does not need a restart yet."
    //         - debug "The system does not need a restart yet."
    // </code>
    // -->

    public static final Set<Key> DEBUG_TYPES = new HashSet<>();

    public record Key(String key) {
        public Key(String key) {
            this.key = CoreUtilities.toLowerCase(key);
        }
    }

    public static Key registerFormatType(String name) {
        Key key = new Key(name);
        if (!DEBUG_TYPES.add(key)) {
            throw new IllegalArgumentException("Tried registering duplicate format type! format '" + name + "' already exists.");
        }
        return key;
    }

    public static ScriptLoggingContext parseFromConfiguration(ScriptContainer script) {
        YamlConfiguration formatsConfig = script.getConfigurationSection("formats");
        if (formatsConfig == null) {
            return null;
        }
        Map<Key, ParseableTag> formats = new HashMap<>();
        TagContext context = null;
        for (Key formatType : DEBUG_TYPES) {
            String rawFormat = formatsConfig.getString(formatType.key);
            if (rawFormat != null) {
                if (context == null) {
                    context = DenizenCore.implementation.getTagContext(script);
                }
                formats.put(formatType, TagManager.parseTextToTag(rawFormat, context));
                continue;
            }
            String formatScriptInput = formatsConfig.getString(formatType.key + "_script");
            if (formatScriptInput == null) {
                continue;
            }
            FormatScriptContainer formatScript = ScriptRegistry.getScriptContainerAs(formatScriptInput, FormatScriptContainer.class);
            if (formatScript == null || formatScript.getFormatTag() == null) {
                Debug.echoError(script, "Invalid format script '" + formatScriptInput + "' specified for debug format '" + formatType.key + "'.");
                continue;
            }
            formats.put(formatType, formatScript.getFormatTag());
        }
        if (formats.isEmpty()) {
            Debug.echoError(script, "Invalid logging config, must specify at least one valid debug format.");
            return null;
        }
        return new ScriptLoggingContext(formats, null);
    }

    public boolean hasFormat(Key debugType) {
        return singleFormat != null || formats.containsKey(debugType);
    }

    public String formatOrNull(Key debugType, String rawText, ScriptEntry entry) {
        ParseableTag formatTag = singleFormat == null ? formats.get(debugType) : singleFormat;
        return formatTag != null ? FormatScriptContainer.formatText(formatTag, rawText, entry.getContext(), entry.getScript()) : null;
    }

    public String format(Key debugType, String rawText, ScriptEntry entry) {
        String formatted = formatOrNull(debugType, rawText, entry);
        return formatted != null ? formatted : rawText;
    }
}
