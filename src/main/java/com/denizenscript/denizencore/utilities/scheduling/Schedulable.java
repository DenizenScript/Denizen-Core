package com.denizenscript.denizencore.utilities.scheduling;

public abstract class Schedulable {

    public Runnable run;
    public boolean cancelled;
    public float secondsLeft;

    public void cancel() {
        cancelled = true;
    }

    public boolean isSync() {
        return true;
    }

    public abstract boolean tick(float seconds);
}
