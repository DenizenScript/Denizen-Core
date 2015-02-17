package net.aufdemrand.denizencore.scripts;

import net.aufdemrand.denizencore.tags.TagContext;
import net.aufdemrand.denizencore.utilities.debugging.dB;

public abstract class ScriptEntryData implements Cloneable {
    // TODO: private ScriptEntry entry; ?

    @Override
    public ScriptEntryData clone() {
        try {
            return (ScriptEntryData)super.clone();
        }
        catch (Exception e) {
            dB.echoError(e);
            return null;
        }
    }

    public abstract void transferDataFrom(ScriptEntryData data);

    public abstract TagContext getTagContext();

    @Override
    public String toString() {
        return "{{ Unimplemented toString method in ScriptEntryData! }}";
    }
}
