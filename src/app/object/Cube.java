package app.object;

import transforms.Vec2D;
import transforms.Vec3D;

/**
 * Krychle [-0.5, 0.5]^3 se středem v počátku. Každá stěna má vlastní vrcholy a UV 0–1.
 */
public class Cube extends SceneObject {
    private static final Vec3D COLOR = new Vec3D(0.9, 0.3, 0.2);

    public Cube() {
        MeshBuilder builder = new MeshBuilder();

        Vec3D x = new Vec3D(1, 0, 0);
        Vec3D y = new Vec3D(0, 1, 0);
        Vec3D z = new Vec3D(0, 0, 1);

        // Pro každou stěnu: normála n a tečné vektory u, v tak, aby u x v = n (CCW zvenku)
        addSide(builder, x, y, z);                       // +X
        addSide(builder, x.opposite(), z, y);            // -X
        addSide(builder, y, z, x);                       // +Y
        addSide(builder, y.opposite(), x, z);            // -Y
        addSide(builder, z, x, y);                       // +Z
        addSide(builder, z.opposite(), y, x);            // -Z

        buffers = builder.build();
    }

    private void addSide(MeshBuilder builder, Vec3D n, Vec3D u, Vec3D v) {
        Vec3D center = n.mul(0.5);
        Vec3D hu = u.mul(0.5);
        Vec3D hv = v.mul(0.5);

        Vec3D[] corners = {
                center.sub(hu).sub(hv),
                center.add(hu).sub(hv),
                center.add(hu).add(hv),
                center.sub(hu).add(hv)
        };
        Vec2D[] uvs = {
                new Vec2D(0, 0), new Vec2D(1, 0), new Vec2D(1, 1), new Vec2D(0, 1)
        };

        builder.addFace(corners, uvs, COLOR);
    }
}
