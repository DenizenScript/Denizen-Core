package net.aufdemrand.denizencore.interfaces;

/**
 * Container interface for instances loaded into a dRegistry.
 *
 * @author Jeremy Schroeder
 */

public interface RegistrationableInstance {

    public RegistrationableInstance activate();

    public RegistrationableInstance as(String name);

    public String getName();

    public void onEnable();

    public void onDisable();
}
