package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.objects.core.DurationTag;

public interface Delayable {

    public Delayable setPaused(boolean paused);

    public boolean isPaused();

    public void delayFor(DurationTag duration);

    public boolean isDelayed();

    public boolean isInstantSpeed();
}
