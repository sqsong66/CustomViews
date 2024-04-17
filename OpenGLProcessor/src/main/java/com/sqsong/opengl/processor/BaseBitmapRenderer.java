package com.sqsong.opengl.processor;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLContext;

/**
 * Created by yuxfzju on 16/8/10.
 */

public abstract class BaseBitmapRenderer implements IRenderer<Bitmap> {
    private final static String TAG = BaseBitmapRenderer.class.getSimpleName();

    private static final String vertexShaderCode =
            "attribute vec2 aTexCoord;   \n" +
                    "attribute vec4 aPosition;  \n" +
                    "varying vec2 vTexCoord;  \n" +
                    "void main() {              \n" +
                    "  gl_Position = aPosition; \n" +
                    "  vTexCoord = aTexCoord; \n" +
                    "}  \n";

    protected static final int COORDS_PER_VERTEX = 3;
    protected static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;

    private static final float[] squareCoords = {
            -1f, 1f, 0.0f,   // top left
            -1f, -1f, 0.0f,   // bottom left
            1f, -1f, 0.0f,   // bottom right
            1f, 1f, 0.0f    // top right
    };

    private static final float[] mTexHorizontalCoords = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f,
    };

    protected static final short[] drawOrder = {0, 1, 2, 0, 2, 3};

    protected final FloatBuffer mVertexBuffer;
    protected final ShortBuffer mDrawListBuffer;
    protected final FloatBuffer mTexCoordBuffer;

    protected Program mProgram;

    protected volatile boolean mNeedRelink;

    public BaseBitmapRenderer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(squareCoords);
        mVertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawListBuffer = dlb.asShortBuffer();
        mDrawListBuffer.put(drawOrder);
        mDrawListBuffer.position(0);

        ByteBuffer tcb = ByteBuffer.allocateDirect(mTexHorizontalCoords.length * 4);
        tcb.order(ByteOrder.nativeOrder());
        mTexCoordBuffer = tcb.asFloatBuffer();
        mTexCoordBuffer.put(mTexHorizontalCoords);
        mTexCoordBuffer.position(0);
    }

    @Override
    public void onDrawFrame(Bitmap bitmap, Boolean offScreen) {
        if (bitmap == null || bitmap.isRecycled()) {
            return;
        }
        if (bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            return;
        }
        BlurContext blurContext = null;
        try {
            blurContext = prepare(bitmap, offScreen);
            draw(blurContext);
        } finally {
            onPostBlur(blurContext);
        }
    }

    abstract String getFragmentShaderCode();

    private BlurContext prepare(Bitmap bitmap, Boolean offScreen) {
        EGLContext context = ((EGL10) EGLContext.getEGL()).eglGetCurrentContext();
        if (context.equals(EGL10.EGL_NO_CONTEXT)) {
            throw new IllegalStateException("This thread has no EGLContext.");
        }
        if (mNeedRelink || mProgram == null) {
            deletePrograms();
            mProgram = Program.of(vertexShaderCode, getFragmentShaderCode());
            mNeedRelink = false;
        }
        if (mProgram.id() == 0) {
            throw new IllegalStateException("Failed to create program.");
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(0, 0, w, h);
        return new BlurContext(bitmap, offScreen);
    }

    public abstract void draw(BlurContext blurContext);

    protected void resetAllBuffer() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glUseProgram(0);
        mVertexBuffer.rewind();
        mTexCoordBuffer.rewind();
        mDrawListBuffer.rewind();
    }

    private void onPostBlur(BlurContext blurContext) {
        if (blurContext != null) {
            blurContext.finish();
        }
        mVertexBuffer.clear();
        mTexCoordBuffer.clear();
        mDrawListBuffer.clear();
        deletePrograms();
    }


    protected void deletePrograms() {
        mNeedRelink = true;
        if (mProgram != null) {
            mProgram.delete();
            mProgram = null;
        }
    }

    public static class BlurContext {
        private final Texture inputTexture;
        private Texture horizontalTexture;
        private FrameBuffer blurFrameBuffer;
        private final Bitmap bitmap;

        private BlurContext(Bitmap bitmap, Boolean offScreen) {
            //todo Textures share problem is not solved. Here create a new texture directly, not get from the texture cache
            //It doesn't affect performance seriously.
            this.bitmap = bitmap;
            inputTexture = Texture.create(bitmap);
            if (offScreen) {
                horizontalTexture = Texture.create(bitmap.getWidth(), bitmap.getHeight());
                blurFrameBuffer = FrameBufferCache.getInstance().getFrameBuffer();
                if (blurFrameBuffer != null) {
                    blurFrameBuffer.bindTexture(horizontalTexture);
                } else {
                    throw new IllegalStateException("Failed to create framebuffer.");
                }
            }
        }

        protected Texture getInputTexture() {
            return inputTexture;
        }

        protected Texture getHorizontalTexture() {
            return horizontalTexture;
        }

        protected FrameBuffer getBlurFrameBuffer() {
            return blurFrameBuffer;
        }

        protected Bitmap getBitmap() {
            return bitmap;
        }

        private void finish() {
            if (inputTexture != null) {
                inputTexture.delete();
            }
            if (horizontalTexture != null) {
                horizontalTexture.delete();
            }
            FrameBufferCache.getInstance().recycleFrameBuffer(blurFrameBuffer);
        }
    }

}
