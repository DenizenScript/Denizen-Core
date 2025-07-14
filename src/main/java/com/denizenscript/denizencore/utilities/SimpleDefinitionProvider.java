package com.denizenscript.denizencore.utilities;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.MapTag;

public class SimpleDefinitionProvider implements DefinitionProvider {

    private final MapTag definitions;

    public SimpleDefinitionProvider(MapTag map) {
        this.definitions = map;
    }

    public SimpleDefinitionProvider() {
        this(new MapTag());
    }

    public SimpleDefinitionProvider(DefinitionProvider definitionProvider) {
        this(new MapTag(definitionProvider.getAllDefinitions().map));
    }

    @Override
    public void addDefinition(String definition, ObjectTag value) {
        definitions.putDeepObject(definition, value);
    }

    @Override
    public void addDefinition(String definition, String value) {
        addDefinition(definition, new ElementTag(value, true));
    }

    @Override
    public MapTag getAllDefinitions() {
        return this.definitions;
    }

    @Override
    public ObjectTag getDefinitionObject(String definition) {
        if (definition == null) {
            return null;
        }
        return definitions.getDeepObject(definition);
    }

    @Override
    public String getDefinition(String definition) {
        if (definition == null) {
            return null;
        }
        return CoreUtilities.stringifyNullPass(getDefinitionObject(definition));
    }

    @Override
    public boolean hasDefinition(String definition) {
        return getDefinitionObject(definition) != null;
    }

    @Override
    public void removeDefinition(String definition) {
        addDefinition(definition, (ObjectTag) null);
    }
}
