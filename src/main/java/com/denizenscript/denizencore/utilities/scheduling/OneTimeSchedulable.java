package com.denizenscript.denizencore.utilities.scheduling;

public class OneTimeSchedulable extends Schedulable {

    public OneTimeSchedulable(Runnable runnable, float fireAfterSeconds) {
        run = runnable;
        secondsLeft = fireAfterSeconds;
    }

    @Override
    public boolean tick(float seconds) {
        if (cancelled) {
            return false;
        }
        secondsLeft -= seconds;
        if (secondsLeft <= 0) {
            run.run();
            return false;
        }
        return true;
    }
}
