package com.denizenscript.denizencore.utilities.scheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncSchedulable extends Schedulable {

    public static final Executor executor = Executors.newCachedThreadPool();
    protected final Schedulable schedulable;

    public AsyncSchedulable(Schedulable schedulable) {
        this.schedulable = schedulable;
        final Runnable runnable = schedulable.run;
        this.schedulable.run = () -> {
            executor.execute(runnable);
        };
    }

    @Override
    public boolean isSync() {
        return false;
    }

    @Override
    public boolean tick(float seconds) {
        return this.schedulable.tick(seconds);
    }
}
