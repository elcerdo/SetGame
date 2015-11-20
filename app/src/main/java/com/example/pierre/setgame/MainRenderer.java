package com.example.pierre.setgame;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer extends GestureDetector.SimpleOnGestureListener implements Renderer {
    private FloatBuffer verticesBufferSquare;

    private float verticesArraySquare[] = {
      -1,-1,0,1,
      1,-1,0,1,
      1,1,0,1,
      -1,1,0,1,
    };

    public int counter;
    private long startTime;

    private int tile_program;
    private int textures[];
    private Context context;

    public int width;
    public int height;
    private float sx;
    private float sy;

    private int cards[];

    public static FloatBuffer arrayToBuffer(float array[]) {
        ByteBuffer byte_buffer = ByteBuffer.allocateDirect(array.length * 4);
        byte_buffer.order(ByteOrder.nativeOrder());
        FloatBuffer float_buffer = byte_buffer.asFloatBuffer();
        float_buffer.put(array);
        float_buffer.position(0);
        return float_buffer;
    }

    public static int loadShader(int type, String code) {
        int shaderId = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shaderId, code);
        GLES20.glCompileShader(shaderId);
        int isCompiled[] = new int[1];
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, isCompiled, 0);
        if(isCompiled[0] == GLES20.GL_FALSE) {
            String error_string = "SHADER COMPILE ERROR " + GLES20.glGetShaderInfoLog(shaderId);
            Log.e("SetGame", error_string);
            throw new RuntimeException(error_string);
        }
        return shaderId;
    }

    public static int loadTexture(InputStream is, int texture_id) {
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture_id);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        return texture_id;
    }

    public static void assertStatus() {
        int error = GLES20.glGetError();
        if (error == GLES20.GL_NO_ERROR) return;
        String error_string = "OPENGL STATUS ERROR " + GLU.gluErrorString(error);
        Log.e("SetGame", error_string);
        throw new RuntimeException(error_string);
    }

    MainRenderer(Context context_) {
        Log.i("SetGame", "create renderer");
        counter = 2;
        width = height = -1;
        sx = .5f;
        sy = 0;
        verticesBufferSquare = arrayToBuffer(verticesArraySquare);
        startTime = System.currentTimeMillis();
        context = context_;
        cards = new int[9];
        Random rng = new Random();
        for (int ii=0; ii<cards.length; ii++)
            cards[ii] = rng.nextInt() % 81;
        cards[0] = 15;
        cards[1] = 77;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent evt) {
        counter ++;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent evt0, MotionEvent evt1, float dx, float dy) {
        assert( width > 0 && height > 0 );
        sx -= 2*dx/(float)Math.min(height, width);
        sy += 2*dy/(float)Math.min(height, width);
        return true;
    }

    @Override
    public void onDrawFrame(GL10 foo) {
        final float current_time = (System.currentTimeMillis() - startTime) / 1000f;
        final float omega = 2f*(float)Math.PI*.3f;

        GLES20.glClearColor((float)(1d+Math.cos(omega*current_time))/2f,(float)(1d+Math.cos(omega*current_time+2*Math.PI/3))/2f,(float)(1d+Math.cos(omega*current_time+4*Math.PI/3))/2f,1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glLineWidth(10f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        {
            GLES20.glUseProgram(tile_program);

            int position_attrib = GLES20.glGetAttribLocation(tile_program, "aPosition");
            int model_view_uniform = GLES20.glGetUniformLocation(tile_program, "uModelView");
            int time_uniform = GLES20.glGetUniformLocation(tile_program, "uTime");
            int tap_uniform = GLES20.glGetUniformLocation(tile_program, "uTap");
            int card_uniform = GLES20.glGetUniformLocation(tile_program, "uCard");
            int color_uniform = GLES20.glGetUniformLocation(tile_program, "uColor");

            GLES20.glUniform1f(time_uniform, current_time);
            GLES20.glUniform2f(tap_uniform, sx, sy);

            GLES20.glEnableVertexAttribArray(position_attrib);
            GLES20.glVertexAttribPointer(position_attrib, 4, GLES20.GL_FLOAT, false, 0, verticesBufferSquare);
            float model_view_matrix[] = new float[16];
            int index = 0;
            for (int ii=0; ii<3; ii++)
                for (int jj= 0; jj<3; jj++)
                {
                    Matrix.setIdentityM(model_view_matrix, 0);
                    //Matrix.translateM(model_view_matrix, 0, -width/(float)Math.min(height, width),-height/(float)Math.min(height, width),0);
                    Matrix.translateM(model_view_matrix, 0, -1,-1,0);
                    Matrix.scaleM(model_view_matrix, 0, 1/3f,1/3f,1);
                    Matrix.translateM(model_view_matrix, 0, 2*ii, 2*jj, 0);
                    GLES20.glUniformMatrix4fv(model_view_uniform, 1, false, model_view_matrix, 0);

                    int card = cards[index];
                    final int color = card%3; card /= 3;
                    final int filling = card%3; card/= 3;
                    final int number = card%3; card /= 3;
                    final int shape = card%3;

                    switch (color)
                    {
                        case 0:
                            GLES20.glUniform4f(color_uniform, 1,0,0,1);
                            break;
                        case 1:
                            GLES20.glUniform4f(color_uniform, 0,1,0,1);
                            break;
                        default:
                            GLES20.glUniform4f(color_uniform, 0,0,1,1);
                            break;
                    }

                    GLES20.glUniform2f(card_uniform, shape+3*number, filling);
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
                    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, verticesBufferSquare.capacity() / 4);

                    index++;
                }
            GLES20.glDisableVertexAttribArray(position_attrib);
        }

        assertStatus();
    }

    @Override
    public void onSurfaceCreated(GL10 foo, EGLConfig config) {
        Log.i("SetGame", "create surface");

        { // texture
            textures = new int[2];
            GLES20.glGenTextures(textures.length, textures, 0);
            loadTexture(context.getResources().openRawResource(R.drawable.plain), textures[0]);
            loadTexture(context.getResources().openRawResource(R.drawable.selected), textures[1]);
        }

        { // main shader
            int vertex_shader_id = loadShader(GLES20.GL_VERTEX_SHADER, context.getResources().getString(R.string.tile_vertex_shader));
            int fragment_shader_id = loadShader(GLES20.GL_FRAGMENT_SHADER, context.getResources().getString(R.string.tile_fragment_shader));
            tile_program = GLES20.glCreateProgram();
            GLES20.glAttachShader(tile_program, vertex_shader_id);
            GLES20.glAttachShader(tile_program, fragment_shader_id);
            GLES20.glLinkProgram(tile_program);
        }

        assertStatus();
    }
    @Override
    public void onSurfaceChanged(GL10 foo, int width_, int height_) {
        width = width_;
        height = height_;

        Log.i("SetGame", String.format("surface changed %d %d", width, height));
        GLES20.glViewport(0, 0, width, height);

        setProjection(tile_program, true);
    }

    public void setProjection(int program, boolean physical_referential) {
        GLES20.glUseProgram(program);

        int projection_uniform = GLES20.glGetUniformLocation(program, "uProjection");
        if (projection_uniform >= 0) {
            float projection_matrix[] = new float[16];
            if (physical_referential) {
                float mx = width / (float) Math.min(height, width);
                float my = height / (float) Math.min(height, width);
                Matrix.orthoM(projection_matrix, 0, -mx, mx, -my, my, -10, 10);
            }
            else Matrix.orthoM(projection_matrix, 0, 0, width, 0, height, -10, 10);
            assertStatus();
            GLES20.glUniformMatrix4fv(projection_uniform, 1, false, projection_matrix, 0);
        }

        int size_uniform = GLES20.glGetUniformLocation(program, "uSize");
        if (size_uniform >= 0) GLES20.glUniform2f(size_uniform, width, height);

        assertStatus();
    }
}
