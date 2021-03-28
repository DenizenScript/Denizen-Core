package com.denizenscript.denizencore.scripts;

import com.denizenscript.denizencore.utilities.YamlConfiguration;
import com.denizenscript.denizencore.utilities.debugging.Debug;
import com.denizenscript.denizencore.tags.TagContext;

public abstract class ScriptEntryData implements Cloneable {

    public ScriptEntry scriptEntry;

    @Override
    public ScriptEntryData clone() {
        try {
            return (ScriptEntryData) super.clone();
        }
        catch (Exception e) {
            Debug.echoError(e);
            return null;
        }
    }

    public abstract void transferDataFrom(ScriptEntryData data);

    public abstract TagContext getTagContext();

    @Override
    public String toString() {
        return "{{ Unimplemented toString method in ScriptEntryData! }}";
    }

    public abstract YamlConfiguration save();

    public abstract void load(YamlConfiguration config);
}
