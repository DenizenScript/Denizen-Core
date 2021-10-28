package com.denizenscript.denizencore.exceptions;

public class InvalidArgumentsRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 3159108944857792068L;
    public String message;

    public InvalidArgumentsRuntimeException(String msg) {
        message = msg;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
