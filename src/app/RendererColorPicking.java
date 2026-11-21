package app;

import app.objLoader.ObjLoader;
import app.object.Axis;
import app.object.Cube;
import app.object.Floor;
import app.object.Icosahedron;
import app.object.Pyramid;
import app.object.SceneEntity;
import lwjglutils.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.*;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_RG;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;


public class RendererColorPicking extends AbstractRenderer {
    // ======== Picking ID (0–255) ========
    // ID je v červeném kanálu, u aktivního objektu v zeleném (zvýraznění v náhledu). Dekóduje se z R + G.
    // ID objektu = (index + 1) * ID_STEP => max. 255 / ID_STEP objektů (pro víc objektů zmenšit krok)
    private static final int ID_STEP = 40;
    private static final int ID_NONE = 0;   // pozadí picking textury (osy se do ní nekreslí)
    private static final int NO_SELECTION = -1;

    // Šedé pozadí obrazovky
    private static final float SCREEN_BACKGROUND = 0.1f;

    // ======== Transformace aktivního objektu (krok na jeden stisk Q/E) ========
    private static final double TRANSLATE_STEP = 0.1;
    private static final double ROTATE_STEP = Math.toRadians(5);
    private static final double SCALE_STEP = 0.1;

    private enum TransformMode {
        TRANSLATE("Posun"), ROTATE("Rotace"), SCALE("Scale");

        private final String label;

        TransformMode(String label) {
            this.label = label;
        }
    }

    private OGLBuffers buffersAxis;
    private final List<SceneEntity> entities = new ArrayList<>();
    private int activeIndex = NO_SELECTION;

    private TransformMode transformMode = TransformMode.TRANSLATE;
    private Vec3D transformAxis = new Vec3D(1, 0, 0);
    private String transformAxisName = "X";

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
        textRenderer = new OGLTextRenderer(width, height);

        // ======== Kamera a projekce ========
        {
            camera = new Camera()
                    .withPosition(new Vec3D(0, 0, 0))
                    .withAzimuth(Math.toRadians(135))
                    .withZenith(Math.toRadians(-45))
                    .withRadius(5)
                    .withFirstPerson(false);
            proj = createProjection();
        }

        // ======== Textury ========
        try {
            textureColor = new OGLTexture2D("./textures/brickwall.jpg");
            textureNormal = new OGLTexture2D("./textures/brickwall_normal.jpg");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // ======== Scene objects ========
        // Pořadí v seznamu určuje picking ID i přepínání šipkami
        // Modelové matice: row-vector konvence => A.mul(B) aplikuje nejdřív A, potom B
        buffersAxis = new Axis().getBuffers();
        entities.add(new SceneEntity("Floor", new Floor().getBuffers(),
                new Mat4RotY(Math.toRadians(90))));
        entities.add(new SceneEntity("Dragon", ObjLoader.load("obj/dragon.obj"),
                new Mat4Scale(0.05f).mul(new Mat4RotX(Math.toRadians(90))).mul(new Mat4RotZ(Math.toRadians(90))).mul(new Mat4Transl(-0.25, 0, 0))));
        entities.add(new SceneEntity("Cube", new Cube().getBuffers(),
                new Mat4Scale(0.5).mul(new Mat4Transl(0.8, 1.6, 0.25))));
        entities.add(new SceneEntity("Pyramid", new Pyramid().getBuffers(),
                new Mat4Scale(0.6).mul(new Mat4Transl(0.8, -1.6, 0))));
        entities.add(new SceneEntity("Icosahedron", new Icosahedron().getBuffers(),
                new Mat4Scale(0.35).mul(new Mat4Transl(-1.2, -1.0, 0.30))));

        // ======== Shaderové programy ========
        shaderProgramPhong = ShaderUtils.loadProgram("/phong");
        shaderProgramAxis = ShaderUtils.loadProgram("/axis");
        shaderProgramColorPicking = ShaderUtils.loadProgram("/colorPicking");
    }

