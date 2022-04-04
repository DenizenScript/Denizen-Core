package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.scripts.queues.ScriptEngine;
import com.denizenscript.denizencore.utilities.CoreConfiguration;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class TimedQueue extends ScriptQueue {

    @FunctionalInterface
    public interface DelayTracker {

        boolean isDelayed();
    }

    public static class DeltaTimeDelayTracker implements DelayTracker {

        public long serverTimeEnd;

        public DeltaTimeDelayTracker(long millis) {
            serverTimeEnd = DenizenCore.serverTimeMillis + millis;
        }

        @Override
        public boolean isDelayed() {
            return serverTimeEnd > DenizenCore.serverTimeMillis;
        }
    }

    private long ticks;

    protected boolean paused = false;

    public DelayTracker delay;

    public void delayFor(DurationTag duration) {
        delay = new DeltaTimeDelayTracker(duration.getMillis());
    }

    public boolean isDelayed() {
        return delay != null && delay.isDelayed();
    }

    public TimedQueue(String id) {
        this(id, new DurationTag(CoreConfiguration.scriptQueueSpeed));
    }

    public TimedQueue(String id, long ticks) {
        super(id);
        this.ticks = ticks;
    }

    public TimedQueue(String id, DurationTag timing) {
        super(id);
        ticks = timing.getTicks();
    }

    public boolean isInstantSpeed() {
        return ticks <= 0;
    }

    /**
     * Gets the speed of the queue. This is the
     * time in between each revolution.
     *
     * @return a DurationTag of the speed.
     */
    public DurationTag getSpeed() {
        return new DurationTag(ticks);
    }

    /**
     * Pauses the queue. Paused queues will check
     * to be re-resumed every 'rotation', defined
     * by the speed of the queue.
     *
     * @param paused whether the queue should be paused
     */
    public TimedQueue setPaused(boolean paused) {
        this.paused = paused;
        return this;
    }

    /**
     * Checks if the queue is currently paused.
     *
     * @return true if paused.
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * Sets the speed of a queue. Uses bukkit's 'ticks', which is
     * 20 ticks per second.
     *
     * @param ticks the number of ticks between each rotation.
     */
    public TimedQueue setSpeed(long ticks) {
        this.ticks = ticks;
        return this;
    }

    @Override
    public void onStart() {
        revolve();
        if (script_entries.isEmpty()) {
            return;
        }
        DenizenCore.timedQueues.add(this);
    }

    /**
     * Tick counter for 'tryRevolveOnce'.
     */
    public int tickCounter = 0;

    /**
     * Tries to revolve the timedqueue, incrementing the tick counter and only revolving if it's time to.
     */
    public final void tryRevolveOnce() {
        if (tickCounter++ >= ticks) {
            tickCounter = 0;
            revolve();
        }
    }

    public void revolve() {
        if (script_entries.isEmpty()) {
            if (!waitWhenEmpty) {
                stop();
            }
            return;
        }
        if (paused || isDelayed()) {
            return;
        }
        ScriptEngine.revolve(this);
        if (script_entries.isEmpty() && !waitWhenEmpty) {
            stop();
        }
    }

    @Override
    public String getName() {
        return "TimedQueue";
    }
}
