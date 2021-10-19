package com.denizenscript.denizencore.objects.notable;

public interface Notable {

    boolean isUnique();

    Object getSaveObject();

    void makeUnique(String id);

    void forget();
}
