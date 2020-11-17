package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class ScriptGeneratesErrorScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // script generates error
    //
    // @Regex ^on script generates error$
    //
    // @Group Core
    //
    // @Cancellable true
    //
    // @Warning Abusing this event can cause significant failures in the Denizen debug system. Use only with extreme caution.
    //
    // @Triggers when a script generates an error.
    //
    // @Context
    // <context.message> returns the error message.
    // <context.queue> returns the queue that caused the error, if any.
    // <context.script> returns the script that caused the error, if any.
    // <context.line> returns the line number within the script file that caused the error, if any.
    //
    // -->

    public static ScriptGeneratesErrorScriptEvent instance;

    public ScriptGeneratesErrorScriptEvent() {
        instance = this;
    }

    public ScriptQueue queue;
    public String message;
    public int line;
    public ScriptTag script;
    public static boolean cancelledTracker = false;

    @Override
    public ScriptEntryData getScriptEntryData() {
        if (queue != null && queue.getLastEntryExecuted() != null) {
            return queue.getLastEntryExecuted().entryData;
        }
        return DenizenCore.getImplementation().getEmptyScriptEntryData();
    }

    @Override
    public ObjectTag getContext(String name) {
        if (name.equals("message")) {
            return new ElementTag(message);
        }
        else if (name.equals("script")) {
            return script;
        }
        else if (name.equals("line") && line != -1) {
            return new ElementTag(line);
        }
        else if (name.equals("queue") && queue != null) {
            return new QueueTag(queue);
        }
        return super.getContext(name);
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("script generates error")) {
            return false;
        }
        return true;
    }

    @Override
    public void cancellationChanged() {
        cancelledTracker = cancelled;
        super.cancellationChanged();
    }

    @Override
    public String getName() {
        return "ScriptGeneratesError";
    }

    public boolean handle(String message, ScriptQueue queue, ScriptTag script, int line) {
        this.queue = queue;
        this.message = message;
        this.line = line;
        this.script = script;
        cancelledTracker = false;
        fire();
        return cancelledTracker;
    }
}
