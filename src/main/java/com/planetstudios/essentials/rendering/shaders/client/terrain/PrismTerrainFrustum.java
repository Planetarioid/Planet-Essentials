package com.planetstudios.essentials.rendering.shaders.client.terrain;

import java.util.List;
import java.util.Objects;

/**
 * Immutable six-plane world-space frustum used to reject section AABBs before
 * a cooperating renderer performs GPU uploads or draw submission.
 *
 * <p>Plane normals need not be normalized. The inside half-space is
 * {@code nx*x + ny*y + nz*z + d >= 0}.</p>
 */
public final class PrismTerrainFrustum {
    private final Plane[] planes;

    public PrismTerrainFrustum(List<Plane> planes) {
        Objects.requireNonNull(planes, "planes");
        if (planes.size() != 6) throw new IllegalArgumentException("A terrain frustum must contain exactly six planes");
        this.planes = planes.toArray(Plane[]::new);
        for (Plane plane : this.planes) Objects.requireNonNull(plane, "plane");
    }

    /** Returns true when the world-space AABB intersects or is inside this frustum. */
    public boolean intersects(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)
                || !Double.isFinite(maxX) || !Double.isFinite(maxY) || !Double.isFinite(maxZ)
                || minX > maxX || minY > maxY || minZ > maxZ) throw new IllegalArgumentException("Invalid terrain bounds");
        for (Plane plane : planes) {
            double px = plane.x >= 0.0 ? maxX : minX;
            double py = plane.y >= 0.0 ? maxY : minY;
            double pz = plane.z >= 0.0 ? maxZ : minZ;
            if (plane.x * px + plane.y * py + plane.z * pz + plane.offset < 0.0) return false;
        }
        return true;
    }

    public record Plane(double x, double y, double z, double offset) {
        public Plane {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || !Double.isFinite(offset) || (x == 0.0 && y == 0.0 && z == 0.0))
                throw new IllegalArgumentException("Frustum plane must be finite with a non-zero normal");
        }
    }
}