package app;

import app.objLoader.ObjLoader;
import app.object.Axis;
import app.object.Cube;
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
import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30C.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30C.GL_RG;
import static org.lwjgl.opengl.GL30C.glBindFramebuffer;


public class RendererColorPicking extends AbstractRenderer {
    // ======== Picking ID ========
    private static final int ID_NONE = 0;   // pozadí picking textury
    private static final int NO_SELECTION = -1;

    // Šedé pozadí obrazovky
    private static final float SCREEN_BACKGROUND = 0.1f;

    // ======== Světlo ========
    // Výchozí pozice světla ve světových souřadnicích
    private static final Vec3D LIGHT_POSITION = new Vec3D(-1.5, 1.5, 2.0);
    private static final Vec3D LIGHT_COLOR = new Vec3D(1.0, 0.9, 0.2);
    private static final double LIGHT_MARKER_SCALE = 0.1;

    // ======== Transformace aktivního objektu ========
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

    // ======== Debug režimy (uDebugMode v phong.frag, id musí odpovídat konstantám DEBUG_* ve shaderu) ========
    private static final int DEBUG_OFF = 0;

    private enum DebugMode {
        POSITION(1, "Pozice (svět xyz)"),
        DEPTH(2, "Hloubka (pohled)"),
        VERTEX_COLOR(3, "Barva vrcholu (bez stínování)"),
        TEXTURE(4, "Textura (bez stínování)"),
        NORMAL(5, "Normála (view space)"),
        TEX_COORD(6, "Texturovací souřadnice"),
        LIGHT_DISTANCE(7, "Vzdálenost od světla"),
        TANGENT(8, "Tečna (view space)"),
        DIFFUSE(9, "Difuzní člen N·L");

        private final int id;
        private final String label;

        DebugMode(int id, String label) {
            this.id = id;
            this.label = label;
        }

        // Další režim v pořadí
        private DebugMode next() {
            DebugMode[] modes = values();
            return modes[(ordinal() + 1) % modes.length];
        }
    }

    private OGLBuffers buffersAxis;
    private final List<SceneEntity> entities = new ArrayList<>();
    private SceneEntity lightEntity;
    private int activeIndex = NO_SELECTION;

    private TransformMode transformMode = TransformMode.TRANSLATE;
    private Vec3D transformAxis = new Vec3D(1, 0, 0);
    private String transformAxisName = "X";

    private boolean debugEnabled = false;
    private DebugMode debugMode = DebugMode.POSITION;

    private int shaderProgramPhong, shaderProgramAxis, shaderProgramColorPicking;
    private OGLTexture2D textureBrickwall, textureBrickwallNormal, textureGold, textureD20, textureElephant, textureElephantNormal;

    private final CameraController cameraController = new CameraController();
    private Mat4PerspRH proj;

    private OGLRenderTarget renderTarget;
    private OGLTexture2D.Viewer viewer;

