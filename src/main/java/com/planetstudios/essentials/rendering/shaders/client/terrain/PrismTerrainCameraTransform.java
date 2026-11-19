package com.planetstudios.essentials.rendering.shaders.client.terrain;

/**
 * Camera-relative origin for one section-local terrain mesh.
 *
 * <p>Section coordinates are section indices, so each axis is scaled by the
 * 16-block section size before subtracting the camera position. Subtraction is
 * performed in double precision before conversion to floats, avoiding the
 * large-world precision loss caused by converting absolute world positions
 * to floats first.</p>
 *
 * <p>The resulting origin is intended for a per-section shader uniform (or
 * equivalent model transform). Vertex payloads remain immutable and
 * section-local, so cached GPU buffers do not need to be rebuilt when the
 * camera moves.</p>
 */
public record PrismTerrainCameraTransform(float originX, float originY, float originZ) {
    public static final int SECTION_SIZE = 16;

    public PrismTerrainCameraTransform {
        if (!Float.isFinite(originX) || !Float.isFinite(originY) || !Float.isFinite(originZ)) {
            throw new IllegalArgumentException("Camera-relative terrain origin must be finite");
        }
    }

    public static PrismTerrainCameraTransform forMesh(
            PrismTerrainMesh mesh, double cameraX, double cameraY, double cameraZ) {
        if (mesh == null) throw new NullPointerException("mesh");
        return forSection(mesh.sectionX(), mesh.sectionY(), mesh.sectionZ(),
                cameraX, cameraY, cameraZ);
    }

    public static PrismTerrainCameraTransform forSection(
            int sectionX, int sectionY, int sectionZ,
            double cameraX, double cameraY, double cameraZ) {
        requireFinite(cameraX, "cameraX");
        requireFinite(cameraY, "cameraY");
        requireFinite(cameraZ, "cameraZ");

        double relativeX = (long) sectionX * SECTION_SIZE - cameraX;
        double relativeY = (long) sectionY * SECTION_SIZE - cameraY;
        double relativeZ = (long) sectionZ * SECTION_SIZE - cameraZ;
        return new PrismTerrainCameraTransform(toFloat(relativeX), toFloat(relativeY), toFloat(relativeZ));
    }

    /** Returns a camera-relative vertex coordinate from a section-local value. */
    public float x(float sectionLocalX) { return addLocal(originX, sectionLocalX, "x"); }
    public float y(float sectionLocalY) { return addLocal(originY, sectionLocalY, "y"); }
    public float z(float sectionLocalZ) { return addLocal(originZ, sectionLocalZ, "z"); }

    private static float addLocal(float origin, float local, String axis) {
        if (!Float.isFinite(local) || local < 0.0f || local > SECTION_SIZE) {
            throw new IllegalArgumentException("Section-local " + axis + " must be finite and within [0, 16]");
        }
        float result = origin + local;
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException("Camera-relative " + axis + " coordinate overflowed");
        }
        return result;
    }

    private static float toFloat(double value) {
        if (!Double.isFinite(value) || value > Float.MAX_VALUE || value < -Float.MAX_VALUE) {
            throw new IllegalArgumentException("Camera-relative terrain origin is outside the supported float range");
        }
        return (float) value;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
