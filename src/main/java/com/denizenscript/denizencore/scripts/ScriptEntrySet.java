package com.denizenscript.denizencore.scripts;

import java.util.ArrayList;
import java.util.List;

public class ScriptEntrySet {

    public List<ScriptEntry> entries;

    public ScriptEntrySet(List<ScriptEntry> baseEntries) {
        entries = baseEntries;
    }

    public ScriptEntrySet duplicate() {
        List<ScriptEntry> newEntries = new ArrayList<>(entries.size());
        for (ScriptEntry entry : entries) {
            newEntries.add(entry.clone());
        }
        return new ScriptEntrySet(newEntries);
    }
}
