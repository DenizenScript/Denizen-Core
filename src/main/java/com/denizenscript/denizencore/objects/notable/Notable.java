package com.denizenscript.denizencore.objects.notable;

public interface Notable {

    boolean isUnique();

    /**
     * Gets the object to be saved to the notables.yml.
     * This should either be a String, or a ConfigurationSerializable object.
     *
     * @return the object to be saved
     */
    Object getSaveObject();

    /**
     * Saves the object in the NotableManager. Notable objects are saved through
     * a server restart.
     *
     * @param id the id of the notable
     */
    void makeUnique(String id);

    void forget();
}
