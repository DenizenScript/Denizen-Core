package com.denizenscript.denizencore.objects.core;

import com.denizenscript.denizencore.DenizenCore;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import com.denizenscript.denizencore.utilities.debugging.Debug;

import java.util.List;

public class QuaternionTag implements ObjectTag {

    // <--[ObjectType]
    // @name QuaternionTag
    // @prefix quaternion
    // @base ElementTag
    // @ExampleTagBase quaternion[identity]
    // @ExampleValues identity
    // @ExampleForReturns
    // - narrate "TODO: Placeholder: quaternion is %VALUE%"
    // @format
    // The identity format for quaternions is <x>,<y>,<z>,<w>
    // For example "0,0,0,1" is the Identity quaternion.
    // You can input the word "identity" to get an identity quaternion as well.
    //
    // You can also construct quaternions via tags such as <@link tag VectorObject.to_axis_angle_quaternion> or <@link tag VectorObject.quaternion_between_vectors>.
    //
    // @description
    // A QuaternionTag represents a 3D rotation in an advanced mathematical format.
    // These are only useful in certain obscure cases, such as Display entities.
    // They have some useful capabilities, such as preventing 'gimbal lock' (a phenomenon where repeatedly rotating something eventually stops working).
    // See <@link url https://en.wikipedia.org/wiki/Quaternion> for more info about what a quaternion actually is.
    //
    // -->

    @Fetchable("quaternion")
    public static QuaternionTag valueOf(String string, TagContext context) {
        string = CoreUtilities.toLowerCase(string);
        if (string.startsWith("quaternion@")) {
            string = string.substring("quaternion@".length());
        }
        if (string.equals("identity")) {
            return new QuaternionTag(0, 0, 0, 1);
        }
        List<String> split = CoreUtilities.split(string, ',');
        if (split.size() != 4) {
            if (context == null || context.showErrors()) {
                Debug.log("Minor: valueOf QuaternionTag returning null: '" + string + "': not in valid quaternion format (x,y,z,w).");
            }
            return null;
        }
        try {
            double x = Double.parseDouble(split.get(0)),
                    y = Double.parseDouble(split.get(1)),
                    z = Double.parseDouble(split.get(2)),
                    w = Double.parseDouble(split.get(3));
            return new QuaternionTag(x, y, z, w);
        }
        catch (NumberFormatException ex) {
            if (context == null || context.showErrors()) {
                Debug.log("Minor: valueOf QuaternionTag returning null: '" + string + "': has invalid value: " + ex.getMessage());
            }
            return null;
        }
    }

    public static boolean matches(String string) {
        if (CoreUtilities.toLowerCase(string).startsWith("quaternion@")) {
            return true;
        }
        return valueOf(string, CoreUtilities.noDebugContext) != null;
    }

    private String prefix = "Quaternion";

    public double x, y, z, w;

    public QuaternionTag(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    @Override
    public String identify() {
        return "quaternion@" + CoreUtilities.doubleToString(x) + ","
                + CoreUtilities.doubleToString(y) + ","
                + CoreUtilities.doubleToString(z) + ","
                + CoreUtilities.doubleToString(w);
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String debuggable() {
        return "<LG>quaternion@<Y>" + CoreUtilities.doubleToString(x) + "<G>, <Y>"
                + CoreUtilities.doubleToString(y) + "<G>, <Y>"
                + CoreUtilities.doubleToString(z) + "<G>, <Y>"
                + CoreUtilities.doubleToString(w);
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    public static void register() {

        // <--[tag]
        // @attribute <QuaternionTag.x>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the raw X value of this quaternion.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "x", (attribute, object) -> {
            return new ElementTag(object.x);
        });

        // <--[tag]
        // @attribute <QuaternionTag.y>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the raw Y value of this quaternion.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "y", (attribute, object) -> {
            return new ElementTag(object.y);
        });

