package app;

import lwjglutils.OGLBuffers;
import lwjglutils.OGLTexture2D;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;


public class Renderer extends AbstractRenderer {
    /*
    teorie:
    zobrazovací řetězec
    vertex shader, fragment shader - co to je, k čemu to je (jaký má každý shader vlastnosti), jak to funguje
    phongův osvětlovací model

    praxe:
    umět napsat vertex a fragment shader
    rozumět všemu v Rendereru
     */

    private OGLBuffers buffers;
    private int shaderProgram;
    private OGLBuffers axisBuffers;
    private int axisShaderProgram;
    private OGLTexture2D texture;
    private OGLBuffers sqrBuffers;
    private int sqrShaderProgram;

    private float rotZ = 0;

    private Camera camera;
    private Mat4 projection;
    private double cameraSpeed = 0.05;
    private double scrollSpeed = 0.5;
    private boolean mouseDown = false;
    private double lastX, lastY;
    private boolean keyW, keyA, keyS, keyD;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        Vec3D observerPosition = new Vec3D(3, 3, 3);
        camera = new Camera()
                .withPosition(observerPosition)
                .withAzimuth(azimuthToOrigin(observerPosition))
                .withZenith(zenithToOrigin(observerPosition))
                .withFirstPerson(true);

        // Textures
        try {
            texture = new OGLTexture2D("./textures/poppy.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // trojúhelník
        float[] vb = {
                -1, -1, 0,      1, 0, 0,    0, 0, 1,
                1, -1, 0,       0, 1, 0,    0, 0, 1,
                1, 1, 0,        0, 0, 1,    0, 0, 1,
                -1, 1, 0,       0, 1, 0,    0, 0, 1,
        };

        int[] ib = {
                0, 1, 2,
                3, 0, 2
        };

        OGLBuffers.Attrib[] attribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3),
//                new OGLBuffers.Attrib("inTexCoord", 2),
                new OGLBuffers.Attrib("inNormal", 3)
        };

        buffers = new OGLBuffers(vb, attribs, ib);
        shaderProgram = ShaderUtils.loadProgram("/triangle");
        glUseProgram(shaderProgram);
        texture.bind(shaderProgram, "texturePoppy");


        // osy
        float axisLength = 5f;
        float[] axisVb = {
                // osa X - červená
                0f, 0f, 0f,           1, 0, 0,
                axisLength, 0f, 0f,   1, 0, 0,
                // osa Y - zelená
                0f, 0f, 0f,           0, 1, 0,
                0f, axisLength, 0f,   0, 1, 0,
                // osa Z - modrá
                0f, 0f, 0f,           0, 0, 1,
                0f, 0f, axisLength,   0, 0, 1
        };
        int[] axisIb = {0, 1, 2, 3, 4, 5};

        OGLBuffers.Attrib[] axisAttribs = {
                new OGLBuffers.Attrib("inPosition", 3),
                new OGLBuffers.Attrib("inColor", 3)
        };

        axisBuffers = new OGLBuffers(axisVb, axisAttribs, axisIb);
        axisShaderProgram = ShaderUtils.loadProgram("/axis");

        glEnable(GL_DEPTH_TEST);
    }

    public void display() {
        if (keyW) camera = camera.up(cameraSpeed);
        if (keyS) camera = camera.down(cameraSpeed);
        if (keyA) camera = camera.left(cameraSpeed);
        if (keyD) camera = camera.right(cameraSpeed);

        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glUseProgram(shaderProgram);

        projection = new Mat4PerspRH(Math.PI / 3, (double) height / width, 0.1, 100);

        int locUModel = glGetUniformLocation(shaderProgram, "uModel");
        int locUView  = glGetUniformLocation(shaderProgram, "uView");
        int locUProj  = glGetUniformLocation(shaderProgram, "uProj");

        Mat4 model = new Mat4Identity();
        glUniformMatrix4fv(locUModel, false, model.floatArray());
        glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());
        glUniformMatrix4fv(locUProj, false, projection.floatArray());

        // vykreslit
        buffers.draw(GL_TRIANGLES, shaderProgram);

        // osy
        glUseProgram(axisShaderProgram);

        glUniformMatrix4fv(glGetUniformLocation(axisShaderProgram, "uView"),
                false, camera.getViewMatrix().floatArray());
        glUniformMatrix4fv(glGetUniformLocation(axisShaderProgram, "uProj"),
                false, projection.floatArray());

        axisBuffers.draw(GL_LINES, axisShaderProgram);
    }

    protected GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            boolean pressed = action != GLFW_RELEASE;

            switch (key) {
                case GLFW_KEY_W -> keyW = pressed;
                case GLFW_KEY_A -> keyA = pressed;
                case GLFW_KEY_S -> keyS = pressed;
                case GLFW_KEY_D -> keyD = pressed;
            }
        }
    };

    protected GLFWMouseButtonCallback mouseCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                mouseDown = (action == GLFW_PRESS);
            }
        }
    };

    protected GLFWCursorPosCallback cursorCallback = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mouseDown) {
                double dx = x - lastX;
                double dy = y - lastY;
                camera = camera.addAzimuth(-dx / 100).addZenith(-dy / 100);
            }

            lastX = x;
            lastY = y;
        }
    };

    protected GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            if (dy > 0)
                camera = camera.forward(scrollSpeed);
            else if (dy < 0)
                camera = camera.backward(scrollSpeed);
        }
    };

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mouseCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cursorCallback;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }

    private double azimuthToOrigin(Vec3D observerPosition) {
        return observerPosition
                .opposite()
                .ignoreZ()
                .normalized()
                .map(viewNormalized -> {
                    double angle = Math.acos(viewNormalized.dot(new Vec2D(1, 0)));
                    return (viewNormalized.getY() > 0 ? angle : 2 * Math.PI - angle);
                })
                .orElse(0.0);
    }

    private double zenithToOrigin(Vec3D observerPosition) {
        return observerPosition
                .opposite()
                .normalized()
                .flatMap(view -> view.withZ(0).normalized().map(projection -> {
                    double angle = Math.acos(view.dot(projection));
                    return (view.getZ() > 0) ? angle : -angle;
                }))
                .orElse(0.0);
    }
}