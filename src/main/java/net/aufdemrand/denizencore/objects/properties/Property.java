package net.aufdemrand.denizencore.objects.properties;

import net.aufdemrand.denizencore.objects.Mechanism;
import net.aufdemrand.denizencore.tags.Attribute;

public interface Property {

    public String getPropertyString();

    public String getPropertyId();

    public String getAttribute(Attribute attribute);

    public void adjust(Mechanism mechanism);
}
