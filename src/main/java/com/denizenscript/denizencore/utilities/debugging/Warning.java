package com.denizenscript.denizencore.utilities.debugging;

import com.denizenscript.denizencore.scripts.ScriptEntry;
import com.denizenscript.denizencore.scripts.containers.ScriptContainer;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.tags.TagContext;

public class Warning {

    public String message;

    public Warning(String message) {
        this.message = message;
    }

    public boolean testShouldWarn() {
        return true;
    }

    public void warn(TagContext context) {
        warn(context == null ? null : context.entry);
    }

    public void warn(ScriptEntry entry) {
        warn(entry == null ? null : entry.getResidingQueue());
    }

    public void warn() {
        warn((ScriptQueue) null);
    }

    public void warn(ScriptQueue queue) {
        if (!testShouldWarn()) {
            return;
        }
        Debug.echoError(queue, message);
    }

    public void warn(ScriptContainer script) {
        if (!testShouldWarn()) {
            return;
        }
        Debug.echoError("[In Script: " + script.getName() + "] " + message);
    }
}
