package com.denizenscript.denizencore.exceptions;

public class Unreachable extends RuntimeException {

    private static final long serialVersionUID = 3159108944857792068L;

    /**
     * Indicates that a code section is known to be unreachable at runtime, but the compiler does not know this.
     */
    public Unreachable() {
    }

    @Override
    public String getMessage() {
        return "This code is unreachable.";
    }
}
