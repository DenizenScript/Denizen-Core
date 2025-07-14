package com.denizenscript.denizencore.scripts.containers.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagManager;
import com.denizenscript.denizencore.utilities.SimpleDefinitionProvider;
import com.denizenscript.denizencore.utilities.YamlConfiguration;

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
    }

    public String getFormat() {
        return getString("format", "<[text]>");
    }

    public String getFormattedText(String text, ScriptEntry entry) {
        return getFormattedText(text, DenizenCore.implementation.getTagContext(entry));
    }

    public String getFormattedText(String textToReplace, TagContext context) {
        TagContext changedContext = context.clone();
        changedContext.script = new ScriptTag(this);
        changedContext.definitionProvider = new SimpleDefinitionProvider(changedContext.definitionProvider);
        context.definitionProvider.addDefinition("text", textToReplace);
        DenizenCore.implementation.addFormatScriptDefinitions(changedContext.definitionProvider, changedContext);
        return TagManager.tag(getFormat(), context);
    }
}
