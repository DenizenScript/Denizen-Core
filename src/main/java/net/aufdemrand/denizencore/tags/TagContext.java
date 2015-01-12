package net.aufdemrand.denizencore.tags;

import net.aufdemrand.denizencore.objects.dScript;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;

public class TagContext { // TODO: make abstract?
    public final boolean instant;
    public final boolean debug;
    public final ScriptEntry entry;
    public final dScript script;

    /**
     * TODO: NEVER CALL DIRECTLY
     */
    public TagContext( boolean instant, boolean debug, ScriptEntry entry) {
        this.instant = instant;
        this.debug = debug;
        this.entry = entry;
        this.script = null;
    }

    /**
     * TODO: NEVER CALL DIRECTLY
     */
    public TagContext(boolean instant, boolean debug, ScriptEntry entry, dScript script) {
        this.instant = instant;
        this.debug = debug;
        this.entry = entry;
        this.script = script;
    }

    // TODO: IMPLEMENT IN BUKKIT

    /**
     * Must implement.
     */
    public ScriptEntryData getScriptEntryData() {
        return null;
    }
}

