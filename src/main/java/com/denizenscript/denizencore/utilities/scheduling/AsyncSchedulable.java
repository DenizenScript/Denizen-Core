package com.denizenscript.denizencore.utilities.scheduling;

import com.denizenscript.denizencore.DenizenCore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncSchedulable extends Schedulable {

    public static final Executor executor = Executors.newCachedThreadPool();
    protected final Schedulable schedulable;

    public AsyncSchedulable(Schedulable schedulable) {
        this.schedulable = schedulable;
        final Runnable runnable = schedulable.run;
        this.schedulable.run = new Runnable() {
            @Override
            public void run() {
                if (DenizenCore.MAIN_THREAD == Thread.currentThread()) {
                    executor.execute(runnable);
                }
                else {
                    runnable.run();
                }
            }
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
