package com.denizenscript.denizencore.utilities;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotates that a field/method/class should not be reflected into with Denizen scripting reflection tools like {@link com.denizenscript.denizencore.objects.core.JavaReflectedObjectTag}
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ReflectionRefuse {
}
