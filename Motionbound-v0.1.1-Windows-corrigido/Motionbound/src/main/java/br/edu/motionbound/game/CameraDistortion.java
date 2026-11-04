package br.edu.motionbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/** Pinches and twists the rendered world during a teleport. The HUD is drawn afterwards. */
public final class CameraDistortion implements Disposable {
    private static final String VERTEX="""
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        uniform mat4 u_projTrans;
        varying vec4 v_color;
        varying vec2 v_texCoords;
        void main() {
            v_color=a_color;
            v_color.a*=255.0/254.0;
            v_texCoords=a_texCoord0;
            gl_Position=u_projTrans*a_position;
        }
        """;
    private static final String FRAGMENT="""
        #ifdef GL_ES
        precision mediump float;
        #endif
        uniform sampler2D u_texture;
        uniform float u_amount;
        uniform float u_twist;
        uniform float u_aspect;
        varying vec4 v_color;
        varying vec2 v_texCoords;
        void main() {
            vec2 uv=v_texCoords;
            float edge=16.0*uv.x*(1.0-uv.x)*uv.y*(1.0-uv.y);
            float falloff=edge*edge;
            vec2 p=(uv-0.5)*vec2(u_aspect,1.0);
            float angle=u_twist*falloff;
            float c=cos(angle),s=sin(angle);
            p=mat2(c,-s,s,c)*p*(1.0+u_amount*falloff);
            vec2 warped=clamp(p/vec2(u_aspect,1.0)+0.5,0.0,1.0);
            gl_FragColor=texture2D(u_texture,warped)*v_color;
        }
        """;
    private final ShaderProgram shader=new ShaderProgram(VERTEX,FRAGMENT);
    private final SpriteBatch batch;
    private final OrthographicCamera camera=new OrthographicCamera();
    private FrameBuffer frame;
    private TextureRegion picture;

    public CameraDistortion() {
        if(!shader.isCompiled())throw new IllegalStateException("Falha no efeito de câmera: "+shader.getLog());
        batch=new SpriteBatch(100,shader);
        batch.disableBlending();
    }
    public boolean active(boolean pulling,float remaining) {
        return pulling||remaining>StatusEffects.RIFT_RELEASE_TIME-.7f;
    }
    public void begin(int width,int height) {
        if(frame==null||frame.getWidth()!=width||frame.getHeight()!=height) {
            if(frame!=null)frame.dispose();
            frame=new FrameBuffer(Pixmap.Format.RGBA8888,width,height,false);
            frame.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
            picture=new TextureRegion(frame.getColorBufferTexture());picture.flip(false,true);
            camera.setToOrtho(false,width,height);batch.setProjectionMatrix(camera.combined);
        }
        frame.begin();
    }
    public void present(boolean pulling,float remaining) {
        frame.end();
        Gdx.gl.glViewport(0,0,Gdx.graphics.getBackBufferWidth(),Gdx.graphics.getBackBufferHeight());
        float amount,twist;
        if(pulling) {
            float p=MathUtils.clamp((StatusEffects.RIFT_DURATION-remaining)/StatusEffects.RIFT_PULL_TIME,0,1);
            amount=.72f*p*p;twist=.30f*p*p;
        } else {
            float fade=1-MathUtils.clamp((StatusEffects.RIFT_RELEASE_TIME-remaining)/.7f,0,1);
            amount=-.25f*fade*fade;twist=-.12f*fade*fade;
        }
        batch.setColor(Color.WHITE);batch.begin();
        shader.setUniformf("u_amount",amount);shader.setUniformf("u_twist",twist);
        shader.setUniformf("u_aspect",frame.getWidth()/(float)frame.getHeight());
        batch.draw(picture,0,0,frame.getWidth(),frame.getHeight());batch.end();
    }
    @Override public void dispose(){if(frame!=null)frame.dispose();batch.dispose();shader.dispose();}
}