    public void display() {
        resizeRenderTargetIfNeeded();
        glEnable(GL_DEPTH_TEST);

        // ======== 1. průchod: ID barvy do textury (color picking) ========
        renderTarget.bind();
        glViewport(0, 0, width, height);
        glClearColor(ID_NONE / 255.0f, .0f, .0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramColorPicking);

        // ======== 2. průchod: Phong na obrazovku ========
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glClearColor(SCREEN_BACKGROUND, SCREEN_BACKGROUND, SCREEN_BACKGROUND, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramPhong);
        renderAxis(); // osy se vykreslí jen na obrazovku => nejdou vybrat

        glDisable(GL_DEPTH_TEST);
        viewer.view(renderTarget.getColorTexture(), -1, -1, 0.5);

        // ======== Text UI ========
        String activeName = activeIndex == NO_SELECTION ? "žádný" : entities.get(activeIndex).getName();
        textRenderer.addStr2D(3, 20, "Aktivní objekt: " + activeName);
        textRenderer.addStr2D(3, 40, "Osa: " + transformAxisName + " (1/2/3) | Režim: " + transformMode.label
                + " (4/5/6) | Q/E: −/+ | Backspace: reset");
    }

    private void renderAxis() {
        glUseProgram(shaderProgramAxis);
        setGlobalUniforms(shaderProgramAxis);
        buffersAxis.draw(GL_LINES, shaderProgramAxis);
    }

    // Kreslí jen objekty, které jde vybrat (používá se v picking i Phong průchodu)
    private void renderScene(int shaderProgram) {
        for (int i = 0; i < entities.size(); i++) {
            renderObject(shaderProgram, entities.get(i), pickingIdFromIndex(i), i == activeIndex);
        }
    }

    private void renderObject(int shaderProgram, SceneEntity entity, int pickingId, boolean active) {
        glUseProgram(shaderProgram);
        setGlobalUniforms(shaderProgram);

        int locUModel = glGetUniformLocation(shaderProgram, "uModel");
        glUniformMatrix4fv(locUModel, false, entity.getModel().floatArray());

        setPickingColor(shaderProgram, pickingId, active);

        textureColor.bind(shaderProgram, "textureColor", 0);
        textureNormal.bind(shaderProgram, "textureNormal", 1);
        entity.getBuffers().draw(GL_TRIANGLES, shaderProgram);
    }

    // ID pro color picking: neaktivní objekt červeně, aktivní zeleně (Phong uniform uColor ignoruje)
    private void setPickingColor(int shaderProgram, int pickingId, boolean active) {
        float id = pickingId / 255.0f;
        int locUColor = glGetUniformLocation(shaderProgram, "uColor");
        if (active)
            glUniform3f(locUColor, .0f, id, .0f);
        else
            glUniform3f(locUColor, id, .0f, .0f);
    }

