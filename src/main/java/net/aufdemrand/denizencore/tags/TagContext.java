package net.aufdemrand.denizencore.tags;

import net.aufdemrand.denizencore.objects.dScript;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.utilities.DefinitionProvider;
import net.aufdemrand.denizencore.utilities.SimpleDefinitionProvider;

public abstract class TagContext {
    public final boolean instant;
    public final boolean debug;
    public final ScriptEntry entry;
    public final dScript script;
    public final DefinitionProvider definitionProvider;

    public TagContext(boolean instant, boolean debug, ScriptEntry entry, dScript script) {
        this(instant, debug, entry, script, null);
    }

    public TagContext(boolean instant, boolean debug, ScriptEntry entry, dScript script, DefinitionProvider definitionProvider) {
        this.instant = instant;
        this.debug = debug;
        this.entry = entry;
        this.script = script;
        this.definitionProvider = definitionProvider != null ? definitionProvider :
                entry != null ? entry.getResidingQueue() : new SimpleDefinitionProvider();
    }

    public abstract ScriptEntryData getScriptEntryData();
}

