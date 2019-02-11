package net.aufdemrand.denizencore.tags;

import net.aufdemrand.denizencore.objects.dScript;
import net.aufdemrand.denizencore.scripts.ScriptEntry;
import net.aufdemrand.denizencore.scripts.ScriptEntryData;
import net.aufdemrand.denizencore.utilities.DefinitionProvider;
import net.aufdemrand.denizencore.utilities.SimpleDefinitionProvider;
import net.aufdemrand.denizencore.utilities.debugging.Debuggable;

public abstract class TagContext implements Debuggable {
    public boolean instant;
    public boolean debug;
    public final ScriptEntry entry;
    public final dScript script;
    public final DefinitionProvider definitionProvider;

    @Override
    public boolean shouldDebug() {
        return debug;
    }

    @Override
    public boolean shouldFilter(String criteria) {
        return false;
    }

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

