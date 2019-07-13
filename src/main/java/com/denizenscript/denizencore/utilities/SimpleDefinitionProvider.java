package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.objects.Element;
import com.denizenscript.denizencore.objects.dObject;

import java.util.HashMap;
import java.util.Map;

public class SimpleDefinitionProvider implements DefinitionProvider {

    private final Map<String, dObject> definitions = new HashMap<>();

    @Override
    public void addDefinition(String definition, String value) {
        this.definitions.put(CoreUtilities.toLowerCase(definition), new Element(value));
    }

    @Override
    public Map<String, dObject> getAllDefinitions() {
        return this.definitions;
    }

    @Override
    public dObject getDefinitionObject(String definition) {
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
