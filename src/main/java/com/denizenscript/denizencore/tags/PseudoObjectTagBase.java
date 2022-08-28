package com.denizenscript.denizencore.tags;

import com.denizenscript.denizencore.objects.ObjectFetcher;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.ObjectType;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.utilities.debugging.DebugInternals;

public abstract class PseudoObjectTagBase<T extends PseudoObjectTagBase> implements ObjectTag {

    @Override
    public String getPrefix() {
        return identify();
    }

    @Override
    public boolean isUnique() {
        return true;
    }

    @Override
    public String identify() {
        return "(Base-Object)";
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        return this;
    }

    @Override
    public ObjectTag getNextObjectTypeDown() {
        return new ElementTag.FailedObjectTag();
    }

    public ObjectTagProcessor<T> tagProcessor = new ObjectTagProcessor<>();

    public ObjectType<T> type = new ObjectType<>();

    public abstract void registerTags();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute((T) this, attribute);
    }

    public PseudoObjectTagBase() {
        tagProcessor.type = (Class<T>) getClass();
        type.tagProcessor = tagProcessor;
        type.clazz = tagProcessor.type;
        type.longName = DebugInternals.getClassNameOpti(getClass());
        ObjectFetcher.objectsByClass.put(type.clazz, type);
        registerTags();
    }
}
