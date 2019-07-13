package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.objects.dObject;

import java.util.Map;

public interface DefinitionProvider {

    void addDefinition(String definition, String value);

    Map<String, dObject> getAllDefinitions();

    dObject getDefinitionObject(String definition);

    String getDefinition(String definition);

    boolean hasDefinition(String definition);

    void removeDefinition(String definition);
}
