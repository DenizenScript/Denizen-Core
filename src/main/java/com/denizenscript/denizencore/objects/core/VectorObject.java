package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.ObjectTag;

/**
 * Represents an object that contains X/Y/Z 3D Vector.
 */
public interface VectorObject extends ObjectTag {

    // <--[ObjectType]
    // @name VectorObject
    // @ExampleTagBase location[1,2,3]
    // @ExampleValues 1,2,3
    // @ExampleForReturns
    // - adjust <player> velocity:%VALUE%
    // @prefix None
    // @base None
    // @format
    // N/A
    //
    // @description
    // "VectorObject" is a pseudo-ObjectType that represents any object that indicates a 3D vector, such as a LocationTag.
    //
    // -->

    double getX();

    double getY();

    double getZ();

    default double lengthSquared() {
        double x = getX(), y = getY(), z = getZ();
        return x * x + y * y + z * z;
    }

    default double length() {
        return Math.sqrt(lengthSquared());
    }
}
