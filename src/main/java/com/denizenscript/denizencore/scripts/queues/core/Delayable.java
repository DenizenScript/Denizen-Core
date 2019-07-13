package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.objects.Duration;

public interface Delayable {

    public Delayable setPaused(boolean paused);

    public boolean isPaused();

    public void delayFor(Duration duration);

    public boolean isDelayed();

    public boolean isInstantSpeed();
}
