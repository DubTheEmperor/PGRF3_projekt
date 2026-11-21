package app.object;

import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Pravidelný dvacetistěn s poloměrem opsané koule 1 a středem v počátku.
 * Stěny se odvozují z topologie: trojice vrcholů, které jsou navzájem spojené hranou.
 */
public class Icosahedron extends SceneObject {
    private static final Vec3D COLOR = new Vec3D(0.3, 0.6, 0.9);
    private static final double PHI = (1 + Math.sqrt(5)) / 2;
    private static final int FACE_COUNT = 20;
    private static final double EPSILON = 1e-6;

    public Icosahedron() {
        List<Vec3D> vertices = createVertices();
        List<int[]> faces = createFaces(vertices);

        MeshBuilder builder = new MeshBuilder();
        Vec2D[] uvs = {new Vec2D(0, 0), new Vec2D(1, 0), new Vec2D(0.5, 1)};
        for (int[] face : faces) {
            Vec3D[] triangle = {vertices.get(face[0]), vertices.get(face[1]), vertices.get(face[2])};
            builder.addFace(triangle, uvs, COLOR);
        }

        buffers = builder.build();
    }

    // ======== Vrcholy: (0,±1,±φ), (±1,±φ,0), (±φ,0,±1), normalizované na poloměr 1 ========
    static List<Vec3D> createVertices() {
        List<Vec3D> vertices = new ArrayList<>();
        for (int s1 : new int[]{-1, 1}) {
            for (int s2 : new int[]{-1, 1}) {
                vertices.add(new Vec3D(0, s1, s2 * PHI));
                vertices.add(new Vec3D(s1, s2 * PHI, 0));
                vertices.add(new Vec3D(s2 * PHI, 0, s1));
            }
        }

        double scale = 1 / Math.sqrt(1 + PHI * PHI);
        vertices.replaceAll(v -> v.mul(scale));
        return vertices;
    }

    // ======== Stěny: trojice vrcholů se všemi vzdálenostmi rovnými délce hrany, orientované CCW zvenku ========
    static List<int[]> createFaces(List<Vec3D> vertices) {
        double edgeLength = minimalDistance(vertices);

        List<int[]> faces = new ArrayList<>();
        int n = vertices.size();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (!isEdge(vertices.get(i), vertices.get(j), edgeLength)) continue;
                for (int k = j + 1; k < n; k++) {
                    if (isEdge(vertices.get(i), vertices.get(k), edgeLength)
                            && isEdge(vertices.get(j), vertices.get(k), edgeLength)) {
                        faces.add(orientOutward(vertices, i, j, k));
                    }
                }
            }
        }

        if (faces.size() != FACE_COUNT) {
            throw new IllegalStateException("Dvacetistěn: nalezeno " + faces.size() + " stěn místo " + FACE_COUNT);
        }
        return faces;
    }

    private static int[] orientOutward(List<Vec3D> vertices, int a, int b, int c) {
        Vec3D pa = vertices.get(a);
        Vec3D pb = vertices.get(b);
        Vec3D pc = vertices.get(c);

        Vec3D normal = pb.sub(pa).cross(pc.sub(pa));
        Vec3D centroid = pa.add(pb).add(pc);

        // Těleso je centrované v počátku => normála stěny musí mířit stejně jako její těžiště
        return normal.dot(centroid) >= 0 ? new int[]{a, b, c} : new int[]{a, c, b};
    }

    private static double minimalDistance(List<Vec3D> vertices) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < vertices.size(); i++) {
            for (int j = i + 1; j < vertices.size(); j++) {
                min = Math.min(min, vertices.get(i).sub(vertices.get(j)).length());
            }
        }
        return min;
    }

    private static boolean isEdge(Vec3D a, Vec3D b, double edgeLength) {
        return Math.abs(a.sub(b).length() - edgeLength) < EPSILON;
    }
}
