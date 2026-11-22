package app.objLoader;

import app.Vertex;
import app.object.MeshBuilder;
import lwjglutils.OGLBuffers;
import transforms.Vec2D;
import transforms.Vec3D;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Načítání OBJ (trojúhelníky v/vt/vn) do OGLBuffers.
 * Layout vrcholu: inPosition(3), inTexCoord(2), inNormal(3), inTangent(4).
 */
public class ObjLoader {
    private static final int FLOATS_PER_VERTEX = 12;

    /** Klíč pozice pro sdílení normál mezi rohy se stejnou polohou (OBJ může mít pro každý roh vlastní v). */
    private record PositionKey(double x, double y, double z) {
        PositionKey(Vec3D p) {
            this(p.getX(), p.getY(), p.getZ());
        }
    }

    /** UV i normály ze souboru. */
    public static OGLBuffers load(String resource) {
        return build(parse(resource));
    }

    /** UV ze souboru, normály ze souboru se zahodí a spočítají se z geometrie (hladké stínování). */
    public static OGLBuffers loadWithComputedNormals(String resource) {
        return build(computeSmoothNormals(parse(resource)));
    }

    // ======== Parsování ========
    private static List<Vertex> parse(String resource) {
        // Loader neřeší "o", "g" a "materiál"
        List<Vec3D> positions = new ArrayList<>();
        List<Vec2D> uvs = new ArrayList<>();
        List<Vec3D> normals = new ArrayList<>();
        List<Vertex> vertices = new ArrayList<>();

        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (inputStream == null) {
            throw new RuntimeException("Resource nenalezen: " + resource);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] currentLine = line.split(" ");

                if (line.startsWith("v ")) {
                    positions.add(parseVec3(currentLine));
                } else if (line.startsWith("vt ")) {
                    double u = Double.parseDouble(currentLine[1]);
                    double v = Double.parseDouble(currentLine[2]);
                    uvs.add(new Vec2D(u, v));
                } else if (line.startsWith("vn ")) {
                    normals.add(parseVec3(currentLine));
                } else if (line.startsWith("f ")) {
                    // Pozor: faces nemusí být jen 3 vrcholy, nutná triangulace
                    // Pokud se v různých faces objevuje stejný vrchol, zbytečně ho znovu přidáváme do seznamu vrcholů
                    // Vrchol nemusí mít uv
                    // Vrchol nemusí mít normálu
                    for (int i = 1; i <= 3; i++) {
                        vertices.add(parseFaceVertex(currentLine[i], positions, uvs, normals));
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return vertices;
    }

    private static Vec3D parseVec3(String[] tokens) {
        return new Vec3D(Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]));
    }

    private static Vertex parseFaceVertex(String token, List<Vec3D> positions, List<Vec2D> uvs, List<Vec3D> normals) {
        String[] indices = token.split("/");
        return new Vertex(
                positions.get(Integer.parseInt(indices[0]) - 1),
                uvs.get(Integer.parseInt(indices[1]) - 1),
                normals.get(Integer.parseInt(indices[2]) - 1)
        );
    }

    // ======== Hladké normály ========
    // Normála stěny (cross produkt => vážená plochou) se přičte všem rohům se stejnou pozicí, pak se normalizuje.
    // Předpokládá vinutí CCW při pohledu zvenku.
    private static List<Vertex> computeSmoothNormals(List<Vertex> vertices) {
        Map<PositionKey, Vec3D> sums = new HashMap<>();
        for (int i = 0; i < vertices.size(); i += 3) {
            Vec3D faceNormal = faceNormal(vertices, i);
            for (int j = i; j < i + 3; j++) {
                sums.merge(new PositionKey(vertices.get(j).getPosition()), faceNormal, Vec3D::add);
            }
        }

        List<Vertex> result = new ArrayList<>(vertices.size());
        for (Vertex v : vertices) {
            Vec3D normal = sums.get(new PositionKey(v.getPosition())).normalized().orElse(new Vec3D(0, 0, 1));
            result.add(new Vertex(v.getPosition(), v.getUv(), normal));
        }
        return result;
    }

    // Nenormalizovaná normála trojúhelníku začínajícího na indexu first (délka = 2× plocha)
    private static Vec3D faceNormal(List<Vertex> vertices, int first) {
        Vec3D p0 = vertices.get(first).getPosition();
        Vec3D p1 = vertices.get(first + 1).getPosition();
        Vec3D p2 = vertices.get(first + 2).getPosition();
        return p1.sub(p0).cross(p2.sub(p0));
    }

    // ======== Vertex buffer ========
    private static OGLBuffers build(List<Vertex> vertices) {
        float[] vb = new float[vertices.size() * FLOATS_PER_VERTEX];
        int[] ib = new int[vertices.size()];

        // Pozor: nyní si tohle můžeme dovolit, protože máme jednoduché trojúhelníky, které jdou za sebou
        for (int i = 0; i < vertices.size(); i++) {
            ib[i] = i;
        }

        int index = 0;
        for (int i = 0; i < vertices.size(); i += 3) {
            MeshBuilder.Tangent tangent = triangleTangent(vertices, i);
            for (int j = i; j < i + 3; j++) {
                index = writeVertex(vb, index, vertices.get(j), tangent);
            }
        }

        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal", 3),
                new OGLBuffers.Attrib("inTangent", 4)
        };

        return new OGLBuffers(vb, attribs, ib);
    }

    // Tečna z pozic a UV trojúhelníku; k normále vrcholu ji ortogonalizuje až phong.vert (Gram-Schmidt)
    private static MeshBuilder.Tangent triangleTangent(List<Vertex> vertices, int first) {
        Vertex v0 = vertices.get(first), v1 = vertices.get(first + 1), v2 = vertices.get(first + 2);
        Vec3D normal = faceNormal(vertices, first).normalized().orElse(new Vec3D(0, 0, 1));
        return MeshBuilder.computeTangent(v0.getPosition(), v1.getPosition(), v2.getPosition(),
                v0.getUv(), v1.getUv(), v2.getUv(), normal);
    }

    private static int writeVertex(float[] vb, int index, Vertex v, MeshBuilder.Tangent tangent) {
        index = writeVec3(vb, index, v.getPosition());
        vb[index++] = (float) v.getUv().getX();
        vb[index++] = (float) v.getUv().getY();
        index = writeVec3(vb, index, v.getNormal());
        index = writeVec3(vb, index, tangent.direction());
        vb[index++] = (float) tangent.handedness();
        return index;
    }

    private static int writeVec3(float[] vb, int index, Vec3D v) {
        vb[index++] = (float) v.getX();
        vb[index++] = (float) v.getY();
        vb[index++] = (float) v.getZ();
        return index;
    }
}
