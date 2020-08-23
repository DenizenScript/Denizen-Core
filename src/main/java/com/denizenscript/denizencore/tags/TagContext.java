package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.queues.ContextSource;
import com.denizenscript.denizencore.utilities.DefinitionProvider;
import com.denizenscript.denizencore.utilities.SimpleDefinitionProvider;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.utilities.debugging.Debuggable;

public abstract class TagContext implements Debuggable, Cloneable {
    public boolean debug;
    public boolean showErrors;
    public ScriptEntry entry;
    public ScriptTag script;
    public DefinitionProvider definitionProvider;
    public ContextSource contextSource;

    @Override
    public TagContext clone() {
        try {
            return (TagContext) super.clone();
        }
        catch (CloneNotSupportedException ex) {
            Debug.echoError(ex);
            return null;
        }
    }

    public boolean showErrors() {
        return debug || showErrors;
    }

    @Override
    public boolean shouldDebug() {
        return debug;
    }

    public TagContext(boolean debug, ScriptEntry entry, ScriptTag script) {
        this(debug, entry, script, null);
    }

    public TagContext(boolean debug, ScriptEntry entry, ScriptTag script, DefinitionProvider definitionProvider) {
        this.debug = debug;
        this.entry = entry;
        this.script = script;
        this.definitionProvider = definitionProvider != null ? definitionProvider :
                (entry != null ? entry.getResidingQueue() : new SimpleDefinitionProvider());
        if (entry != null && entry.getResidingQueue() != null) {
            this.contextSource = entry.getResidingQueue().contextSource;
        }
    }

    public abstract ScriptEntryData getScriptEntryData();
}

