package com.denizenscript.denizencore.utilities.math;

public class Vector3 {

    public double x, y, z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 crossProduct(Vector3 two) {
        return new Vector3(y * two.z - two.y * z, two.x * z - x * two.z, x * two.y - y * two.x);
    }

    public double dot(Vector3 two) {
        return x * two.x + y * two.y + z * two.z;
    }

    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
