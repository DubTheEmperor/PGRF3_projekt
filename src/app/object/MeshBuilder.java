package app.object;

import lwjglutils.OGLBuffers;
import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Pomocník pro skládání geometrie těles.
 * Layout vrcholu: inPosition(3), inColor(3), inTexCoord(2), inNormal(3), inTangent(4).
 * inTangent.xyz = tečna (směr U v textuře), inTangent.w = ±1 orientace bitangenty (zrcadlené UV).
 */
public class MeshBuilder {
    private static final double EPSILON = 1e-9;

    /** Tečna stěny: směr rostoucího U a orientace bitangenty (B = cross(N, T) * handedness). */
    record Tangent(Vec3D direction, double handedness) {
    }

    private final List<Float> vertexData = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();
    private int vertexCount = 0;

    public int addVertex(Vec3D position, Vec3D color, Vec2D uv, Vec3D normal, Tangent tangent) {
        addVec3(position);
        addVec3(color);
        vertexData.add((float) uv.getX());
        vertexData.add((float) uv.getY());
        addVec3(normal);
        addVec3(tangent.direction());
        vertexData.add((float) tangent.handedness());
        return vertexCount++;
    }

    public void addTriangle(int a, int b, int c) {
        indices.add(a);
        indices.add(b);
        indices.add(c);
    }

    /**
     * Přidá konvexní rovinný mnohoúhelník (vrcholy CCW při pohledu zvenku).
     * Stěna má vlastní vrcholy s plochou normálou a tečnou, triangulace je vějířem z prvního vrcholu.
     */
    public void addFace(Vec3D[] polygon, Vec2D[] uvs, Vec3D color) {
        Vec3D edge1 = polygon[1].sub(polygon[0]);
        Vec3D edge2 = polygon[2].sub(polygon[0]);
        Vec3D normal = edge1.cross(edge2).normalized().orElse(new Vec3D(0, 0, 1));
        Tangent tangent = computeTangent(polygon[0], polygon[1], polygon[2], uvs[0], uvs[1], uvs[2], normal);

        int first = -1;
        for (int i = 0; i < polygon.length; i++) {
            int index = addVertex(polygon[i], color, uvs[i], normal, tangent);
            if (i == 0) first = index;
        }

        for (int i = 1; i < polygon.length - 1; i++) {
            addTriangle(first, first + i, first + i + 1);
        }
    }

    /**
     * Tečna trojúhelníku z pozic a UV: T = (e1 * dv2 - e2 * dv1) / det, kde det = du1 * dv2 - du2 * dv1.
     * Znaménko det určuje, zda je UV zrcadlené (handedness). Výsledek je ortogonalizovaný k normále.
     */
    static Tangent computeTangent(Vec3D p0, Vec3D p1, Vec3D p2, Vec2D uv0, Vec2D uv1, Vec2D uv2, Vec3D normal) {
        Vec3D e1 = p1.sub(p0);
        Vec3D e2 = p2.sub(p0);
        double du1 = uv1.getX() - uv0.getX(), dv1 = uv1.getY() - uv0.getY();
        double du2 = uv2.getX() - uv0.getX(), dv2 = uv2.getY() - uv0.getY();
        double det = du1 * dv2 - du2 * dv1;

        if (Math.abs(det) < EPSILON) {
            return new Tangent(anyPerpendicular(normal), 1.0);  // degenerované UV
        }

        Vec3D t = e1.mul(dv2).sub(e2.mul(dv1)).mul(1 / det);
        // Gram-Schmidt: odstranit složku ve směru normály
        Vec3D direction = t.sub(normal.mul(normal.dot(t))).normalized().orElse(anyPerpendicular(normal));
        return new Tangent(direction, Math.signum(det));
    }

    private static Vec3D anyPerpendicular(Vec3D normal) {
        Vec3D helper = Math.abs(normal.getX()) < 0.9 ? new Vec3D(1, 0, 0) : new Vec3D(0, 1, 0);
        return normal.cross(helper).normalized().orElse(new Vec3D(1, 0, 0));
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
                new OGLBuffers.Attrib("inNormal", 3),
                new OGLBuffers.Attrib("inTangent", 4)
        };

        return new OGLBuffers(vb, attribs, ib);
    }

    private void addVec3(Vec3D v) {
        vertexData.add((float) v.getX());
        vertexData.add((float) v.getY());
        vertexData.add((float) v.getZ());
    }
}
