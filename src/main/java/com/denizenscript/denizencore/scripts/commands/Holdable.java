package com.denizenscript.denizencore.scripts.commands;

/**
 * Simply used to indicate that a command can be 'held', so we don't wait for
 * commands that will never mark themselves 'finished'.
 */
public interface Holdable {

    // <--[language]
    // @name ~Waitable
    // @group Script Command System
    // @description
    // A command that is "~Waitable" (or "Holdable", or that can be "~waited for") is a command that:
    // - Might potentially take a while to execute
    // - Is able to perform a slowed execution (that doesn't freeze the server)
    // - And so supports the "~" prefix.
    //
    // This is written, for example, like: - ~run MySlowScript
    //
    // When a command is ~waited for, the queue it's in will wait for it to complete, but the rest of the server will continue running.
    // This is of course similar to the "wait" command, but waits for the action to complete instead of simply for a period of time.
    //
    // Some commands, particularly those related to file operation, when ~waited for will move the file operation off-thread.
    // Others may need to be on the server thread, and may split the operation into smaller segments spread out over 1 tick each or similar logic.
    // Some of these commands, when NOT waited for, will freeze the server thread until the operation completes.
    // Others, however, may still perform the action in a delayed/slow/off-thread manner, but simply not hold the queue.
    // -->
}
