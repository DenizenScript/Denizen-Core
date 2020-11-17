package com.denizenscript.denizencore.events.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.events.ScriptEvent;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.QueueTag;
import com.denizenscript.denizencore.scripts.ScriptEntryData;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class ServerGeneratesExceptionScriptEvent extends ScriptEvent {

    // <--[event]
    // @Events
    // server generates exception
    //
    // @Regex ^on server generates exception$
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
    // -->

    public static ServerGeneratesExceptionScriptEvent instance;

    public ServerGeneratesExceptionScriptEvent() {
        instance = this;
    }

    public Throwable exception;
    public ScriptQueue queue;
    public String fullTrace;
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
            return new ElementTag(exception.getMessage());
        }
        else if (name.equals("full_trace")) {
            return new ElementTag(fullTrace);
        }
        else if (name.equals("type")) {
            return new ElementTag(exception.getClass().getSimpleName());
        }
        else if (name.equals("queue") && queue != null) {
            return new QueueTag(queue);
        }
        return super.getContext(name);
    }

    @Override
    public boolean couldMatch(ScriptPath path) {
        if (!path.eventLower.startsWith("server generates exception")) {
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
        return "ServerGeneratesException";
    }

    public boolean handle(Throwable ex, String trace, ScriptQueue queue) {
        this.queue = queue;
        this.fullTrace = trace;
        this.exception = ex;
        cancelledTracker = false;
        fire();
        return cancelledTracker;
    }
}
