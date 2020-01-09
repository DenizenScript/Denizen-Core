package com.denizenscript.denizencore.events;

import java.util.Set;

/**
 * To make the world event system as easy as possible, firing an event is as easy as calling the
 * EventHandler's doEvents(...) from anywhere in your code that's appropriate. However, should
 * you choose to implement SmartEvent, you'll make your event more efficient and manageable.
 * <p/>
 * SmartEvents must be registered with the EventManager
 */
public interface OldSmartEvent {

    /**
     * Determines by the event names provided if this event handler should initialize.
     * Typically regex is used to determine a valid pattern. Called when scripts reload.
     *
     * @param events A list of events to test
     * @return Whether they should initialize
     */
    boolean shouldInitialize(Set<String> events);

    /**
     * Called if shouldInitialize() returns true. This method handles any code that is
     * required to make this event, or set of events, function. No code should reach the
     * doEvents(...) call unless this method is invoked.
     */
    void _initialize();

    /**
     * Called as scripts are reloaded. Since there's the possibility that events may change
     * on a reload, it should be assumed that this SmartEvent may no longer be used. Instead,
     * it should breakDown, and re-initialize via shouldInitialize(), and if  that returns true,
     * intitialize().
     */
    void breakDown();
}
