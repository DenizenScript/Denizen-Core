package net.aufdemrand.denizencore.scripts;

import net.aufdemrand.denizencore.utilities.debugging.dB;

import java.util.ArrayList;
import java.util.List;

public class ScriptEntrySet {

    public List<ScriptEntry> entries;

    public ScriptEntrySet(List<ScriptEntry> baseEntries) {
        entries = baseEntries;
    }

    public ScriptEntrySet duplicate() {
        List<ScriptEntry> newEntries = new ArrayList<ScriptEntry>(entries.size());
        try {
            for (ScriptEntry entry : entries) {
                newEntries.add(entry.clone());
            }
        }
        catch (CloneNotSupportedException e) {
            dB.echoError(e); // This should never happen
        }
        return new ScriptEntrySet(newEntries);
    }
}
