package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.SimpleDefinitionProvider;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;

public abstract class TagContext implements Debuggable {
    public boolean instant;
    public boolean debug;
    public final ScriptEntry entry;
    public final ScriptTag script;
    public final DefinitionProvider definitionProvider;

    @Override
    public boolean shouldDebug() {
        return debug;
    }

    @Override
    public boolean shouldFilter(String criteria) {
        return false;
    }

    public TagContext(boolean instant, boolean debug, ScriptEntry entry, ScriptTag script) {
        this(instant, debug, entry, script, null);
    }

    public TagContext(boolean instant, boolean debug, ScriptEntry entry, ScriptTag script, DefinitionProvider definitionProvider) {
        this.instant = instant;
        this.debug = debug;
        this.entry = entry;
        this.script = script;
        this.definitionProvider = definitionProvider != null ? definitionProvider :
                (entry != null ? entry.getResidingQueue() : new SimpleDefinitionProvider());
    }

    public abstract ScriptEntryData getScriptEntryData();
}

