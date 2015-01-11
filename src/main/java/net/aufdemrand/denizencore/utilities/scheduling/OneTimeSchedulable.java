package net.aufdemrand.denizencore.utilities.scheduling;

public class OneTimeSchedulable extends Schedulable {

    public OneTimeSchedulable(Runnable runme, float fireTime) {
        run = runme;
        secondsLeft = fireTime;
    }

    public float secondsLeft = 0;

    public Runnable run;

    public boolean cancelled = false;

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
