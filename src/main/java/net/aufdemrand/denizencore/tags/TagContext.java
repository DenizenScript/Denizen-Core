package net.aufdemrand.denizencore.tags;

import net.aufdemrand.denizencore.scripts.ScriptEntry;

public class TagContext {
    public final boolean instant;
    public final boolean debug;
    public final ScriptEntry entry;
    public TagContext( boolean instant, boolean debug, ScriptEntry entry) {
        this.instant = instant;
        this.debug = debug;
        this.entry = entry;
    }
}