    public void init() {
        renderTarget = new OGLRenderTarget(width, height);
        viewer = new OGLTexture2D.Viewer();
        textRenderer = new OGLTextRenderer(width, height);

        // ======== Projekce (kamera je v CameraController) ========
        proj = createProjection();

        // ======== Textury ========
        try {
            textureBrickwall = new OGLTexture2D("./textures/brickwall.jpg");
            textureBrickwallNormal = new OGLTexture2D("./textures/brickwall_normal.jpg");
            textureGold = new OGLTexture2D("./textures/gold.jpg");
            textureD20 = new OGLTexture2D("./textures/d20.png");
            textureElephant = new OGLTexture2D("textures/marble.jpg");
            textureElephantNormal = new OGLTexture2D("textures/marble_normal.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        enableRepeat(textureElephant);
        enableRepeat(textureElephantNormal);

        // ======== Scene objects ========
        // Pořadí v seznamu určuje picking ID i přepínání šipkami
        buffersAxis = new Axis().getBuffers();

        entities.add(new SceneEntity("Elephant", ObjLoader.loadWithComputedNormals("obj/elephantBody.obj"),
                new Mat4Transl(9.545, -0.434, -40.658).mul(new Mat4Scale(0.01)).mul(new Mat4RotX(Math.toRadians(180))).mul(new Mat4Transl(-0.25, 0, 0)),
                textureElephant, textureElephantNormal));
        entities.add(new SceneEntity("Cube", new Cube().getBuffers(),
                new Mat4Scale(0.5).mul(new Mat4Transl(0.8, 1.6, 0.25)), textureBrickwall, textureBrickwallNormal));
        entities.add(new SceneEntity("Pyramid", new Pyramid().getBuffers(),
                new Mat4Scale(0.6).mul(new Mat4Transl(0.8, -1.6, 0)), textureGold));
        entities.add(new SceneEntity("Icosahedron", new Icosahedron().getBuffers(),
                new Mat4Scale(0.35).mul(new Mat4Transl(-1.2, -1.0, 0.30)), textureD20));

        // Světlo
        lightEntity = SceneEntity.createLight("Light", new Icosahedron().getBuffers(),
                new Mat4Scale(LIGHT_MARKER_SCALE).mul(new Mat4Transl(LIGHT_POSITION)));
        entities.add(lightEntity);

        // ======== Shaderové programy ========
        shaderProgramPhong = ShaderUtils.loadProgram("/phong");
        shaderProgramAxis = ShaderUtils.loadProgram("/axis");
        shaderProgramColorPicking = ShaderUtils.loadProgram("/colorPicking");
    }

    public void display() {
        resizeRenderTargetIfNeeded();
        cameraController.update(activeIndex == NO_SELECTION
                ? Optional.empty()
                : Optional.of(entities.get(activeIndex).getCenter()));
        glEnable(GL_DEPTH_TEST);

        // ======== 1. průchod: ID barvy do textury (color picking) ========
        renderTarget.bind();
        glViewport(0, 0, width, height);
        glClearColor(ID_NONE, .0f, .0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramColorPicking);

        // ======== 2. průchod: Phong na obrazovku ========
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, width, height);
        glClearColor(SCREEN_BACKGROUND, SCREEN_BACKGROUND, SCREEN_BACKGROUND, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderScene(shaderProgramPhong);
        renderAxis(); // osy se vykreslí jen na obrazovku

        glDisable(GL_DEPTH_TEST);
        viewer.view(renderTarget.getColorTexture(), -1, -1, 0.5);

        // ======== Text UI ========
        String activeName = activeIndex == NO_SELECTION ? "žádný" : entities.get(activeIndex).getName();
        textRenderer.addStr2D(3, 20, "Aktivní objekt: " + activeName);
        textRenderer.addStr2D(3, 40, "Osa: " + transformAxisName + " (1/2/3) | Režim: " + transformMode.label
                + " (4/5/6) | Q/E: −/+ | Backspace: reset");
        textRenderer.addStr2D(3, 60, debugStatus());
    }

    private void renderAxis() {
        glUseProgram(shaderProgramAxis);
        setGlobalUniforms(shaderProgramAxis);
        buffersAxis.draw(GL_LINES, shaderProgramAxis);
    }

    // Kreslí jen objekty, které jde vybrat
    private void renderScene(int shaderProgram) {
        for (int i = 0; i < entities.size(); i++) {
            renderObject(shaderProgram, entities.get(i), pickingIdFromIndex(i), i == activeIndex);
        }
    }

    private void renderObject(int shaderProgram, SceneEntity entity, int pickingId, boolean active) {
        boolean unlit = entity.isLight() && shaderProgram == shaderProgramPhong;
        int program = unlit ? shaderProgramColorPicking : shaderProgram;

        glUseProgram(program);
        setGlobalUniforms(program);

        int locUModel = glGetUniformLocation(program, "uModel");
        glUniformMatrix4fv(locUModel, false, entity.getModel().floatArray());

        if (unlit) {
            int locUColor = glGetUniformLocation(program, "uColor");
            glUniform3f(locUColor, (float) LIGHT_COLOR.getX(), (float) LIGHT_COLOR.getY(), (float) LIGHT_COLOR.getZ());
        } else {
            setPickingColor(program, pickingId, active);
        }

        int locUUseNormalMap = glGetUniformLocation(program, "uUseNormalMap");
        glUniform1i(locUUseNormalMap, entity.isNormalMapped() ? 1 : 0);

        bindTextures(program, entity);
        entity.getBuffers().draw(GL_TRIANGLES, program);
    }

    // Textury entity
    private void bindTextures(int shaderProgram, SceneEntity entity) {
        if (entity.getTextureColor() != null)
            entity.getTextureColor().bind(shaderProgram, "textureColor", 0);
        if (entity.getTextureNormal() != null)
            entity.getTextureNormal().bind(shaderProgram, "textureNormal", 1);
    }

    private void enableRepeat(OGLTexture2D texture) {
        texture.bind();
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    }

    // ID pro color picking: neaktivní objekt červeně, aktivní zeleně (Phong uniform uColor ignoruje)
    private void setPickingColor(int shaderProgram, int pickingId, boolean active) {
        float id = pickingId / pickingScale();
        int locUColor = glGetUniformLocation(shaderProgram, "uColor");
        if (active)
            glUniform3f(locUColor, .0f, id, .0f);
        else
            glUniform3f(locUColor, id, .0f, .0f);
    }

    private void setGlobalUniforms(int shaderProgram) {
        // View matice - posíláme do shaderu jako uniform
        int locUView = glGetUniformLocation(shaderProgram, "uView");
        glUniformMatrix4fv(locUView, false, cameraController.getViewMatrix().floatArray());
        // Proj matice - posíláme do shaderu jako uniform
        int locUProj = glGetUniformLocation(shaderProgram, "uProj");
        glUniformMatrix4fv(locUProj, false, proj.floatArray());
        // Pozice světla (programy bez uLightPos dostanou lokaci -1 a OpenGL volání ignoruje)
        Vec3D lightPosition = getLightPosition();
        int locULightPos = glGetUniformLocation(shaderProgram, "uLightPos");
        glUniform3f(locULightPos, (float) lightPosition.getX(), (float) lightPosition.getY(), (float) lightPosition.getZ());
        // Debug režim (jen phong; ostatní programy uniform nemají)
        int locUDebugMode = glGetUniformLocation(shaderProgram, "uDebugMode");
        glUniform1i(locUDebugMode, debugEnabled ? debugMode.id : DEBUG_OFF);
    }

    // Pozice světla = počátek značky světla transformovaný její modelovou maticí
    private Vec3D getLightPosition() {
        return lightEntity.getCenter();
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
        return index + 1;
    }

    private int indexFromPickingId(int pickingId) {
        int index = pickingId - 1;
        return index >= 0 && index < entities.size() ? index : NO_SELECTION;
    }

    // Společné měřítko kódování i dekódování ID do barvy (ID / počet objektů)
    private float pickingScale() {
        return entities.size();
    }

    // Jediné místo změny výběru => zde se přepíná i režim kamery
    private void setActive(int index) {
        if (index == activeIndex) return;
        activeIndex = index;

        if (index == NO_SELECTION)
            cameraController.release();     // volná kamera (rozhlížení kolem sebe)
        else
            cameraController.focusOn(entities.get(index).getCenter());  // orbit kolem objektu
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
        if (entity.isLight() && transformMode != TransformMode.TRANSLATE) return;  // světlo jde jen posouvat
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

    // ====== Debug režimy ======
    private void nextDebugMode() {
        if (debugEnabled) debugMode = debugMode.next();
    }

    private String debugStatus() {
        return debugEnabled
                ? "Debug: " + debugMode.label + " (; vypnout | Tab: další režim)"
                : "Debug: vypnuto (; zapnout)";
    }

    // ====== Picking ======
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

        // Nenulový je vždy jen jeden kanál (R = neaktivní, G = aktivní) => součet je zakódované ID
        int pickingId = Math.round((pixel.get(0) + pixel.get(1)) * pickingScale());
        return indexFromPickingId(pickingId);
    }

    // ====== Callbacks (kamera je v CameraController) ======
    private GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            cameraController.onKey(key, action);

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
                    case GLFW_KEY_GRAVE_ACCENT -> debugEnabled = !debugEnabled;
                    case GLFW_KEY_TAB -> nextDebugMode();
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
            cameraController.onMouseButton(button, action);   // levé tlačítko: orbit kamery

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
            cameraController.onCursorPos(x, y);
        }
    };

    private GLFWScrollCallback scrollCallback = new GLFWScrollCallback() {
        @Override
        public void invoke(long window, double dx, double dy) {
            cameraController.onScroll(dy);
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
