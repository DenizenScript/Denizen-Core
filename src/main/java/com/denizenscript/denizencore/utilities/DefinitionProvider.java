package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;

public interface DefinitionProvider {

    void addDefinition(String definition, String value);

    void addDefinition(String definition, ObjectTag value);

    MapTag getAllDefinitions();

    ObjectTag getDefinitionObject(String definition);

    String getDefinition(String definition);

    boolean hasDefinition(String definition);

    void removeDefinition(String definition);
}
