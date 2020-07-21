package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;

import java.util.HashMap;
import java.util.Map;

public class SimpleDefinitionProvider implements DefinitionProvider {

    private final Map<String, ObjectTag> definitions = new HashMap<>();

    @Override
    public void addDefinition(String definition, ObjectTag value) {
        this.definitions.put(CoreUtilities.toLowerCase(definition), value);
    }

    @Override
    public void addDefinition(String definition, String value) {
        this.addDefinition(definition, new ElementTag(value));
    }

    @Override
    public Map<String, ObjectTag> getAllDefinitions() {
        return this.definitions;
    }

    @Override
    public ObjectTag getDefinitionObject(String definition) {
        if (definition == null) {
            return null;
        }
        return definitions.get(CoreUtilities.toLowerCase(definition));
    }

    @Override
    public String getDefinition(String definition) {
        if (definition == null) {
            return null;
        }
        return CoreUtilities.stringifyNullPass(definitions.get(CoreUtilities.toLowerCase(definition)));
    }

    @Override
    public boolean hasDefinition(String definition) {
        return definitions.containsKey(CoreUtilities.toLowerCase(definition));
    }

    @Override
    public void removeDefinition(String definition) {
        definitions.remove(CoreUtilities.toLowerCase(definition));
    }
}
