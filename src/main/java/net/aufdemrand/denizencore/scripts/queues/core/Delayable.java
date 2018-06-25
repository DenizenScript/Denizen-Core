package net.aufdemrand.denizencore.scripts.queues.core;

import net.aufdemrand.denizencore.objects.Duration;

public interface Delayable {

    public Delayable setPaused(boolean paused);

    public boolean isPaused();

    public void delayFor(Duration duration);

    public boolean isDelayed();

    public boolean isInstantSpeed();
}
