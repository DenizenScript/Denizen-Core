package com.denizenscript.denizencore.exceptions;

public class TagProcessingException extends Exception {

    private static final long serialVersionUID = 3159108944857792098L;
    public String message;

    public TagProcessingException(String msg) {
        message = msg;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
