package app.object;

import lwjglutils.OGLBuffers;
import lwjglutils.OGLTexture2D;
import transforms.Mat4;
import transforms.Point3D;
import transforms.Vec3D;

/**
 * Objekt scény, který jde vybrat: název, geometrie, textury a modelová transformace.
 */
public class SceneEntity {
    private final String name;
    private final OGLBuffers buffers;
    private final Mat4 initialModel;
    private final OGLTexture2D textureColor;
    private final OGLTexture2D textureNormal;   // null = bez normal mapy (geometrická normála)
    private final boolean light;    // značka zdroje světla: kreslí se bez osvětlení, jde jen posouvat
    private Mat4 model;

    public SceneEntity(String name, OGLBuffers buffers, Mat4 model, OGLTexture2D textureColor) {
        this(name, buffers, model, textureColor, null);
    }

    public SceneEntity(String name, OGLBuffers buffers, Mat4 model, OGLTexture2D textureColor, OGLTexture2D textureNormal) {
        this(name, buffers, model, textureColor, textureNormal, false);
    }

    private SceneEntity(String name, OGLBuffers buffers, Mat4 model,
                        OGLTexture2D textureColor, OGLTexture2D textureNormal, boolean light) {
        this.name = name;
        this.buffers = buffers;
        this.initialModel = model;
        this.model = model;
        this.textureColor = textureColor;
        this.textureNormal = textureNormal;
        this.light = light;
    }

    /** Značka světla nemá textury, kreslí se jednou barvou. */
    public static SceneEntity createLight(String name, OGLBuffers buffers, Mat4 model) {
        return new SceneEntity(name, buffers, model, null, null, true);
    }

    public boolean isLight() {
        return light;
    }

    public String getName() {
        return name;
    }

    public OGLBuffers getBuffers() {
        return buffers;
    }

    public OGLTexture2D getTextureColor() {
        return textureColor;
    }

    public OGLTexture2D getTextureNormal() {
        return textureNormal;
    }

    public boolean isNormalMapped() {
        return textureNormal != null;
    }

    /** Lokální počátek objektu po modelové transformaci (střed orbity kamery, pozice světla). */
    public Vec3D getCenter() {
        return new Point3D().mul(model).ignoreW();
    }

    public Mat4 getModel() {
        return model;
    }

    public void setModel(Mat4 model) {
        this.model = model;
    }

    public void resetModel() {
        this.model = initialModel;
    }
}
