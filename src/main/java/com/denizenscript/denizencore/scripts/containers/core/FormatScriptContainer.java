package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptLoggingContext;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.tags.ParseableTag;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.SimpleDefinitionProvider;
import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;

public class FormatScriptContainer extends ScriptContainer {

    // <--[language]
    // @name Format Script Containers
    // @group Script Container System
    // @description
    // Format script containers are very simple script containers used for formatting messages.
    //
    // <code>
    // Format_Script_Name:
    //
    //     type: format
    //
    //     # The only key is the format. The format can use '<[text]>' as a special def to contain the message being sent.
    //     # | All format scripts MUST have this key!
    //     format: [MyProject]: <[text]>
    // </code>
    //
    // -->

    public FormatScriptContainer(YamlConfiguration configurationSection, String scriptContainerName) {
        super(configurationSection, scriptContainerName);
        canRunScripts = false;
        this.formatTag = getCachedTag("format");
        if (this.formatTag == null) {
            Debug.echoError(this, "Invalid format script: must have a 'format' key.");
        }
    }

    final ParseableTag formatTag;
    ScriptLoggingContext asLoggingContext = null;

    public ParseableTag getFormatTag() {
        return formatTag;
    }

    public ScriptLoggingContext getAsLoggingContext() {
        if (asLoggingContext == null) {
            asLoggingContext = new ScriptLoggingContext(formatTag, formatTag);
        }
        return asLoggingContext;
    }

    public String getRawFormat() {
        return getString("format", "<[text]>");
    }

    public String getFormattedText(String text, ScriptEntry entry) {
        return getFormattedText(text, DenizenCore.implementation.getTagContext(entry));
    }

    public String getFormattedText(String textToReplace, TagContext context) {
        return formatText(formatTag, textToReplace, context, getAsScriptArg());
    }

    public static String formatText(ParseableTag formatTag, String rawText, TagContext context, ScriptTag source) {
        if (formatTag == null) {
            return rawText;
        }
        TagContext changedContext = context.clone();
        changedContext.script = source;
        changedContext.definitionProvider = new SimpleDefinitionProvider(changedContext.definitionProvider);
        changedContext.definitionProvider.addDefinition("text", new ElementTag(rawText, true));
        DenizenCore.implementation.addFormatScriptDefinitions(changedContext.definitionProvider, changedContext);
        return formatTag.parse(changedContext).identify();
    }
}
