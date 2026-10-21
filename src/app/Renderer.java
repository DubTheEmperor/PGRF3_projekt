package app;

import app.objLoader.ObjLoader;
import app.object.Axis;
import app.object.Floor;
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
    private OGLBuffers buffersAxis, buffersFloor, buffersDragon;
    private int shaderProgramPhong, shaderProgramAxis;
    private OGLTexture2D textureColor, textureNormal;

    private Camera camera;
    private Mat4 projection;
    private double cameraSpeed = 0.05;
    private double scrollSpeed = 0.5;
    private boolean mouseDown = false;
    private double lastX, lastY;
    private boolean keyW, keyA, keyS, keyD;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);
        glEnable(GL_DEPTH_TEST);

        // ========  Kamera a projekce ========
        {
            Vec3D observerPosition = new Vec3D(3, 3, 3);
            camera = new Camera()
                    .withPosition(observerPosition)
                    .withAzimuth(azimuthToOrigin(observerPosition))
                    .withZenith(zenithToOrigin(observerPosition))
                    .withFirstPerson(true);
            projection = new Mat4PerspRH(Math.toRadians(45), height / (double) width, 0.1f, 100f);
        }

        // ======== Textury ========
        try {
            textureColor = new OGLTexture2D("textures/brickwall.jpg");
            textureNormal = new OGLTexture2D("textures/brickwall_normal.jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ======== Scene objects ========
        buffersAxis = new Axis().getBuffers();
        buffersFloor = new Floor().getBuffers();
        buffersDragon = ObjLoader.load("obj/dragon.obj");

        // ======== Shaderové programy ========
        shaderProgramPhong = ShaderUtils.loadProgram("/phong");
        shaderProgramAxis = ShaderUtils.loadProgram("/axis");
    }

    public void display() {
        if (keyW) camera = camera.up(cameraSpeed);
        if (keyS) camera = camera.down(cameraSpeed);
        if (keyA) camera = camera.left(cameraSpeed);
        if (keyD) camera = camera.right(cameraSpeed);

        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // ======== Render Axis ========
        {
            glUseProgram(shaderProgramAxis);
            setGlobalUniforms(shaderProgramAxis);
            buffersAxis.draw(GL_LINES, shaderProgramAxis);
        }

        // ======== Render Floor ========
        {
            glUseProgram(shaderProgramPhong);
            setGlobalUniforms(shaderProgramPhong);
            int locUModel = glGetUniformLocation(shaderProgramPhong, "uModel");
            glUniformMatrix4fv(locUModel, false, new Mat4Identity().floatArray());

            textureColor.bind(shaderProgramPhong, "textureColor", 0);
            textureNormal.bind(shaderProgramPhong, "textureNormal", 1);

            buffersFloor.draw(GL_TRIANGLES, shaderProgramPhong);
        }

//        // ======== Render Dragon ========
//        {
//            glUseProgram(shaderProgramPhong);
//            setGlobalUniforms(shaderProgramPhong);
//            int locUModel = glGetUniformLocation(shaderProgramPhong, "uModel");
//            glUniformMatrix4fv(locUModel, false, new Mat4Scale(0.05f).mul(new Mat4RotX(Math.toRadians(90))).mul(new Mat4RotZ(Math.toRadians(90))).mul(new Mat4Transl(-0.25, 0, 0)).floatArray());
//            buffersDragon.draw(GL_TRIANGLES, shaderProgramPhong);
//        }
    }

    private void setGlobalUniforms(int shaderProgram) {
        // View matice - posíláme do shaderu jako uniform
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());

        // Proj matice - posíláme do shaderu jako uniform
        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, projection.floatArray());
    }

    // ====== Callbacks ======
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

    // ====== Metody kamery ======
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