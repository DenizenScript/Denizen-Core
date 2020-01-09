package com.denizenscript.denizencore.scripts.queues.core;

import com.denizenscript.denizencore.utilities.scheduling.AsyncSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.RepeatingSchedulable;
import com.denizenscript.denizencore.utilities.scheduling.Schedulable;
import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.core.DurationTag;
import com.denizenscript.denizencore.scripts.queues.ScriptQueue;

public class TimedQueue extends ScriptQueue implements Delayable {

    /**
     * Gets a TimedQueue instance.
     * <p/>
     * If a queue already exists with the given id, it will return that instance,
     * which may be currently running, unless the type of queue is not a TimedQueue.
     * If a queue does not exist, a new stopped queue is created instead.
     * <p/>
     * IDs are case insensitive.  If having an easy-to-recall ID is not necessary, just
     * pass along null as the id, and it will use ScriptQueue's static method _getNextId()
     * which will return a random UUID.
     * <p/>
     * The speed node will be automatically read from the configuration,
     * and new ScriptQueues may need further information before they
     * can start(), including entries, delays, loops, and possibly context.
     *
     * @param id unique id of the queue
     * @return a TimedQueue
     */
    public static TimedQueue getQueue(String id) {
        // Get id if not specified.
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null!");
        }
        TimedQueue scriptQueue;
        // Does the queue already exist? Get it if it does.
        if (queueExists(id)) {
            scriptQueue = (TimedQueue) ScriptQueue.allQueues.get(id);
        }
        // If not, create a new one.
        else {
            scriptQueue = new TimedQueue(id,
                    DurationTag.valueOf(DenizenCore.getImplementation().scriptQueueSpeed()));
        }
        // Return the queue
        return scriptQueue;
    }

    /////////////////////
    // Private instance fields and constructors
    /////////////////////

    private Schedulable schedulable;

    // The speed of the engine, the # of ticks
    // between each revolution. Use setSpeed()
    // to change this.
    private long ticks;

    // ScriptQueues can be paused mid-rotation.
    // The next entry will be held up until
    // un-paused.
    protected boolean paused = false;

    // The delay in ticks can put off the
    // start of a queue
    protected long delay_ticks = 0;

    @Override
    public void delayFor(DurationTag duration) {
        delay_ticks = DenizenCore.serverTimeMillis + duration.getMillis();
    }

    @Override
    public boolean isDelayed() {
        return (delay_ticks > DenizenCore.serverTimeMillis);
    }

    public TimedQueue(String id) {
        this(id, DurationTag.valueOf(DenizenCore.getImplementation().scriptQueueSpeed()));
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

    @Override
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
    @Override
    public Delayable setPaused(boolean paused) {
        this.paused = paused;
        return this;
    }

    /**
     * Checks if the queue is currently paused.
     *
     * @return true if paused.
     */
    @Override
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
    protected void onStart() {
        revolve();
        if (script_entries.isEmpty()) {
            return;
        }
        Schedulable schedulable = new RepeatingSchedulable(
                new Runnable() {
                    @Override
                    public void run() {
                        revolve();
                    }
                }, (ticks <= 0 ? 1 : ticks) / 20f);
        if (run_async) {
            schedulable = new AsyncSchedulable(schedulable);
        }
        this.schedulable = schedulable;
        DenizenCore.schedule(schedulable);
    }

    @Override
    public String getName() {
        return "TimedQueue";
    }

    @Override
    protected void onStop() {
        if (schedulable != null) {
            schedulable.cancel();
        }
    }

    @Override
    protected boolean shouldRevolve() {
        // Check if this Queue isn't paused
        if (paused) {
            return false;
        }

        // If it's delayed, schedule it for later
        return !isDelayed();
    }
}