    private void setGlobalUniforms(int shaderProgram) {
        // View matice - posíláme do shaderu jako uniform
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, camera.getViewMatrix().floatArray());
        // Proj matice - posíláme do shaderu jako uniform
        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, proj.floatArray());
    }

    // ====== Velikost okna ======
    private Mat4PerspRH createProjection() {
        return new Mat4PerspRH(Math.toRadians(45), height / (double) width, 0.1f, 100f);
    }

    // Picking textura musí mít stejnou velikost jako okno, jinak by souřadnice kurzoru neseděly
    private void resizeRenderTargetIfNeeded() {
        if (renderTarget.getWidth() != width || renderTarget.getHeight() != height) {
            renderTarget = new OGLRenderTarget(width, height);
            proj = createProjection();
        }
    }

    // ====== Výběr aktivního objektu ======
    private int pickingIdFromIndex(int index) {
        return (index + 1) * ID_STEP;
    }

    private int indexFromPickingId(int pickingId) {
        if (pickingId == ID_NONE || pickingId % ID_STEP != 0) return NO_SELECTION;
        int index = pickingId / ID_STEP - 1;
        return index < entities.size() ? index : NO_SELECTION;
    }

    private void setActive(int index) {
        activeIndex = index;
    }

    private void selectNext() {
        int n = entities.size();
        setActive(activeIndex == NO_SELECTION ? 0 : (activeIndex + 1) % n);
    }

    private void selectPrevious() {
        int n = entities.size();
        setActive(activeIndex == NO_SELECTION ? n - 1 : (activeIndex - 1 + n) % n);
    }

    // ====== Transformace aktivního objektu ======
    private void setTransformAxis(Vec3D axis, String name) {
        transformAxis = axis;
        transformAxisName = name;
    }

    private void transformActive(int sign) {
        if (activeIndex == NO_SELECTION) return;
        SceneEntity entity = entities.get(activeIndex);
        Mat4 model = entity.getModel();

        switch (transformMode) {
            case TRANSLATE -> model = model.mul(new Mat4Transl(transformAxis.mul(sign * TRANSLATE_STEP)));
            case ROTATE -> model = new Mat4Rot(sign * ROTATE_STEP, transformAxis).mul(model);
            case SCALE -> model = new Mat4Scale(new Vec3D(1, 1, 1).add(transformAxis.mul(sign * SCALE_STEP))).mul(model);
        }

        entity.setModel(model);
    }

    private void resetActive() {
        if (activeIndex == NO_SELECTION) return;
        entities.get(activeIndex).resetModel();
    }

    // Přečte jeden pixel picking textury pod kurzorem a vrátí index objektu (nebo NO_SELECTION)
    private int pickEntityAt(double cursorX, double cursorY) {
        int x = (int) cursorX;
        int y = renderTarget.getHeight() - (int) cursorY - 1; // okno má počátek vlevo nahoře, textura vlevo dole
        if (x < 0 || x >= renderTarget.getWidth() || y < 0 || y >= renderTarget.getHeight()) {
            return NO_SELECTION;
        }

        FloatBuffer pixel = BufferUtils.createFloatBuffer(2);
        renderTarget.bind();
        glReadPixels(x, y, 1, 1, GL_RG, GL_FLOAT, pixel);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // Nenulový je vždy jen jeden kanál (R = neaktivní, G = aktivní) => součet je přímo ID
        int pickingId = Math.round((pixel.get(0) + pixel.get(1)) * 255);
        return indexFromPickingId(pickingId);
    }

    // ====== Callbacks ======
    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }

            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_RIGHT -> selectNext();
                    case GLFW_KEY_LEFT -> selectPrevious();
                    case GLFW_KEY_1 -> setTransformAxis(new Vec3D(1, 0, 0), "X");
                    case GLFW_KEY_2 -> setTransformAxis(new Vec3D(0, 1, 0), "Y");
                    case GLFW_KEY_3 -> setTransformAxis(new Vec3D(0, 0, 1), "Z");
                    case GLFW_KEY_4 -> transformMode = TransformMode.TRANSLATE;
                    case GLFW_KEY_5 -> transformMode = TransformMode.ROTATE;
                    case GLFW_KEY_6 -> transformMode = TransformMode.SCALE;
                    case GLFW_KEY_BACKSPACE -> resetActive();
                }
            }

            if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                switch (key) {
                    case GLFW_KEY_Q -> transformActive(-1);
                    case GLFW_KEY_E -> transformActive(+1);
                }
            }
        }
    };

    private GLFWMouseButtonCallback mbCallback = new GLFWMouseButtonCallback() {
        @Override
        public void invoke(long window, int button, int action, int mods) {
            // Levé tlačítko: rozhlížení kamerou
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                mousePressed = action == GLFW_PRESS;
            }

            // Pravé tlačítko (stisk): výběr aktivního objektu color pickingem
            if (button == GLFW_MOUSE_BUTTON_RIGHT && action == GLFW_PRESS) {
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);
                setActive(pickEntityAt(xBuffer.get(0), yBuffer.get(0)));
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
