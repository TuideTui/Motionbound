package br.edu.motionbound.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Original 16px Kenney Input Prompts Pixel tiles, enlarged with nearest filtering. */
final class InputPrompts implements Disposable {
    static final int ESC=17, F11=28, W=86, A=120, S=121, D=122, F=123, R=88;
    static final int I=92, J=126, K=127, L=128;
    static final int MOUSE_LEFT=77, MOUSE_RIGHT=78;
    static final int TURN_LEFT=640, TURN_RIGHT=642, TILT_UP=604, TILT_DOWN=645, RELEASE=577;
    private final Texture texture=new Texture(Gdx.files.internal("kenney/input-prompts-pixel/tilemap_packed.png"));
    private final TextureRegion[][] tiles=TextureRegion.split(texture,16,16);

    InputPrompts() {texture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);}
    void draw(SpriteBatch batch,int tile,float x,float y,float size) {
        batch.setColor(Color.WHITE);batch.draw(tiles[tile/34][tile%34],x,y,size,size);
    }
    void wide(SpriteBatch batch,int first,int second,float x,float y,float size) {
        draw(batch,first,x,y,size);draw(batch,second,x+size,y,size);
    }
    @Override public void dispose() {texture.dispose();}
}
