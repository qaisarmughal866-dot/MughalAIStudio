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
    Bitmap render(int frame, String sceneText, String fullStory) {
        EGL14.eglMakeCurrent(display,surface,surface,context);
        GLES20.glViewport(0,0,W,H); GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClearColor(0f,0f,0f,0f); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glUseProgram(program); GLES20.glUniform3f(lightLoc,-0.4f,0.8f,1f);
        float t=frame*0.055f, bob=(float)Math.sin(t*1.7f)*0.10f;
        float talk=(float)(0.5+0.5*Math.sin(t*7.0));
        float greet=(float)Math.sin(t*1.8f);
        float sway=(float)Math.sin(t*0.85f)*0.10f;
        String scene = (sceneText == null ? "" : sceneText).toLowerCase(java.util.Locale.ROOT);
        String whole = (fullStory == null ? "" : fullStory).toLowerCase(java.util.Locale.ROOT);
        boolean vegetable = hasAny(whole,"آلو","الو","ٹماٹر","potato","tomato");
        boolean forest = hasAny(scene,"جنگل","درخت","forest","tree");
        boolean sea = hasAny(scene,"سمندر","دریا","کشتی","sea","ocean");
        boolean desert = hasAny(scene,"صحرا","ریگ","desert");
        boolean night = hasAny(scene,"رات","چاند","night","moon");
        boolean home = hasAny(scene,"گھر","مکان","کمرہ","house","home","room");
        boolean village = hasAny(scene,"گاؤں","گاؤں","کھیت","village","farm");
        // Story-dependent 3D environment: floor, sky landmark and set pieces.
        sphere(0f,-2.25f,-1.25f,5.2f,0.22f,1.1f,
                sea ? 0.08f : desert ? 0.68f : forest || village ? 0.18f : 0.32f,
                sea ? 0.42f : desert ? 0.48f : forest || village ? 0.40f : 0.48f,
                sea ? 0.72f : desert ? 0.25f : forest || village ? 0.16f : 0.25f);
        sphere(-3.55f,2.35f,-2.5f,0.48f,0.48f,0.15f,
                night?0.82f:1f,night?0.84f:0.82f,night?0.65f:0.25f);
        if (forest) {
            for(int i=0;i<7;i++) {
                float x=-3.1f+i*1.03f;
                sphere(x,-0.72f,-1.65f,0.10f,0.92f,0.12f,0.30f,0.17f,0.08f);
                sphere(x,0.25f,-1.7f,0.52f,0.72f,0.48f,0.10f,0.38f,0.13f);
                sphere(x-0.18f,0.52f,-1.68f,0.38f,0.45f,0.4f,0.14f,0.48f,0.16f);
            }
        } else if (home || village) {
            sphere(-2.35f,-0.55f,-1.7f,0.85f,0.85f,0.42f,0.86f,0.72f,0.52f);
            sphere(-2.35f,0.42f,-1.7f,0.98f,0.30f,0.48f,0.55f,0.16f,0.10f);
            sphere(-2.35f,-1.2f,-1.18f,0.20f,0.45f,0.10f,0.30f,0.16f,0.08f);
        } else if (sea) {
            for(int i=0;i<6;i++) sphere(-2.8f+i*1.1f,-1.45f,-1.0f,0.48f,0.055f,0.12f,0.45f,0.78f,0.88f);
        } else if (desert) {
            sphere(2.55f,-1.55f,-1.7f,1.05f,0.42f,0.45f,0.76f,0.55f,0.29f);
        }
        // Keep the fruit-market set only for stories that actually mention produce.
        if (vegetable) {
        // 3D market set: rounded awning, posts, counter, baskets, trees and produce.
        sphere(0f,-2.25f,-0.95f,5.2f,0.22f,1.1f,0.30f,0.48f,0.25f);
        sphere(0f,1.95f,-1.15f,2.65f,0.22f,0.55f,0.88f,0.48f,0.12f);
        sphere(-2.25f,1.50f,-1.05f,0.12f,1.45f,0.14f,0.48f,0.25f,0.12f);
        sphere(2.25f,1.50f,-1.05f,0.12f,1.45f,0.14f,0.48f,0.25f,0.12f);
        sphere(0f,0.78f,-1.0f,2.25f,0.12f,0.48f,0.48f,0.25f,0.12f);
        sphere(-2.85f,-0.35f,-1.3f,0.52f,0.58f,0.42f,0.28f,0.48f,0.20f);
        sphere(2.85f,-0.35f,-1.3f,0.52f,0.58f,0.42f,0.28f,0.48f,0.20f);
        sphere(-2.85f,0.25f,-1.3f,0.38f,0.38f,0.38f,0.16f,0.42f,0.16f);
        sphere(2.85f,0.25f,-1.3f,0.38f,0.38f,0.38f,0.16f,0.42f,0.16f);
        sphere(-1.95f,-1.45f,-0.8f,0.30f,0.20f,0.22f,0.92f,0.18f,0.10f);
        sphere(-1.48f,-1.42f,-0.8f,0.27f,0.18f,0.20f,0.95f,0.72f,0.12f);
        sphere(1.90f,-1.43f,-0.8f,0.28f,0.18f,0.20f,0.95f,0.25f,0.12f);
        // Potato: rounded 3D body, expressive face and conversational gestures.
        sphere(-1.45f+sway,bob,0,0.78f,1.02f,0.62f,0.70f,0.43f,0.19f);
        sphere(-1.72f,0.25f,0.57f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(-1.18f,0.25f,0.57f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(-1.70f,0.25f,0.63f,0.04f,0.07f,0.035f,0.08f,0.05f,0.03f);
        sphere(-1.16f,0.25f,0.63f,0.04f,0.07f,0.035f,0.08f,0.05f,0.03f);
        sphere(-1.45f+sway,-0.08f,0.62f,0.16f,0.035f+talk*0.12f,0.04f,0.22f,0.035f,0.025f);
        sphere(-2.18f,-0.05f+greet*0.32f,0,0.13f,0.48f,0.14f,0.48f,0.27f,0.12f);
        sphere(-0.72f,-0.05f+Math.max(0f,greet)*0.38f,0,0.13f,0.48f,0.14f,0.48f,0.27f,0.12f);
        sphere(-1.70f,-1.05f,0,0.16f,0.42f,0.17f,0.32f,0.19f,0.09f);
        sphere(-1.20f,-1.05f,0,0.16f,0.42f,0.17f,0.32f,0.19f,0.09f);
        // Tomato: round, slightly lobed body, green calyx and stem.
        sphere(1.35f-sway,-bob,0,0.91f,0.86f,0.72f,0.88f,0.12f,0.12f);
        sphere(1.12f,0.23f,0.66f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(1.61f,0.23f,0.66f,0.09f,0.13f,0.06f,1f,1f,1f);
        sphere(1.14f,0.23f,0.72f,0.04f,0.07f,0.035f,0.05f,0.03f,0.02f);
        sphere(1.63f,0.23f,0.72f,0.04f,0.07f,0.035f,0.05f,0.03f,0.02f);
        sphere(1.37f-sway,-0.10f,0.75f,0.16f,0.035f+talk*0.12f,0.04f,0.22f,0.025f,0.02f);
        sphere(1.35f,0.88f,0,0.16f,0.27f,0.15f,0.12f,0.43f,0.12f);
        for(int i=0;i<5;i++){ double a=i*Math.PI*2/5; sphere(1.35f+(float)Math.cos(a)*0.25f,0.84f+(float)Math.sin(a)*0.13f,0.08f,0.23f,0.07f,0.12f,0.16f,0.55f,0.13f); }
        sphere(0.62f,-0.05f+Math.max(0f,-greet)*0.38f,0,0.13f,0.45f,0.14f,0.18f,0.42f,0.12f);
        sphere(2.08f,-0.05f+greet*0.32f,0,0.13f,0.45f,0.14f,0.18f,0.42f,0.12f);
        sphere(1.08f,-0.92f,0,0.15f,0.4f,0.16f,0.12f,0.36f,0.10f);
        sphere(1.62f,-0.92f,0,0.15f,0.4f,0.16f,0.12f,0.36f,0.10f);
        // A small 3D fruit stand and floating produce anchor the conversation in a market scene.
        sphere(-0.05f,-1.62f,-0.65f,1.55f,0.12f,0.34f,0.38f,0.20f,0.10f);
        sphere(-0.72f,-1.35f,-0.55f,0.20f,0.20f,0.20f,0.95f,0.16f,0.10f);
        sphere(-0.22f,-1.34f,-0.55f,0.20f,0.20f,0.20f,0.95f,0.72f,0.12f);
        sphere(0.28f,-1.35f,-0.55f,0.20f,0.20f,0.20f,0.28f,0.72f,0.20f);
        }
        if (!vegetable) {
            float phase = frame * 0.13f;
            float actorX = hasAny(whole,"دو","two","دوست","friends") ? -0.95f : 0f;
            boolean human = hasAny(whole,"لڑکا","لڑکی","بچہ","بچی","آدمی","عورت","بادشاہ","شہزادی","انسان","boy","girl","child","king","princess","man","woman","farmer");
            boolean lion = hasAny(whole,"شیر","lion");
            boolean rabbit = hasAny(whole,"خرگوش","rabbit");
            boolean bird = hasAny(whole,"پرند","چڑیا","کبوتر","bird","sparrow","pigeon");
            boolean dog = hasAny(whole,"کتا","کتے","dog","puppy");
            boolean snake = hasAny(whole,"سانپ","snake");
            boolean fish = hasAny(whole,"مچھلی","مچھلیاں","fish");
            boolean run = hasAny(whole,"دوڑ","بھاگ","race","run","running");
            boolean fly = hasAny(whole,"اڑ","پرواز","fly","flying");
            boolean isTalking = hasAny(whole,"کہا","بول","بات","کہتا","کہتی","said","speak","talk");
            boolean wave = hasAny(whole,"سلام","ہاتھ ہلایا","خوش آمدید","welcome","wave","waved");
            float gait = run ? 0.8f : 0.38f;
            if (human || (!lion && !rabbit && !bird && !dog && !snake && !fish)) {
                drawHumanoid(actorX, -0.35f, phase, gait, isTalking, wave, 0.20f,0.43f,0.78f);
                if (hasAny(whole,"دو","two","دوست","friends")) drawHumanoid(1.55f,-0.35f,phase+1.4f,gait,isTalking,false,0.72f,0.28f,0.22f);
            }
            if (lion) drawQuadruped(-0.25f,-0.55f,phase,gait,0.78f,0.46f,0.12f,true);
            if (rabbit) drawQuadruped(0.25f,-0.62f,phase,gait,0.83f,0.80f,0.70f,false);
            if (dog) drawQuadruped(0.30f,-0.72f,phase,gait,0.52f,0.29f,0.12f,false);
            if (bird) drawBird(0.25f,0.10f,phase,0.32f,0.62f,0.28f);
            if (snake) drawSnake(0.35f,-1.15f,phase);
            if (fish) drawFish(0.25f,-0.15f,phase);
        }
                ByteBuffer buf=ByteBuffer.allocateDirect(W*H*4).order(ByteOrder.nativeOrder());
        GLES20.glFinish();
        GLES20.glReadPixels(0,0,W,H,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,buf);
        buf.rewind();
        int[] argb=new int[W*H];
        for(int y=0;y<H;y++) {
            int dstY=H-1-y;
            for(int x=0;x<W;x++) {
                int r=buf.get()&255, g=buf.get()&255, b=buf.get()&255, a=buf.get()&255;
                argb[dstY*W+x]=(a<<24)|(r<<16)|(g<<8)|b;
            }
        }
        Bitmap result=Bitmap.createBitmap(W,H,Bitmap.Config.ARGB_8888);
        result.setPixels(argb,0,W,0,0,W,H);
        return result;
    }

    private static boolean hasAny(String s,String... words) {
        for(String w:words) if(s.contains(w)) return true;
        return false;
    }
    private void drawHumanoid(float x,float y,float t,float gait,boolean talk,boolean wave,float r,float g,float b) {
        float swing=(float)Math.sin(t)*gait;
        sphere(x,y,0,0.34f,0.52f,0.23f,r,g,b);
        sphere(x,y+0.78f,0.02f,0.27f,0.29f,0.23f,0.78f,0.56f,0.40f);
        sphere(x-0.09f,y+0.83f,0.23f,0.035f,0.05f,0.025f,0.03f,0.02f,0.02f);
        sphere(x+0.09f,y+0.83f,0.23f,0.035f,0.05f,0.025f,0.03f,0.02f,0.02f);
        sphere(x,y+0.68f,0.255f,0.08f,talk?0.045f+0.025f*(float)Math.abs(Math.sin(t*5)):0.025f,0.025f,0.25f,0.03f,0.02f);
        sphere(x-0.43f,y+0.02f+(wave?0.38f*(float)Math.abs(Math.sin(t*2)):swing*0.2f),0,0.11f,0.42f,0.11f,r*0.8f,g*0.8f,b*0.8f);
        sphere(x+0.43f,y+0.02f-swing*0.2f,0,0.11f,0.42f,0.11f,r*0.8f,g*0.8f,b*0.8f);
        sphere(x-0.16f,y-0.62f+(float)Math.sin(t)*0.12f,0,0.12f,0.40f,0.13f,0.12f,0.10f,0.09f);
        sphere(x+0.16f,y-0.62f-(float)Math.sin(t)*0.12f,0,0.12f,0.40f,0.13f,0.12f,0.10f,0.09f);
    }
    private void drawQuadruped(float x,float y,float t,float gait,float r,float g,float b,boolean mane) {
        float step=(float)Math.sin(t)*gait;
        sphere(x,y,0,0.72f,0.40f,0.34f,r,g,b);
        sphere(x+0.55f,y+0.34f,0.02f,0.35f,0.36f,0.30f,r,g,b);
        if(mane) sphere(x+0.53f,y+0.34f,-0.02f,0.43f,0.45f,0.34f,0.52f,0.25f,0.06f);
        sphere(x+0.70f,y+0.42f,0.29f,0.045f,0.055f,0.03f,0.02f,0.02f,0.02f);
        sphere(x+0.84f,y+0.18f,0.22f,0.13f,0.09f,0.08f,0.18f,0.08f,0.04f);
        for(int i=0;i<4;i++) sphere(x-0.48f+(i%2)*0.78f,y-0.45f+(i<2?step:-step)*0.12f,0,0.11f,0.35f,0.12f,r*0.8f,g*0.8f,b*0.8f);
        sphere(x-0.78f,y+0.05f,0,0.35f,0.07f,0.08f,r,g,b);
    }
    private void drawBird(float x,float y,float t,float r,float g,float b) {
        sphere(x,y,0,0.42f,0.30f,0.25f,r,g,b);
        float flap=(float)Math.sin(t*3)*0.24f;
        sphere(x-0.12f,y+flap,0.22f,0.30f,0.08f,0.06f,r*0.8f,g*0.8f,b*0.8f);
        sphere(x+0.30f,y+0.28f,0.03f,0.22f,0.22f,0.19f,r,g,b);
        sphere(x+0.49f,y+0.23f,0.18f,0.13f,0.06f,0.07f,0.95f,0.52f,0.08f);
        sphere(x+0.34f,y+0.31f,0.20f,0.035f,0.04f,0.03f,0.02f,0.02f,0.02f);
    }
    private void drawSnake(float x,float y,float t) {
        for(int i=0;i<7;i++) sphere(x-0.65f+i*0.20f,y+(float)Math.sin(t+i*0.7f)*0.10f,0,0.19f,0.13f,0.16f,0.20f,0.55f,0.16f);
        sphere(x+0.72f,y+0.12f,0,0.24f,0.20f,0.18f,0.24f,0.62f,0.18f);
        sphere(x+0.82f,y+0.18f,0.15f,0.035f,0.035f,0.025f,0f,0f,0f);
    }
    private void drawFish(float x,float y,float t) {
        sphere(x,y+(float)Math.sin(t)*0.12f,0,0.60f,0.30f,0.25f,0.16f,0.48f,0.72f);
        sphere(x-0.68f,y,0,0.23f,0.28f,0.10f,0.12f,0.36f,0.60f);
        sphere(x+0.38f,y+0.08f,0.20f,0.04f,0.04f,0.03f,0f,0f,0f);
    }

    void close(){
        if(display!=null){EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
            if(surface!=null)EGL14.eglDestroySurface(display,surface);
            if(context!=null)EGL14.eglDestroyContext(display,context);
            EGL14.eglTerminate(display);}
    }
}
