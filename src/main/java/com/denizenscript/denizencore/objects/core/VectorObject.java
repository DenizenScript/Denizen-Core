package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.utilities.CoreUtilities;

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

    void setX(double x);

    void setY(double y);

    void setZ(double z);

    default double lengthSquared() {
        double x = getX(), y = getY(), z = getZ();
        return x * x + y * y + z * z;
    }

    default double length() {
        return Math.sqrt(lengthSquared());
    }

    static <T extends VectorObject> void register(Class<T> type,  ObjectTagProcessor<T> processor) {

        // <--[tag]
        // @attribute <VectorObject.x>
        // @returns ElementTag(Decimal)
        // @group identity
        // @description
        // Returns the X coordinate of this object.
        // -->
        processor.registerTag(ElementTag.class, "x", (attribute, object) -> {
            return new ElementTag(object.getX());
        });

        // <--[tag]
        // @attribute <VectorObject.y>
        // @returns ElementTag(Decimal)
        // @group identity
        // @description
        // Returns the Y coordinate of this object.
        // -->
        processor.registerTag(ElementTag.class, "y", (attribute, object) -> {
            return new ElementTag(object.getY());
        });

        // <--[tag]
        // @attribute <VectorObject.z>
        // @returns ElementTag(Decimal)
        // @group identity
        // @description
        // Returns the Z coordinate of this object.
        // -->
        processor.registerTag(ElementTag.class, "z", (attribute, object) -> {
            return new ElementTag(object.getZ());
        });

        // <--[tag]
        // @attribute <VectorObject.xyz>
        // @returns ElementTag
        // @group identity
        // @description
        // Returns the basic vector in "x,y,z" format.
        // For example: 1,2,3
        // Other values, such as world, yaw, and pitch will be excluded from this output.
        // -->
        processor.registerTag(ElementTag.class, "xyz", (attribute, object) -> {
            return new ElementTag(CoreUtilities.doubleToString(object.getX()) + "," + CoreUtilities.doubleToString(object.getY()) + "," + CoreUtilities.doubleToString(object.getZ()));
        });

        // <--[tag]
        // @attribute <VectorObject.with_x[<number>]>
        // @returns VectorObject
        // @group identity
        // @description
        // Returns a copy of this object with a changed X value.
        // -->
        processor.registerTag(type, "with_x", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            T output = (T) object.duplicate();
            output.setX(attribute.getDoubleParam());
            return output;
        });

        // <--[tag]
        // @attribute <VectorObject.with_y[<number>]>
        // @returns VectorObject
        // @group identity
        // @description
        // Returns a copy of this object with a changed Y value.
        // -->
        processor.registerTag(type, "with_y", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            T output = (T) object.duplicate();
            output.setY(attribute.getDoubleParam());
            return output;
        });

        // <--[tag]
        // @attribute <VectorObject.with_z[<number>]>
        // @returns VectorObject
        // @group identity
        // @description
        // Returns a copy of this object with a changed Z value.
        // -->
        processor.registerTag(type, "with_z", (attribute, object) -> {
            if (!attribute.hasParam()) {
                return null;
            }
            T output = (T) object.duplicate();
            output.setZ(attribute.getDoubleParam());
            return output;
        });

        // <--[tag]
        // @attribute <VectorObject.add[<vector/location/x,y,z>]>
        // @returns VectorObject
        // @group math
        // @description
        // Returns a copy of this object with the specified coordinates added to it.
        // -->
        processor.registerTag(type, type, "add", (attribute, object, input) -> { // TODO: ability to specify VectorObject as the input
            T other = (T) object.duplicate();
            other.setX(object.getX() + input.getX());
            other.setY(object.getY() + input.getY());
            other.setZ(object.getZ() + input.getZ());
            return other;
        });

        // <--[tag]
        // @attribute <VectorObject.sub[<vector/location/x,y,z>]>
        // @returns VectorObject
        // @group math
        // @description
        // Returns a copy of this object with the specified coordinates subtracted from it.
        // -->
        processor.registerTag(type, type, "sub", (attribute, object, input) -> {
            T other = (T) object.duplicate();
            other.setX(object.getX() - input.getX());
            other.setY(object.getY() - input.getY());
            other.setZ(object.getZ() - input.getZ());
            return other;
        });

        // <--[tag]
        // @attribute <VectorObject.mul[<length>]>
        // @returns VectorObject
        // @group math
        // @description
        // Returns a copy of this object multiplied by the specified length.
        // -->
        processor.registerTag(type, ElementTag.class, "mul", (attribute, object, length) -> {
            T other = (T) object.duplicate();
            double len = length.asDouble();
            other.setX(object.getX() * len);
            other.setY(object.getY() * len);
            other.setZ(object.getZ() * len);
            return other;
        });

        // <--[tag]
        // @attribute <VectorObject.div[<length>]>
        // @returns VectorObject
        // @group math
        // @description
        // Returns a copy of this object divided by the specified length.
        // -->
        processor.registerTag(type, ElementTag.class, "div", (attribute, object, length) -> {
            T other = (T) object.duplicate();
            double len = 1.0 / length.asDouble();
            other.setX(object.getX() * len);
            other.setY(object.getY() * len);
            other.setZ(object.getZ() * len);
            return other;
        });

        // <--[tag]
        // @attribute <VectorObject.normalize>
        // @returns VectorObject
        // @group math
        // @description
        // Returns a 1-length vector in the same direction as this vector.
        // -->
        processor.registerTag(type, "normalize", (attribute, object) -> {
            double len = object.length();
            if (len == 0) {
                len = 1;
            }
            len = 1.0 / len;
            T other = (T) object.duplicate();
            other.setX(object.getX() * len);
            other.setY(object.getY() * len);
            other.setZ(object.getZ() * len);
            return other;
        });

        // <--[tag]
        // @attribute <VectorObject.vector_length_squared>
        // @returns ElementTag(Decimal)
        // @synonyms VectorObject.magnitude
        // @group VectorObject
        // @description
        // Returns the square of the 3D length of the vector.
        // -->
        processor.registerTag(ElementTag.class, "vector_length_squared", (attribute, object) -> {
            return new ElementTag(object.lengthSquared());
        });

        // <--[tag]
        // @attribute <VectorObject.vector_length>
        // @returns ElementTag(Decimal)
        // @synonyms VectorObject.magnitude
        // @group VectorObject
        // @description
        // Returns the 3D length of the vector.
        // -->
        processor.registerTag(ElementTag.class, "vector_length", (attribute, object) -> {
            return new ElementTag(object.length());
        });
    }
}
