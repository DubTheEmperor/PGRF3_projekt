package app;

import lwjglutils.OGLBuffers;
import lwjglutils.ShaderUtils;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;


public class Renderer extends AbstractRenderer {
    private OGLBuffers buffers;
    private int shaderProgram;

    public void init() {
        glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // vb
        float[] vb = {
           0f,  1f,
          -1f,  0f,
           1f, -1f
        };

        // ib
        int[] ib = {0, 1, 2};

        // popsat grafice, co je to za data
        OGLBuffers.Attrib[] attribs = {
          new OGLBuffers.Attrib("inPosition", 2)
        };

        // dát do dohromady
        buffers = new OGLBuffers(vb, attribs, ib);

        // shaderový program
        shaderProgram = ShaderUtils.loadProgram("/triangle");

        glUseProgram(shaderProgram);
    }

    public void display() {
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // vykreslit
        buffers.draw(GL_TRIANGLES, shaderProgram);
    }
}