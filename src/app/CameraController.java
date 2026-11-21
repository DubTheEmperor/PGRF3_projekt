package app;

import transforms.Camera;
import transforms.Mat4;
import transforms.Vec3D;

import java.util.Optional;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Kamera a její ovládání (bez GLFW callback objektů, renderer jen přeposílá události).
 * Dva režimy, přepínané při změně výběru (focusOn / release):
 * - bez výběru = volná kamera (1. osoba): myš rozhlíží kolem sebe, kolečko posouvá dopředu/dozadu,
 *   WASD posouvá (W/S svisle, A/D do stran)
 * - s výběrem = orbit (3. osoba) kolem vybraného objektu: myš obíhá, kolečko přibližuje,
 *   střed orbity sleduje objekt (update), WASD se ignoruje
 */
public class CameraController {
    private static final double MOVE_SPEED = 0.025;          // posun WASD za snímek
    private static final double ROTATE_SENSITIVITY = 0.2;   // stupně na pixel
    private static final double ZOOM_FACTOR = 0.8;          // orbit: násobek poloměru na jeden krok kolečka
    private static final double SCROLL_STEP = 0.2;          // volná kamera: posun na jeden krok kolečka
    private static final double MIN_RADIUS = 0.1;

    private Camera camera;
    private boolean rotating = false;
    private double lastX, lastY;
    private boolean keyW, keyA, keyS, keyD;

    public CameraController() {
        this(new Camera()
                .withPosition(new Vec3D(0, 0, 0))
                .withAzimuth(Math.toRadians(135))
                .withZenith(Math.toRadians(-45))
                .withRadius(5)
                .withFirstPerson(false));
        release();  // na začátku není nic vybráno => volná kamera ve stejném místě oka
    }

    public CameraController(Camera camera) {
        this.camera = camera;
    }

    // ======== Přepínání režimů (volá renderer při změně výběru) ========
    /** Orbit kolem cíle: kamera zůstane na místě a natočí se na cíl. */
    public void focusOn(Vec3D target) {
        Vec3D eye = camera.getEye();
        Vec3D direction = target.sub(eye);
        double distance = direction.length();

        double azimuth = camera.getAzimuth();
        double zenith = camera.getZenith();
        if (distance > 1e-6) {
            azimuth = Math.atan2(direction.getY(), direction.getX());
            zenith = Math.asin(direction.getZ() / distance);
        }

        camera = new Camera(target, azimuth, zenith, Math.max(distance, MIN_RADIUS), false);
    }

    /** Volná kamera: oko i směr pohledu zůstanou, otáčí se kolem sebe. */
    public void release() {
        camera = camera.withPosition(camera.getEye()).withFirstPerson(true);
    }

    // ======== Události vstupu ========
    public void onKey(int key, int action) {
        if (action == GLFW_REPEAT) return;  // stav klávesy se mění jen při PRESS / RELEASE
        boolean pressed = action == GLFW_PRESS;
        switch (key) {
            case GLFW_KEY_W -> keyW = pressed;
            case GLFW_KEY_A -> keyA = pressed;
            case GLFW_KEY_S -> keyS = pressed;
            case GLFW_KEY_D -> keyD = pressed;
        }
    }

    public void onMouseButton(int button, int action) {
        if (button == GLFW_MOUSE_BUTTON_LEFT) {
            rotating = action == GLFW_PRESS;
        }
    }

    public void onCursorPos(double x, double y) {
        if (rotating) {
            camera = camera.addAzimuth(Math.toRadians(lastX - x) * ROTATE_SENSITIVITY);
            camera = camera.addZenith(Math.toRadians(lastY - y) * ROTATE_SENSITIVITY);
        }
        lastX = x;
        lastY = y;
    }

    public void onScroll(double dy) {
        if (camera.getFirstPerson())
            camera = camera.forward(dy * SCROLL_STEP);
        else
            camera = camera.mulRadius(Math.pow(ZOOM_FACTOR, dy));
    }

    // ======== Aktualizace za snímek ========
    /**
     * @param target pozice vybraného objektu (orbit ji sleduje); prázdný => volná kamera, WASD
     */
    public void update(Optional<Vec3D> target) {
        if (target.isPresent()) {
            camera = camera.withPosition(target.get());
            return;
        }

        if (keyW) camera = camera.up(MOVE_SPEED);
        if (keyS) camera = camera.down(MOVE_SPEED);
        if (keyA) camera = camera.left(MOVE_SPEED);
        if (keyD) camera = camera.right(MOVE_SPEED);
    }

    public Mat4 getViewMatrix() {
        return camera.getViewMatrix();
    }
}
