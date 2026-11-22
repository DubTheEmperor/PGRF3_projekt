package app.object;

import transforms.Vec2D;
import transforms.Vec3D;

import java.util.ArrayList;
import java.util.List;

/**
 * Pravidelný dvacetistěn s poloměrem opsané koule 1 a středem v počátku.
 * Stěny se odvozují z topologie: trojice vrcholů, které jsou navzájem spojené hranou.
 * Každá stěna má číslo 1–20 a UV své buňky v atlasu textures/d20.png (kostka d20).
 */
public class Icosahedron extends SceneObject {
    private static final Vec3D COLOR = new Vec3D(0.3, 0.6, 0.9);
    private static final double PHI = (1 + Math.sqrt(5)) / 2;
    private static final int FACE_COUNT = 20;
    private static final double EPSILON = 1e-6;
    private static final int ATLAS_COLUMNS = 5;
    private static final int ATLAS_ROWS = 4;
    private static final double CELL_MARGIN = 12.0 / 256;   // okraj trojúhelníku v buňce (podíl velikosti buňky)

    public Icosahedron() {
        List<Vec3D> vertices = createVertices();
        List<int[]> faces = createFaces(vertices);

        int[] numbers = assignNumbers(vertices, faces);

        MeshBuilder builder = new MeshBuilder();
        for (int i = 0; i < faces.size(); i++) {
            int[] face = faces.get(i);
            Vec3D[] triangle = {vertices.get(face[0]), vertices.get(face[1]), vertices.get(face[2])};
            builder.addFace(triangle, atlasUvs(numbers[i]), COLOR);
        }

        buffers = builder.build();
    }

    // ======== Čísla stěn jako u kostky d20: protilehlé stěny mají součet 21 ========
    static int[] assignNumbers(List<Vec3D> vertices, List<int[]> faces) {
        int[] numbers = new int[faces.size()];
        int next = 1;
        for (int i = 0; i < faces.size(); i++) {
            if (numbers[i] != 0) continue;
            numbers[i] = next;
            numbers[oppositeFace(vertices, faces, i)] = FACE_COUNT + 1 - next;
            next++;
        }
        return numbers;
    }

    // Protilehlá stěna má vrcholy -v (těleso je středově souměrné podle počátku)
    private static int oppositeFace(List<Vec3D> vertices, List<int[]> faces, int faceIndex) {
        Vec3D centroid = centroid(vertices, faces.get(faceIndex));
        for (int j = 0; j < faces.size(); j++) {
            if (centroid.add(centroid(vertices, faces.get(j))).length() < EPSILON) return j;
        }
        throw new IllegalStateException("Dvacetistěn: stěna " + faceIndex + " nemá protilehlou stěnu");
    }

    private static Vec3D centroid(List<Vec3D> vertices, int[] face) {
        return vertices.get(face[0]).add(vertices.get(face[1])).add(vertices.get(face[2])).mul(1.0 / 3);
    }

    // ======== UV v atlasu d20.png ========
    // Musí odpovídat generátoru atlasu: 1280×1024 px, mřížka 5×4 čtvercových buněk (256 px),
    // buňka (číslo - 1) po řádcích; v buňce rovnostranný trojúhelník s okrajem 12 px, vrchol nahoře.
    // OGLTexture2D obrázek nepřeklápí => v = řádek obrázku / výška (v = 0 je horní okraj).
    private static Vec2D[] atlasUvs(int number) {
        int cell = number - 1;
        int column = cell % ATLAS_COLUMNS;
        int row = cell / ATLAS_COLUMNS;

        double width = 1 - 2 * CELL_MARGIN;
        double height = width * Math.sqrt(3) / 2;
        double top = (1 - height) / 2;
        double bottom = top + height;

        // Pořadí jako vrcholy stěny (CCW zvenku): levý dolní, pravý dolní, vrchol
        return new Vec2D[]{
                atlasUv(column, row, CELL_MARGIN, bottom),
                atlasUv(column, row, 1 - CELL_MARGIN, bottom),
                atlasUv(column, row, 0.5, top)
        };
    }

    private static Vec2D atlasUv(int column, int row, double x, double y) {
        return new Vec2D((column + x) / ATLAS_COLUMNS, (row + y) / ATLAS_ROWS);
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
