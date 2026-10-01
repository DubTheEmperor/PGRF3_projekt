package app;

import lwjglutils.OGLBuffers;
import lwjglutils.OGLTexture2D;
import lwjglutils.ShaderUtils;
import org.lwjgl.glfw.GLFWKeyCallback;
import transforms.Mat4;
import transforms.Mat4RotZ;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;


public class Renderer extends AbstractRenderer {
    private OGLBuffers buffers;
    private int shaderProgram;
    private OGLTexture2D texture;

    private float rotZ = 0;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // Textures
        try {
            texture = new OGLTexture2D("./textures/wood.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // vb
        float[] vb = {
        // 1. vrchol
           0f,  1f, 1,     1, 0, 0,     0.5f, 1,
        // 2. vrchol
          -1f,  0f, 1,     0, 1, 0,     0, 0.5f,
        // 3. vrchol
           1f, -1f, 1,     0, 0, 1,     1, 0
        };

        // ib
        int[] ib = {0, 1, 2};

        // popsat grafice, co je to za data
        OGLBuffers.Attrib[] attribs = {
          new OGLBuffers.Attrib("inPosition", 3),
          new OGLBuffers.Attrib("inColor", 3),
          new OGLBuffers.Attrib("inTexCoord", 2)
        };

        // dát do dohromady
        buffers = new OGLBuffers(vb, attribs, ib);

        // shaderový program
        shaderProgram = ShaderUtils.loadProgram("/triangle");

        glUseProgram(shaderProgram);

        texture.bind(shaderProgram, "textureWood");
    }

    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        rotZ += 0.001f;

        int locUModel = glGetUniformLocation(shaderProgram, "uModel");
        Mat4 model = new Mat4RotZ(Math.toDegrees(rotZ));
        glUniformMatrix4fv(locUModel, false, model.floatArray());

        // vykreslit
        buffers.draw(GL_TRIANGLES, shaderProgram);
    }



    protected GLFWKeyCallback keyCallback = new GLFWKeyCallback() {
        @Override
        public void invoke(long window, int key, int scancode, int action, int mods) {
            if (action == GLFW_PRESS && key == GLFW_KEY_L){
                int locUColor = glGetUniformLocation(shaderProgram, "uColor");
                glUniform3f(locUColor, 0, 0, 1);
            }
        }
    };

    @Override
    public GLFWKeyCallback getKeyCallback() {
        return keyCallback;
    }


}