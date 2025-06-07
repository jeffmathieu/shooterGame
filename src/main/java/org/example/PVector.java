package org.example;

public class PVector {

    public double x;
    public double y;
    public double z;

    public PVector(double x, double y, double z) {
        super();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PVector normalize() {
        float len = (float) Math.sqrt(x*x + y*y + z*z);
        if (len != 0) {
            this.x /= len;
            this.y /= len;
            this.z /= len;
        }
        return this;
    }

    public void div(double value) {
        x /= value;
        y /= value;
        z /= value;
    }

    public PVector mult(float n) {
        this.x *= n;
        this.y *= n;
        this.z *= n;
        return this;
    }

    /**
     * Overload that accepts a double scalar.
     */
    public PVector mult(double n) {
        this.x *= n;
        this.y *= n;
        this.z *= n;
        return this;
    }

    public void add(PVector v) {
        x += v.x;
        y += v.y;
        z += v.z;
    }

    public void sub(PVector v) {
        x -= v.x;
        y -= v.y;
        z -= v.z;
    }

    public void limit(float max) {
        if (mag() > max) {
            normalize();
            mult(max);
        }
    }

    public double mag() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static PVector sub(PVector v1, PVector v2) {
        return sub(v1, v2, null);
    }

    public static PVector sub(PVector v1, PVector v2, PVector target) {
        if (target == null) {
            target = new PVector(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
        } else {
            target.set(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z);
        }
        return target;
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public PVector copy() {
        return new PVector(this.x, this.y, this.z);
    }
}