        // <--[tag]
        // @attribute <QuaternionTag.z>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the raw Z value of this quaternion.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "z", (attribute, object) -> {
            return new ElementTag(object.z);
        });

        // <--[tag]
        // @attribute <QuaternionTag.w>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the raw W value of this quaternion.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "w", (attribute, object) -> {
            return new ElementTag(object.w);
        });

        // <--[tag]
        // @attribute <QuaternionTag.xyz>
        // @returns ElementTag
        // @description
        // Returns the X, Y, and Z values of this quaternion as a simple "x,y,z" format.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "xyz", (attribute, object) -> {
            return new ElementTag(CoreUtilities.doubleToString(object.x) + "," + CoreUtilities.doubleToString(object.y) + "," + CoreUtilities.doubleToString(object.z));
        });

        // <--[tag]
        // @attribute <QuaternionTag.xyzw>
        // @returns ElementTag
        // @description
        // Returns the X, Y, Z, and W values of this quaternion as a simple "x,y,z,w" format.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "xyzw", (attribute, object) -> {
            return new ElementTag(CoreUtilities.doubleToString(object.x) + "," + CoreUtilities.doubleToString(object.y)
                    + "," + CoreUtilities.doubleToString(object.z) + "," + CoreUtilities.doubleToString(object.w));
        });

        // <--[tag]
        // @attribute <QuaternionTag.xyz_vector>
        // @returns VectorObject
        // @description
        // Returns the X, Y, and Z values of this quaternion as a <@link objecttype VectorObject>.
        // -->
        tagProcessor.registerStaticTag(VectorObject.class, "xyz_vector", (attribute, object) -> {
            return DenizenCore.implementation.getVector(object.x, object.y, object.z);
        });

        // <--[tag]
        // @attribute <QuaternionTag.plus[<quaternion>]>
        // @returns QuaternionTag
        // @description
        // Returns this quaternion "plus" another, using simple addition.
        // This is NOT the same as combining quaternions! For that, use <@link tag QuaternionTag.mul>.
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, QuaternionTag.class, "plus", (attribute, object, other) -> {
            return new QuaternionTag(object.x + other.x, object.y + other.y, object.z + other.z, object.w + other.w);
        });

        // <--[tag]
        // @attribute <QuaternionTag.mul[<quaternion>]>
        // @returns QuaternionTag
        // @description
        // Returns this quaternion multiplied by another, using quaternion multiplication.
        // Effectively combines the rotation represented by two quaternions.
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, QuaternionTag.class, "mul", (attribute, object, other) -> {
            return object.multipliedBy(other);
        });

        // <--[tag]
        // @attribute <QuaternionTag.scale[<factor>]>
        // @returns QuaternionTag
        // @description
        // Returns this quaternion multiplied by scaling factor.
        // This does NOT increase rotation angle.
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, ElementTag.class, "scale", (attribute, object, scale) -> {
            double s = scale.asDouble();
            return new QuaternionTag(object.x * s, object.y * s, object.z * s, object.w * s);
        });

        // <--[tag]
        // @attribute <QuaternionTag.normalize>
        // @returns QuaternionTag
        // @description
        // Returns a copy of this quaternion, normalized.
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, "normalize", (attribute, object) -> {
            return object.normalized();
        });

        // <--[tag]
        // @attribute <QuaternionTag.length>
        // @returns ElementTag(Decimal)
        // @synonyms QuaternionTag.magnitude
        // @description
        // Returns the length of this quaternion.
        // This value is not very useful in most cases.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "length", (attribute, object) -> {
            return new ElementTag(object.length());
        });

        // <--[tag]
        // @attribute <QuaternionTag.length_squared>
        // @returns ElementTag(Decimal)
        // @synonyms QuaternionTag.magnitude_squared
        // @description
        // Returns the squared length of this quaternion.
        // This value is not very useful in most cases.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "length_squared", (attribute, object) -> {
            return new ElementTag(object.lengthSquared());
        });

        // <--[tag]
        // @attribute <QuaternionTag.conjugate>
        // @returns QuaternionTag
        // @description
        // Returns the conjugate of this quaternion.
        // This is equivalent to (-x, -y, -z, w).
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, "conjugate", (attribute, object) -> {
            return object.conjugate();
        });

        // <--[tag]
        // @attribute <QuaternionTag.inverse>
        // @returns QuaternionTag
        // @description
        // Returns the inverse of this quaternion.
        // The inverse is the same amount of rotation, in the opposite direction.
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, "inverse", (attribute, object) -> {
            double len_sq = object.lengthSquared();
            return new QuaternionTag(-object.x * len_sq, -object.y * len_sq, -object.z * len_sq, object.w * len_sq);
        });

        // <--[tag]
        // @attribute <QuaternionTag.negative>
        // @returns QuaternionTag
        // @description
        // Returns the negative of this quaternion.
        // That is, (-x, -y, -z, -w).
        // This is not just making the rotation backwards, for that use <@link tag QuaternionTag.inverse>.
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, "negative", (attribute, object) -> {
            return object.negative();
        });

        // <--[tag]
        // @attribute <QuaternionTag.quaternion_between[<quaternion>]>
        // @returns QuaternionTag
        // @description
        // Returns the quaternion representing the rotation between this quaternion and another.
        // Equivalent to '[a].mul[<[b].conjugate>]'.
        // Returns a result such that b == a.mul[result].
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, QuaternionTag.class, "quaternion_between", (attribute, object, other) -> {
            return object.multipliedBy(other.conjugate());
        });

        // <--[tag]
        // @attribute <QuaternionTag.axis_angle_for[<vector>]>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the angle around the specified axis vector created by this quaternion.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, ObjectTag.class, "axis_angle_for", (attribute, object, param) -> {
            VectorObject axis = DenizenCore.implementation.vectorize(param, attribute.context);
            if (axis == null) {
                return null;
            }
            VectorObject ra = DenizenCore.implementation.getVector(object.x, object.y, object.z);
            VectorObject p = ra.project(axis);
            QuaternionTag twist = new QuaternionTag(p.getX(), p.getY(), p.getZ(), object.w).normalized();
            VectorObject new_forward = twist.transformX();
            return new ElementTag(vecToAngle(new_forward.getX(), new_forward.getY()));
        });

        // <--[tag]
        // @attribute <QuaternionTag.transform[<vector>]>
        // @returns VectorObject
        // @description
        // Returns a copy of the given vector, transformed by this quaternion.
        // -->
        tagProcessor.registerStaticTag(VectorObject.class, ObjectTag.class, "transform", (attribute, object, param) -> {
            return object.transform(DenizenCore.implementation.vectorize(param, attribute.context));
        });

        // <--[tag]
        // @attribute <QuaternionTag.represented_angle>
        // @returns ElementTag(Decimal)
        // @description
        // Returns the angle represented by this quaternion.
        // -->
        tagProcessor.registerStaticTag(ElementTag.class, "represented_angle", (attribute, object) -> {
            return new ElementTag(object.representedAngle());
        });

        // <--[tag]
        // @attribute <QuaternionTag.represented_axis>
        // @returns VectorObject
        // @description
        // Returns the axis represented by this quaternion, as a vector.
        // -->
        tagProcessor.registerStaticTag(VectorObject.class, "represented_axis", (attribute, object) -> {
            return object.representedAxis();
        });

        // <--[tag]
        // @attribute <QuaternionTag.slerp[end=<quaternion>;amount=<amount>]>
        // @returns QuaternionTag
        // @description
        // Returns the spherical linear interpolation ('slerp') between this quaternion and another.
        // -->
        tagProcessor.registerStaticTag(QuaternionTag.class, MapTag.class, "slerp", (attribute, object, map) -> {
            QuaternionTag end = map.getRequiredObjectAs("end", QuaternionTag.class, attribute);
            ElementTag slerpAmount = map.getRequiredObjectAs("amount", ElementTag.class, attribute);
            if (end == null || slerpAmount == null) {
                return null;
            }
            return object.slerp(end, slerpAmount.asDouble());
        });
    }

    public QuaternionTag slerp(QuaternionTag end, double slerpAmount) {
        double cosHalfTheta = w * end.w + x * end.x + y * end.y + z * end.z;
        if (cosHalfTheta < 0)
        {
            end = end.negative();
            cosHalfTheta = -cosHalfTheta;
        }
        if (cosHalfTheta > (1.0 - 1e-12))
        {
            return this;
        }
        double halfTheta = Math.acos(cosHalfTheta);
        double sinHalfTheta = Math.sqrt(1.0 - cosHalfTheta * cosHalfTheta);
        double aFraction = Math.sin((1 - slerpAmount) * halfTheta) / sinHalfTheta;
        double bFraction = Math.sin(slerpAmount * halfTheta) / sinHalfTheta;
        return new QuaternionTag(x * aFraction + end.x * bFraction, y * aFraction + end.y * bFraction,
                z * aFraction + end.z * bFraction, w * aFraction + end.w * bFraction);
    }

    public VectorObject representedAxis() {
        double sign = Math.signum(w);
        double x = this.x * sign,
                y = this.y * sign,
                z = this.z * sign;
        double len = Math.sqrt(x * x + y * y + z * z);
        if (len == 0) {
            len = 1;
        }
        len = 1 / len;
        return DenizenCore.implementation.getVector(x * len, y * len, z * len);
    }

    public double representedAngle() {
        double wAbs = Math.abs(w);
        if (wAbs > 1)
        {
            return 0;
        }
        return 2 * Math.acos(wAbs);
    }

    public static double vecToAngle(double x, double y)
    {
        if (x == 0 && y == 0) {
            return 0;
        }
        if (x != 0) {
            return Math.atan2(y, x);
        }
        if (y > 0) {
            return 0;
        }
        return Math.PI;
    }

    public VectorObject transform(VectorObject v) {
        VectorObject other = v.duplicate();
        double x2 = x * 2;
        double y2 = y * 2;
        double z2 = z * 2;
        double xx2 = x * x2;
        double xy2 = x * y2;
        double xz2 = x * z2;
        double yy2 = y * y2;
        double yz2 = y * z2;
        double zz2 = z * z2;
        double wx2 = w * x2;
        double wy2 = w * y2;
        double wz2 = w * z2;
        other.setX(v.getX() * (1f - yy2 - zz2) + v.getY() * (xy2 - wz2) + v.getZ() * (xz2 + wy2));
        other.setY(v.getX() * (xy2 + wz2) + v.getY() * (1f - xx2 - zz2) + v.getZ() * (yz2 - wx2));
        other.setZ(v.getX() * (xz2 - wy2) + v.getY() * (yz2 + wx2) + v.getZ() * (1f - xx2 - yy2));
        return other;
    }

    public VectorObject transformX() {
        double y2 = y * 2;
        double z2 = z * 2;
        return DenizenCore.implementation.getVector(1.0 - (y * y2) - (z * z2), (x * y2) + (w * z2), (x * z2) - (w * y2));
    }

    public double lengthSquared() {
        return x * x + y * y + z * z + w * w;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public QuaternionTag multipliedBy(QuaternionTag b) {
        return new QuaternionTag(
                x * b.w + b.x * w + y * b.z - z * b.y,
                y * b.w + b.y * w + z * b.x - x * b.z,
                z * b.w + b.z * w + x * b.y - y * b.x,
                w * b.w - x * b.x - y * b.y - z * b.z
        );
    }

    public QuaternionTag conjugate() {
        return new QuaternionTag(-x, -y, -z, w);
    }

    public QuaternionTag negative() {
        return new QuaternionTag(-x, -y, -z, -w);
    }

    public QuaternionTag normalized() {
        double len = 1.0 / length();
        return new QuaternionTag(x * len, y * len, z * len, w * len);
    }

    public static ObjectTagProcessor<QuaternionTag> tagProcessor = new ObjectTagProcessor<>();

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }
}
