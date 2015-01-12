package net.aufdemrand.denizencore.tags;

import net.aufdemrand.denizencore.objects.dScript;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;

public abstract class TagContext {
    public final boolean instant;
    public final boolean debug;
    public final ScriptEntry entry;
    public final dScript script;

    public TagContext(boolean instant, boolean debug, ScriptEntry entry, dScript script) {
        this.instant = instant;
        this.debug = debug;
        this.entry = entry;
        this.script = script;
    }

    public abstract ScriptEntryData getScriptEntryData();
}

