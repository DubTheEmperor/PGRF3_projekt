package app.object;

import lwjglutils.OGLBuffers;
import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Pomocník pro skládání geometrie těles.
 * Layout vrcholu odpovídá Floor: inPosition(3), inColor(3), inTexCoord(2), inNormal(3).
 */
public class MeshBuilder {
    private static final int FLOATS_PER_VERTEX = 11;

    private final List<Float> vertexData = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();
    private int vertexCount = 0;

    public int addVertex(Vec3D position, Vec3D color, Vec2D uv, Vec3D normal) {
        addVec3(position);
        addVec3(color);
        vertexData.add((float) uv.getX());
        vertexData.add((float) uv.getY());
        addVec3(normal);
        return vertexCount++;
    }

    public void addTriangle(int a, int b, int c) {
        indices.add(a);
        indices.add(b);
        indices.add(c);
    }

    /**
     * Přidá konvexní rovinný mnohoúhelník (vrcholy CCW při pohledu zvenku).
     * Stěna má vlastní vrcholy s plochou normálou a triangulace je vějířem z prvního vrcholu.
     */
    public void addFace(Vec3D[] polygon, Vec2D[] uvs, Vec3D color) {
        Vec3D edge1 = polygon[1].sub(polygon[0]);
        Vec3D edge2 = polygon[2].sub(polygon[0]);
        Vec3D normal = edge1.cross(edge2).normalized().orElse(new Vec3D(0, 0, 1));

        int first = -1;
        for (int i = 0; i < polygon.length; i++) {
            int index = addVertex(polygon[i], color, uvs[i], normal);
            if (i == 0) first = index;
        }

        for (int i = 1; i < polygon.length - 1; i++) {
            addTriangle(first, first + i, first + i + 1);
        }
    }

    public OGLBuffers build() {
        float[] vb = new float[vertexData.size()];
        for (int i = 0; i < vb.length; i++) {
            vb[i] = vertexData.get(i);
        }

        int[] ib = new int[indices.size()];
        for (int i = 0; i < ib.length; i++) {
            ib[i] = indices.get(i);
        }

        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal", 3)
        };

        return new OGLBuffers(vb, attribs, ib);
    }

    private void addVec3(Vec3D v) {
        vertexData.add((float) v.getX());
        vertexData.add((float) v.getY());
        vertexData.add((float) v.getZ());
    }
}
