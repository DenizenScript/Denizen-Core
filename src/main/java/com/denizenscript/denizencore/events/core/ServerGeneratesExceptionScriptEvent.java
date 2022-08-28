package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.objects.core.ScriptTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

public class ServerGeneratesExceptionScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // server generates exception
    //
    // @Group Core
    //
    // @Cancellable true
    //
    // @Warning Abusing this event can cause significant failures in the Denizen debug system. Use only with extreme caution.
    //
    // @Triggers when an exception occurs on the server.
    //
    // @Context
    // <context.message> returns the Exception message.
    // <context.full_trace> returns the full exception trace+message output details.
    // <context.type> returns the type of the error. (EG, NullPointerException).
    // <context.queue> returns the queue that caused the exception, if any.
    // <context.script> returns the script that caused the exception, if any.
    // <context.line> returns the line number within the script file that caused the exception, if any.
    // -->

    public static ServerGeneratesExceptionScriptEvent instance;

    public ServerGeneratesExceptionScriptEvent() {
        instance = this;
        registerCouldMatcher("server generates exception");
    }

    public Throwable exception;
    public ScriptQueue queue;
    public String fullTrace;
    public int line;
    public ScriptTag script;
    public static boolean cancelledTracker = false;

    @Override
    public ScriptEntryData getScriptEntryData() {
        if (queue != null && queue.getLastEntryExecuted() != null) {
            return queue.getLastEntryExecuted().entryData;
        }
        return super.getScriptEntryData();
    }

    @Override
    public ObjectTag getContext(String name) {
        switch (name) {
            case "message": return new ElementTag(exception.getMessage());
            case "full_trace": return new ElementTag(fullTrace);
            case "type": return new ElementTag(DebugInternals.getClassNameOpti(exception.getClass()));
            case "queue":
                if (queue != null) {
                    return new QueueTag(queue);
                }
                break;
            case "script": return script;
            case "line":
                if (line != -1) {
                    return new ElementTag(line);
                }
                break;
        }
        return super.getContext(name);
    }

    @Override
    public void cancellationChanged() {
        cancelledTracker = cancelled;
        super.cancellationChanged();
    }

    public boolean handle(Throwable ex, String trace, ScriptQueue queue, ScriptTag script, int line) {
        this.queue = queue;
        this.fullTrace = trace;
        this.exception = ex;
        this.line = line;
        this.script = script;
        cancelledTracker = false;
        fire();
        return cancelledTracker;
    }
}
