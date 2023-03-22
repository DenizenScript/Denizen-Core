package com.denizenscript.denizencore.objects.properties;

import com.denizenscript.denizencore.objects.ObjectTag;

public abstract class ObjectProperty<T extends ObjectTag> implements Property {

    public T object;
}
