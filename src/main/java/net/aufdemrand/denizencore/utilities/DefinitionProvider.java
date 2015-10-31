package net.aufdemrand.denizencore.utilities;

import java.util.Map;

public interface DefinitionProvider {

    void addDefinition(String definition, String value);

    Map<String, String> getAllDefinitions();

    String getDefinition(String definition);

    boolean hasDefinition(String definition);

    void removeDefinition(String definition);
}
