package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.scheduling.RepeatingSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;
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

    /////////////////////
    // Private instance fields and constructors
    /////////////////////

    private Schedulable schedulable;

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
        this(id, DurationTag.valueOf(DenizenCore.getImplementation().scriptQueueSpeed(), CoreUtilities.basicContext));
    }

    public TimedQueue(String id, long ticks) {
        super(id);
        this.ticks = ticks;
    }

    public TimedQueue(String id, DurationTag timing) {
        super(id);
        ticks = timing.getTicks();
    }

    /////////////////////
    // Public instance setters and getters
    /////////////////////

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
        if (schedulable == null) {
            Schedulable schedulable = new RepeatingSchedulable(
                    new Runnable() {
                        @Override
                        public void run() {
                            revolve();
                        }
                    }, (ticks <= 0 ? 1 : ticks) / 20f);
            this.schedulable = schedulable;
            DenizenCore.schedule(schedulable);
        }
    }

    @Override
    public String getName() {
        return "TimedQueue";
    }

    @Override
    protected void onStop() {
        if (schedulable != null) {
            schedulable.cancel();
            schedulable = null;
        }
    }

    @Override
    public boolean shouldRevolve() {
        // Check if this Queue isn't paused
        if (paused) {
            return false;
        }

        // If it's delayed, schedule it for later
        return !isDelayed();
    }
}
