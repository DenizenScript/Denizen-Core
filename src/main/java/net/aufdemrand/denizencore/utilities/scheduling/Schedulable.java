package net.aufdemrand.denizencore.utilities.scheduling;

public abstract class Schedulable {

    protected Runnable run;
    protected boolean cancelled;
    protected float secondsLeft;

    public void cancel() {
        cancelled = true;
    }

    public boolean isSync() {
        return true;
    }

    public abstract boolean tick(float seconds);

    protected abstract void run();
}
