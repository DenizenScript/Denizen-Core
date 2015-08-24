package net.aufdemrand.denizencore.utilities.scheduling;

import net.aufdemrand.denizencore.DenizenCore;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AsyncSchedulable extends Schedulable {

    public static final Executor executor = Executors.newCachedThreadPool();
    protected final Schedulable schedulable;

    public AsyncSchedulable(Schedulable schedulable) {
        this.schedulable = schedulable;
    }

    @Override
    public boolean isSync() {
        return false;
    }

    @Override
    public boolean tick(float seconds) {
        return this.schedulable.tick(seconds);
    }

    @Override
    protected void run() {
        if (DenizenCore.MAIN_THREAD == Thread.currentThread()) {
            executor.execute(run);
        }
        else {
            run.run();
        }
    }
}
