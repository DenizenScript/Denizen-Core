package com.denizenscript.denizencore.utilities.math;

public class Quaternion {

    public double x, y, z, w;

    public Quaternion(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public static Quaternion identity() {
        return new Quaternion(0, 0, 0, 1);
    }

    public static Quaternion fromAxisAngle(Vector3 axis, double angle) {
        double s = Math.sin(angle * 0.5);
        return new Quaternion(axis.x * s, axis.y * s, axis.z * s, Math.cos(angle * 0.5));
    }

    public static Quaternion getQuaternionBetween(Vector3 v1, Vector3 v2) {
        double dot = v1.dot(v2);
        if (dot < -0.9999f) {
            double absX = Math.abs(v1.x);
            double absY = Math.abs(v1.y);
            double absZ = Math.abs(v1.z);
            if (absX < absY && absX < absZ) {
                return new Quaternion(0, -v1.z, v1.y, 0).normalized();
            }
            else if (absY < absZ) {
                return new Quaternion(-v1.z, 0, v1.x, 0).normalized();
            }
            else {
                return new Quaternion(-v1.y, v1.x, 0, 0).normalized();
            }
        }
        else {
            Vector3 axis = v1.crossProduct(v2);
            return new Quaternion(axis.x, axis.y, axis.z, dot + 1).normalized();
        }
    }

    public Quaternion normalized() {
        double len_inv = 1 / Math.sqrt(x * x + y * y + z * z + w * w);
        return new Quaternion(x * len_inv, y * len_inv, z * len_inv, w * len_inv);
    }

    public Vector3 transform(Vector3 v) {
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
        return new Vector3(v.x * (1f - yy2 - zz2) + v.y * (xy2 - wz2) + v.z * (xz2 + wy2),
                v.x * (xy2 + wz2) + v.y * (1f - xx2 - zz2) + v.z * (yz2 - wx2),
                v.x * (xz2 - wy2) + v.y * (yz2 + wx2) + v.z * (1f - xx2 - yy2));
    }

    public Quaternion multipliedBy(Quaternion b) {
        return new Quaternion(
                x * b.w + b.x * w + y * b.z - z * b.y,
                y * b.w + b.y * w + z * b.x - x * b.z,
                z * b.w + b.z * w + x * b.y - y * b.x,
                w * b.w - x * b.x - y * b.y - z * b.z);
    }

    public Quaternion negative() {
        return new Quaternion(-x, -y, -z, -w);
    }

    public Quaternion inverse() {
        double len_sq = x * x + y * y + z * z + w * w;
        return new Quaternion(-x * len_sq, -y * len_sq, -z * len_sq, w * len_sq);
    }

    public Quaternion slerp(Quaternion end, double interpolationAmount) {
        double cosHalfTheta = w * end.w + x * end.x + y * end.y + z * end.z;
        if (cosHalfTheta < 0) {
            end = end.negative();
            cosHalfTheta = -cosHalfTheta;
        }
        if (cosHalfTheta > (1.0 - 1e-12)) {
            return this;
        }
        double halfTheta = Math.acos(cosHalfTheta);
        double sinHalfTheta = Math.sqrt(1.0 - cosHalfTheta * cosHalfTheta);
        double aFraction = Math.sin((1 - interpolationAmount) * halfTheta) / sinHalfTheta;
        double bFraction = Math.sin(interpolationAmount * halfTheta) / sinHalfTheta;
        return new Quaternion(x * aFraction + end.x * bFraction, y * aFraction + end.y * bFraction,
                z * aFraction + end.z * bFraction, w * aFraction + end.w * bFraction);
    }

    @Override
    public String toString() {
        return "(Quaternion: " + x + ", " + y + ", " + z + ", " + w + ")";
    }
}
