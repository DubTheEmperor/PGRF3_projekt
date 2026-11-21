package app.object;

import transforms.Vec2D;
import transforms.Vec3D;

/**
 * Čtyřboký jehlan: čtvercová podstava [-0.5, 0.5]^2 v rovině z = 0, vrchol v (0, 0, 1).
 */
public class Pyramid extends SceneObject {
    private static final Vec3D COLOR = new Vec3D(0.9, 0.8, 0.2);

    public Pyramid() {
        MeshBuilder builder = new MeshBuilder();

        // Podstava CCW při pohledu shora
        Vec3D[] base = {
                new Vec3D(-0.5, -0.5, 0),
                new Vec3D(0.5, -0.5, 0),
                new Vec3D(0.5, 0.5, 0),
                new Vec3D(-0.5, 0.5, 0)
        };
        Vec3D apex = new Vec3D(0, 0, 1);

        // ======== Podstava (normála -Z, proto obrácené pořadí) ========
        builder.addFace(
                new Vec3D[]{base[0], base[3], base[2], base[1]},
                new Vec2D[]{new Vec2D(0, 0), new Vec2D(0, 1), new Vec2D(1, 1), new Vec2D(1, 0)},
                COLOR
        );

        // ======== Boční stěny ========
        Vec2D[] sideUvs = {new Vec2D(0, 0), new Vec2D(1, 0), new Vec2D(0.5, 1)};
        for (int i = 0; i < base.length; i++) {
            Vec3D a = base[i];
            Vec3D b = base[(i + 1) % base.length];
            builder.addFace(new Vec3D[]{a, b, apex}, sideUvs, COLOR);
        }

        buffers = builder.build();
    }
}
