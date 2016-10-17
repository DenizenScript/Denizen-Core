package net.aufdemrand.denizencore.utilities;

import java.util.HashMap;
import java.util.Map;

public class SimpleDefinitionProvider implements DefinitionProvider {

    private final Map<String, String> definitions = new HashMap<String, String>();

    @Override
    public void addDefinition(String definition, String value) {
        this.definitions.put(CoreUtilities.toLowerCase(definition), value);
    }

    @Override
    public Map<String, String> getAllDefinitions() {
        return this.definitions;
    }

    @Override
    public String getDefinition(String definition) {
        if (definition == null) {
            return null;
        }
        return this.definitions.get(CoreUtilities.toLowerCase(definition));
    }

    @Override
    public boolean hasDefinition(String definition) {
        return this.definitions.containsKey(CoreUtilities.toLowerCase(definition));
    }

    @Override
    public void removeDefinition(String definition) {
        this.definitions.remove(CoreUtilities.toLowerCase(definition));
    }
}
