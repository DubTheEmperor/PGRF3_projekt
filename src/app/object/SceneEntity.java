package app.object;

import lwjglutils.OGLBuffers;
import transforms.Mat4;

/**
 * Objekt scény, který jde vybrat: název, geometrie a modelová transformace.
 */
public class SceneEntity {
    private final String name;
    private final OGLBuffers buffers;
    private final Mat4 initialModel;
    private final boolean normalMapped;
    private Mat4 model;

    public SceneEntity(String name, OGLBuffers buffers, Mat4 model) {
        this(name, buffers, model, false);
    }

    public SceneEntity(String name, OGLBuffers buffers, Mat4 model, boolean normalMapped) {
        this.name = name;
        this.buffers = buffers;
        this.initialModel = model;
        this.model = model;
        this.normalMapped = normalMapped;
    }

    public String getName() {
        return name;
    }

    public OGLBuffers getBuffers() {
        return buffers;
    }

    public boolean isNormalMapped() {
        return normalMapped;
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
