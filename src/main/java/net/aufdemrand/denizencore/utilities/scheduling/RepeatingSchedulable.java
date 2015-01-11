package net.aufdemrand.denizencore.utilities.scheduling;

public class RepeatingSchedulable extends Schedulable {

    public RepeatingSchedulable(Runnable runme, float fireRate) {
        run = runme;
        fireEverySeconds = fireRate;
        secondsLeft = fireRate;
    }

    public float fireEverySeconds = 0;
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
            secondsLeft += fireEverySeconds;
        }
        return true;
    }
}
