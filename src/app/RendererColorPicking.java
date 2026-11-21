package app;

import app.objLoader.ObjLoader;
import app.object.Axis;
import app.object.Cube;
import app.object.Floor;
import app.object.Icosahedron;
import app.object.Pyramid;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;
import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;


public class RendererColorPicking extends AbstractRenderer {
    // ======== Picking ID (hodnota červeného kanálu 0–255) ========
    private static final int ID_NONE = 0;   // pozadí picking textury (osy se do ní nekreslí)
    private static final int ID_FLOOR = 30;
    private static final int ID_DRAGON = 70;
    private static final int ID_CUBE = 110;
    private static final int ID_PYRAMID = 150;
    private static final int ID_ICOSAHEDRON = 190;

    // Šedé pozadí obrazovky
    private static final float SCREEN_BACKGROUND = 0.1f;

    private OGLBuffers buffersAxis, buffersFloor, buffersDragon, buffersCube, buffersPyramid, buffersIcosahedron;
    private int shaderProgramPhong, shaderProgramAxis, shaderProgramColorPicking;
    private OGLTexture2D textureColor, textureNormal;

    private Camera camera;
    private boolean mousePressed = false;
    private double lastX, lastY;
    private Mat4PerspRH proj;

    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer viewer;

    public void init() {
        renderTarget = new OGLRenderTarget(width, height);
        viewer = new OGLTexture2D.Viewer();

        // ======== Kamera a projekce ========
        {
            camera = new Camera()
                    .withPosition(new Vec3D(0, 0, 0))
                    .withAzimuth(Math.toRadians(135))
                    .withZenith(Math.toRadians(-45))
                    .withRadius(5)
                    .withFirstPerson(false);
            proj = new Mat4PerspRH(Math.toRadians(45), height / (double) width, 0.1f, 100f);
        }

        // ======== Textury ========
        try {
            textureColor = new OGLTexture2D("./textures/brickwall.jpg");
            textureNormal = new OGLTexture2D("./textures/brickwall_normal.jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ======== Scene objects ========
        buffersAxis = new Axis().getBuffers();
        buffersFloor = new Floor().getBuffers();
        buffersDragon = ObjLoader.load("obj/dragon.obj");
        buffersCube = new Cube().getBuffers();
        buffersPyramid = new Pyramid().getBuffers();
        buffersIcosahedron = new Icosahedron().getBuffers();

        // ======== Shaderové programy ========
        shaderProgramPhong = ShaderUtils.loadProgram("/phong");
        shaderProgramAxis = ShaderUtils.loadProgram("/axis");
        shaderProgramColorPicking = ShaderUtils.loadProgram("/colorPicking");
        // TODO: shaderový program, který obarvuje ID barvou
    }

    public void display() {
        glEnable(GL_DEPTH_TEST);

        // TODO: Vykreslit scénu do textury
        // TODO: shaderovým program, který obarvuje ID barvou
        renderTarget.bind();
        glViewport(0, 0, width, height);
        glClearColor(ID_NONE / 255.0f, .0f, .0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramColorPicking);

        // Kresli na obrazovku
        // TODO: tady už renderuju phongem
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glClearColor(SCREEN_BACKGROUND, SCREEN_BACKGROUND, SCREEN_BACKGROUND, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramPhong);
        renderAxis(); // osy se vykreslí jen na obrazovku

        glDisable(GL_DEPTH_TEST);
        viewer.view(renderTarget.getColorTexture(), -1, -1, 0.5);
    }

    private void renderAxis() {
        glUseProgram(shaderProgramAxis);
        setGlobalUniforms(shaderProgramAxis);
        buffersAxis.draw(GL_LINES, shaderProgramAxis);
    }

    // Kreslí jen objekty, které jde vybrat (používá se v picking i Phong průchodu)
    private void renderScene(int shaderProgram) {
        // ======== Render objektů scény ========
        // Modelové matice: row-vector konvence => A.mul(B) aplikuje nejdřív A, potom B
        renderObject(shaderProgram, buffersFloor,
                new Mat4RotY(Math.toRadians(90)), ID_FLOOR);

        renderObject(shaderProgram, buffersDragon,
                new Mat4Scale(0.05f).mul(new Mat4RotX(Math.toRadians(90))).mul(new Mat4RotZ(Math.toRadians(90))).mul(new Mat4Transl(-0.25, 0, 0)),
                ID_DRAGON);

        renderObject(shaderProgram, buffersCube,
                new Mat4Scale(0.5).mul(new Mat4Transl(0.8, 1.6, 0.25)), ID_CUBE);

        renderObject(shaderProgram, buffersPyramid,
                new Mat4Scale(0.6).mul(new Mat4Transl(0.8, -1.6, 0)), ID_PYRAMID);

        renderObject(shaderProgram, buffersIcosahedron,
                new Mat4Scale(0.35).mul(new Mat4Transl(-1.2, -1.0, 0.30)), ID_ICOSAHEDRON);
    }

    private void renderObject(int shaderProgram, OGLBuffers buffers, Mat4 model, int pickingId) {
        glUseProgram(shaderProgram);
        setGlobalUniforms(shaderProgram);

        int locUModel = glGetUniformLocation(shaderProgram, "uModel");
        glUniformMatrix4fv(locUModel, false, model.floatArray());

        // ID pro color picking (Phong uniform uColor ignoruje)
        int locUColor = glGetUniformLocation(shaderProgram, "uColor");
        glUniform3f(locUColor, pickingId / 255.0f, .0f, .0f);

        textureColor.bind(shaderProgram, "textureColor", 0);
        textureNormal.bind(shaderProgram, "textureNormal", 1);
        buffers.draw(GL_TRIANGLES, shaderProgram);
    }

    private void setGlobalUniforms(int shaderProgram) {
        // View matice - posíláme do shaderu jako uniform
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());
        // Proj matice - posíláme do shaderu jako uniform
        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, proj.floatArray());
    }

