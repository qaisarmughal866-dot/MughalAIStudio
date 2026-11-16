package com.mughalai.studio;

import android.graphics.Bitmap;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/** Off-screen OpenGL ES renderer for real lit 3D vegetable meshes. */
final class GLVegetableRenderer {
    private static final int W=1280, H=720;
    private EGLDisplay display;
    private EGLContext context;
    private EGLSurface surface;
    private int program, posLoc, normalLoc, mvpLoc, modelLoc, colorLoc, lightLoc;
    private FloatBuffer vertices, normals;
    private ShortBuffer indices;
    private int indexCount;
    private final float[] projection=new float[16], view=new float[16], vp=new float[16];
    private final float[] model=new float[16], mvp=new float[16];
    private final int[] pixels=new int[W*H];

    GLVegetableRenderer() throws Exception {
        display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        int[] version=new int[2];
        if(!EGL14.eglInitialize(display,version,0,version,1)) throw new Exception("OpenGL initialization failed");
        int[] attrs={EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,
                EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_DEPTH_SIZE,16,EGL14.EGL_NONE};
        EGLConfig[] configs=new EGLConfig[1]; int[] count=new int[1];
        if(!EGL14.eglChooseConfig(display,attrs,0,configs,0,1,count,0)||count[0]==0) throw new Exception("No OpenGL config");
        int[] ctxAttrs={EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE};
        context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,ctxAttrs,0);
        int[] surfAttrs={EGL14.EGL_WIDTH,W,EGL14.EGL_HEIGHT,H,EGL14.EGL_NONE};
        surface=EGL14.eglCreatePbufferSurface(display,configs[0],surfAttrs,0);
        if(context==null||surface==null||!EGL14.eglMakeCurrent(display,surface,surface,context)) throw new Exception("Could not create 3D surface");
        String vs="attribute vec3 aPosition; attribute vec3 aNormal; uniform mat4 uMvp; uniform mat4 uModel; varying vec3 vNormal; void main(){ gl_Position=uMvp*vec4(aPosition,1.0); vNormal=mat3(uModel)*aNormal; }";
        String fs="precision mediump float; uniform vec4 uColor; uniform vec3 uLight; varying vec3 vNormal; void main(){ float d=max(dot(normalize(vNormal),normalize(uLight)),0.0); float l=0.34+0.66*d; gl_FragColor=vec4(uColor.rgb*l,uColor.a); }";
        program=link(vs,fs);
        posLoc=GLES20.glGetAttribLocation(program,"aPosition");
        normalLoc=GLES20.glGetAttribLocation(program,"aNormal");
        mvpLoc=GLES20.glGetUniformLocation(program,"uMvp");
        modelLoc=GLES20.glGetUniformLocation(program,"uModel");
        colorLoc=GLES20.glGetUniformLocation(program,"uColor");
        lightLoc=GLES20.glGetUniformLocation(program,"uLight");
        makeSphere();
        Matrix.perspectiveM(projection,0,38f,(float)W/H,0.1f,100f);
        Matrix.setLookAtM(view,0,0f,0f,8.4f,0f,0f,0f,0f,1f,0f);
        Matrix.multiplyMM(vp,0,projection,0,view,0);
    }
    private int shader(int type,String code) throws Exception {
        int s=GLES20.glCreateShader(type); GLES20.glShaderSource(s,code); GLES20.glCompileShader(s);
        int[] ok=new int[1]; GLES20.glGetShaderiv(s,GLES20.GL_COMPILE_STATUS,ok,0);
        if(ok[0]==0) throw new Exception("3D shader compilation failed: "+GLES20.glGetShaderInfoLog(s));
        return s;
    }
    private int link(String v,String f) throws Exception {
        int p=GLES20.glCreateProgram(); GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,v));
        GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,f)); GLES20.glLinkProgram(p);
        int[] ok=new int[1]; GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0);
        if(ok[0]==0) throw new Exception("3D shader link failed"); return p;
    }
    private void makeSphere() {
        int rings=24, sectors=32; float[] v=new float[(rings+1)*(sectors+1)*3];
        float[] n=new float[v.length]; short[] ix=new short[rings*sectors*6]; int k=0;
        for(int r=0;r<=rings;r++){ double phi=Math.PI*r/rings;
            for(int s=0;s<=sectors;s++){ double theta=2*Math.PI*s/sectors;
                float x=(float)(Math.sin(phi)*Math.cos(theta)), y=(float)Math.cos(phi), z=(float)(Math.sin(phi)*Math.sin(theta));
                v[k]=x; n[k++]=x; v[k]=(float)y; n[k++]=(float)y; v[k]=(float)z; n[k++]=(float)z;
            }}
        k=0; for(int r=0;r<rings;r++) for(int s=0;s<sectors;s++){
            short a=(short)(r*(sectors+1)+s), b=(short)(a+sectors+1);
            ix[k++]=a;ix[k++]=b;ix[k++]=(short)(a+1);ix[k++]=(short)(a+1);ix[k++]=b;ix[k++]=(short)(b+1);
        }
        vertices=direct(v); normals=direct(n); indices=direct(ix); indexCount=ix.length;
    }
    private FloatBuffer direct(float[] a){FloatBuffer b=ByteBuffer.allocateDirect(a.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();b.put(a).position(0);return b;}
    private ShortBuffer direct(short[] a){ShortBuffer b=ByteBuffer.allocateDirect(a.length*2).order(ByteOrder.nativeOrder()).asShortBuffer();b.put(a).position(0);return b;}
    private void sphere(float x,float y,float z,float sx,float sy,float sz,float r,float g,float b) {
        Matrix.setIdentityM(model,0); Matrix.translateM(model,0,x,y,z); Matrix.scaleM(model,0,sx,sy,sz);
        Matrix.multiplyMM(mvp,0,vp,0,model,0);
        GLES20.glUniformMatrix4fv(mvpLoc,1,false,mvp,0); GLES20.glUniformMatrix4fv(modelLoc,1,false,model,0);
        GLES20.glUniform4f(colorLoc,r,g,b,1f);
        vertices.position(0); normals.position(0);
        GLES20.glVertexAttribPointer(posLoc,3,GLES20.GL_FLOAT,false,0,vertices); GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glVertexAttribPointer(normalLoc,3,GLES20.GL_FLOAT,false,0,normals); GLES20.glEnableVertexAttribArray(normalLoc);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES,indexCount,GLES20.GL_UNSIGNED_SHORT,indices);
    }
    Bitmap render(int frame) {
        EGL14.eglMakeCurrent(display,surface,surface,context);
        GLES20.glViewport(0,0,W,H); GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(0f,0f,0f,0f); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program); GLES20.glUniform3f(lightLoc,-0.4f,0.8f,1f);
        float t=frame*0.055f, bob=(float)Math.sin(t)*0.12f, step=(float)Math.sin(t*2)*0.16f;
        // Potato: sculpted ellipsoid body, tiny eyes, nose, smile, arms and feet.
        sphere(-1.45f,bob,0,0.78f,1.02f,0.62f,0.70f,0.43f,0.19f);
        sphere(-1.72f,0.25f,0.57f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(-1.18f,0.25f,0.57f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(-1.70f,0.25f,0.63f,0.04f,0.07f,0.035f,0.08f,0.05f,0.03f);
        sphere(-1.16f,0.25f,0.63f,0.04f,0.07f,0.035f,0.08f,0.05f,0.03f);
        sphere(-1.45f,-0.08f,0.62f,0.17f,0.07f,0.04f,0.22f,0.035f,0.025f);
        sphere(-2.18f,-0.05f,0,0.13f,0.48f,0.14f,0.48f,0.27f,0.12f);
        sphere(-0.72f,-0.05f,0,0.13f,0.48f,0.14f,0.48f,0.27f,0.12f);
        sphere(-1.70f,-1.05f+step,0,0.16f,0.42f,0.17f,0.32f,0.19f,0.09f);
        sphere(-1.20f,-1.05f-step,0,0.16f,0.42f,0.17f,0.32f,0.19f,0.09f);
        // Tomato: round, slightly lobed body, green calyx and stem.
        sphere(1.35f,-bob,0,0.91f,0.86f,0.72f,0.88f,0.12f,0.12f);
        sphere(1.12f,0.23f,0.66f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(1.61f,0.23f,0.66f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(1.14f,0.23f,0.72f,0.04f,0.07f,0.035f,0.05f,0.03f,0.02f);
        sphere(1.63f,0.23f,0.72f,0.04f,0.07f,0.035f,0.05f,0.03f,0.02f);
        sphere(1.37f,-0.10f,0.75f,0.17f,0.07f,0.04f,0.22f,0.025f,0.02f);
        sphere(1.35f,0.88f,0,0.16f,0.27f,0.15f,0.12f,0.43f,0.12f);
        for(int i=0;i<5;i++){ double a=i*Math.PI*2/5; sphere(1.35f+(float)Math.cos(a)*0.25f,0.84f+(float)Math.sin(a)*0.13f,0.08f,0.23f,0.07f,0.12f,0.16f,0.55f,0.13f); }
        sphere(0.62f,-0.05f,0,0.13f,0.45f,0.14f,0.18f,0.42f,0.12f);
        sphere(2.08f,-0.05f,0,0.13f,0.45f,0.14f,0.18f,0.42f,0.12f);
        sphere(1.08f,-0.92f-step,0,0.15f,0.4f,0.16f,0.12f,0.36f,0.10f);
        sphere(1.62f,-0.92f+step,0,0.15f,0.4f,0.16f,0.12f,0.36f,0.10f);
        ByteBuffer buf=ByteBuffer.allocateDirect(W*H*4).order(ByteOrder.nativeOrder());
        GLES20.glReadPixels(0,0,W,H,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,buf);
        buf.rewind();
        Bitmap raw=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888); raw.copyPixelsFromBuffer(buf);
        android.graphics.Matrix m=new android.graphics.Matrix(); m.setScale(1,-1);
        Bitmap result=Bitmap.createBitmap(raw,0,0,W,H,m,true); raw.recycle();
        return result;
    }
    void close(){
        if(display!=null){EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
            if(surface!=null)EGL14.eglDestroySurface(display,surface);
            if(context!=null)EGL14.eglDestroyContext(display,context);
            EGL14.eglTerminate(display);}
    }
}
