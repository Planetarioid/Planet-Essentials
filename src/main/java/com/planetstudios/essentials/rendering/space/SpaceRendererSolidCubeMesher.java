package com.planetstudios.essentials.rendering.space;

import com.planetstudios.essentials.rendering.shaders.client.PrismFrameStage;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertex;
import com.planetstudios.essentials.rendering.shaders.client.terrain.PrismTerrainVertexData;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * First independent Space Renderer mesher.
 *
 * <p>Only full solid-render cube states are emitted. Faces touching another
 * solid-render state are culled using the immutable one-block snapshot border.
 * This deliberately does not attempt block-model, fluid, tint, or lighting
 * fidelity yet; its output is a CPU geometry milestone, not a draw-ready
 * replacement for vanilla terrain.</p>
 */
public final class SpaceRendererSolidCubeMesher {
    public static final String MATERIAL_ID = "space:solid_cube";
    private static final int WHITE = 0xFFFFFFFF;
    private static final int NO_OVERLAY = 0;
    private static final int FULL_BRIGHT = 0x00F000F0;

    private static final Face[] FACES = {
            new Face( 1, 0, 0,  1,0,0,  1,1,0,  1,1,1,  1,0,1),
            new Face(-1, 0, 0, -1,0,0,  0,0,1,  0,1,1,  0,1,0,  0,0,0),
            new Face( 0, 1, 0,  0,1,0,  0,1,1,  1,1,1,  1,1,0,  0,1,0),
            new Face( 0,-1, 0,  0,-1,0,  0,0,0,  1,0,0,  1,0,1,  0,0,1),
            new Face( 0, 0, 1,  0,0,1,  1,0,1,  1,1,1,  0,1,1,  0,0,1),
            new Face( 0, 0,-1,  0,0,-1,  0,0,0,  0,1,0,  1,1,0,  1,0,0)
    };

    private SpaceRendererSolidCubeMesher() {}

    /** Returns empty for an all-air/non-solid section. */
    public static Optional<PrismTerrainVertexData> build(SpaceRendererSectionSnapshot snapshot) {
        if (snapshot == null) throw new NullPointerException("snapshot");
        var key = snapshot.key();
        var builder = PrismTerrainVertexData.builder(key.x(), key.y(), key.z(),
                MATERIAL_ID, PrismFrameStage.BEFORE_ENTITIES);
        int faces = 0;

        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    BlockState state = snapshot.blockState(x, y, z);
                    if (!state.isSolidRender()) continue;

                    for (Face face : FACES) {
                        if (snapshot.blockState(x + face.dx, y + face.dy, z + face.dz).isSolidRender()) {
                            continue;
                        }
                        builder.addQuad(
                                vertex(x, y, z, face, 0),
                                vertex(x, y, z, face, 1),
                                vertex(x, y, z, face, 2),
                                vertex(x, y, z, face, 3));
                        faces++;
                    }
                }
            }
        }
        return faces == 0 ? Optional.empty() : Optional.of(builder.build());
    }

    private static PrismTerrainVertex vertex(int x, int y, int z, Face face, int corner) {
        int offset = corner * 3;
        float px = x + face.vertices[offset];
        float py = y + face.vertices[offset + 1];
        float pz = z + face.vertices[offset + 2];
        float u = (corner == 1 || corner == 2) ? 1.0f : 0.0f;
        float v = (corner >= 2) ? 1.0f : 0.0f;
        int normal = (face.nx & 0xFF) | ((face.ny & 0xFF) << 8) | ((face.nz & 0xFF) << 16);
        return new PrismTerrainVertex(px, py, pz, u, v, WHITE, FULL_BRIGHT, NO_OVERLAY, normal);
    }

    private static final class Face {
        final int dx, dy, dz, nx, ny, nz;
        final int[] vertices;
        Face(int dx, int dy, int dz, int nx, int ny, int nz,
                int x0,int y0,int z0, int x1,int y1,int z1,
                int x2,int y2,int z2, int x3,int y3,int z3) {
            this.dx=dx; this.dy=dy; this.dz=dz; this.nx=nx; this.ny=ny; this.nz=nz;
            this.vertices = new int[]{x0,y0,z0,x1,y1,z1,x2,y2,z2,x3,y3,z3};
        }
    }
}