    // ====== Callbacks ======
    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
        }
    };

    private GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            if (button == 0) {
                mousePressed = action == 1;
            }

            DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
            DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
            glfwGetCursorPos(window, xBuffer, yBuffer);
            double x = xBuffer.get(0);
            double y = yBuffer.get(0);

            OGLTexImageFloat image = renderTarget.getColorTexture().getTexImage(new OGLTexImageFloat.Format(4));
            System.out.println("x: " + x);
            System.out.println("y: " + y);

            int texY = renderTarget.getHeight() - (int)Math.round(y) - 1;
            float id = image.getPixel((int)Math.round(x), texY, 0) * 255;
            System.out.println("id: " + id);
            switch ((int) Math.round(id)) {
                case ID_FLOOR -> System.out.println("Floor");
                case ID_DRAGON -> System.out.println("Dragon");
                case ID_CUBE -> System.out.println("Cube");
                case ID_PYRAMID -> System.out.println("Pyramid");
                case ID_ICOSAHEDRON -> System.out.println("Icosahedron");
                default -> System.out.println("Nic nevybráno");
            }

        }
    };

    private GLFWCursorPosCallback cpCallbacknew = new GLFWCursorPosCallback() {
        @Override
        public void invoke(long window, double x, double y) {
            if (mousePressed) {
                camera = camera.addAzimuth(Math.toRadians(lastX - x) * 0.3);
                camera = camera.addZenith(Math.toRadians(lastY - y) * 0.3);
            }
            lastX = x;
            lastY = y;
        }
    };

    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            camera = camera.mulRadius(Math.pow(0.8, dy));
        }
    };

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }

    @Override
    public GLFWMouseButtonCallback getMouseCallback() {
        return mbCallback;
    }

    @Override
    public GLFWCursorPosCallback getCursorCallback() {
        return cpCallbacknew;
    }

    @Override
    public GLFWScrollCallback getScrollCallback() {
        return scrollCallback;
    }


}