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
    private Mat4 model;

    public SceneEntity(String name, OGLBuffers buffers, Mat4 model) {
        this.name = name;
        this.buffers = buffers;
        this.initialModel = model;
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public OGLBuffers getBuffers() {
        return buffers;
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
