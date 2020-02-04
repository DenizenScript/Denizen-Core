package com.denizenscript.denizencore.exceptions;

public class InvalidArgumentsException extends Exception {

    private static final long serialVersionUID = 3159108944857792068L;
    public String message;

    public InvalidArgumentsException(String msg) {
        message = msg;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
