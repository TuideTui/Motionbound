package br.edu.motionbound.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/** Kenney Tiny Dungeon atlas if installed; built-in original fallback otherwise. */
public final class GameAssets implements Disposable {
    public final Texture tilesTexture;
    public final Texture heroTexture;
    public final Texture slimeTexture;
    public final Texture skeletonTexture;
    public final Texture glow;
    public final Texture actionsTexture, heartTexture;
    public final TextureRegion[][] actions;
    public final TextureRegion heroPortrait;
    /** Pixel-aligned palm positions in each of the six armed poses; rows match the hero atlas. */
    private static final int[][] SWORD_GRIPS={
        {21,20, 21,19, 20,19, 21,19, 23,17, 19,19},
        {11,20, 11,18, 11,19, 10,12, 8,16, 12,19},
        {13,20, 11,18, 10,18, 13,19, 8,17, 12,19},
        {19,20, 20,18, 20,18, 18,19, 23,17, 20,18}
    };
    public record SwordHand(float x,float y,TextureRegion cover) { }
    public final SwordHand[][] swordHands=new SwordHand[4][6];
    private final Texture townTexture, detailsTexture;
    public final TextureRegion[][] hero, slime, skeleton;
    public final boolean usingKenney;
    private final TextureRegion[][] tiles;
    private final int cols;
    private final TextureRegion[][] town;
    private final TextureRegion[] details;

    public GameAssets() {
        FileHandle atlas=Gdx.files.internal("kenney/tilemap_packed.png");
        usingKenney=atlas.exists();
        tilesTexture = new Texture(usingKenney ? atlas : Gdx.files.internal("fallback/tiles.png"));
        tilesTexture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        tiles=TextureRegion.split(tilesTexture,16,16);
        cols=tiles[0].length;
        heroTexture=texture("characters/hero.png");
        slimeTexture=texture("characters/slime.png");
        skeletonTexture=texture("characters/skeleton.png");
        hero=TextureRegion.split(heroTexture,16,16);
        actionsTexture=texture("characters/hero-actions.png");
        actions=TextureRegion.split(actionsTexture,32,32);
        for(int facing=0;facing<4;facing++)for(int pose=0;pose<6;pose++) {
            int x=SWORD_GRIPS[facing][pose*2],y=SWORD_GRIPS[facing][pose*2+1];
            swordHands[facing][pose]=new SwordHand(x+.5f-16,18.5f-y,
                    new TextureRegion(actions[facing][pose],x-1,y-1,3,3));
        }
        heroPortrait=new TextureRegion(actions[0][0],8,5,18,21);
        Pixmap heart=new Pixmap(10,10,Pixmap.Format.RGBA8888);
        String[] heartRows={"  rr  rr  "," rRRrrRrr ","rRRRrrrrrr","rrrrrrrrrr"," rrrrrrrr ","  rrrrrr  ","   rrrr   ","    rr    "};
        for(int y=0;y<heartRows.length;y++)for(int x=0;x<10;x++)if(heartRows[y].charAt(x)!=' ') {
            heart.setColor(Color.valueOf("392332"));
            if(y+2<10)heart.drawPixel(x,y+2);
        }
        for(int y=0;y<heartRows.length;y++)for(int x=0;x<10;x++)if(heartRows[y].charAt(x)!=' ') {
            heart.setColor(Color.valueOf(heartRows[y].charAt(x)=='R'?"ff9ba2":"d24e68"));heart.drawPixel(x,y+1);
        }
        heartTexture=new Texture(heart);heart.dispose();
        heartTexture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        slime=TextureRegion.split(slimeTexture,16,16);
        skeleton=TextureRegion.split(skeletonTexture,16,16);
        townTexture=texture("kenney/tiny-town/tilemap_packed.png");
        town=TextureRegion.split(townTexture,16,16);
        Pixmap accents=new Pixmap(112,16,Pixmap.Format.RGBA8888);
        accents.setColor(Color.valueOf("58424b"));
        for(int[] p:new int[][]{{2,7},{3,7},{4,8},{11,3},{11,4},{12,5},{8,12},{9,12}})accents.drawPixel(p[0],p[1]);
        accents.setColor(Color.valueOf("92705d"));accents.drawPixel(3,8);accents.drawPixel(12,6);
        accents.setColor(Color.valueOf("3d584a"));
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)if((x*13+y*7)%17<4)accents.drawPixel(16+x,y);
        accents.setColor(Color.valueOf("77915b"));
        for(int[] p:new int[][]{{2,6},{3,6},{9,12},{10,11},{13,3}})accents.drawPixel(16+p[0],p[1]);
        for(int tile=2;tile<7;tile++) {
            int offset=tile*16;
            accents.setColor(Color.valueOf("653a4e"));accents.fillRectangle(offset,0,16,16);
            accents.setColor(Color.valueOf("734455"));
            for(int y=2;y<16;y+=4)for(int x=2;x<16;x+=4)accents.drawPixel(offset+x,y);
            accents.setColor(Color.valueOf("b98b62"));
            if(tile==3||tile==5) {accents.fillRectangle(offset,1,16,2);accents.fillRectangle(offset,13,16,2);}
            if(tile==4||tile==5) {accents.fillRectangle(offset+1,0,2,16);accents.fillRectangle(offset+13,0,2,16);}
            if(tile==6) {
                accents.drawRectangle(offset+5,5,6,6);
                accents.fillRectangle(offset+7,3,2,10);accents.fillRectangle(offset+3,7,10,2);
                accents.setColor(Color.valueOf("653a4e"));accents.fillRectangle(offset+7,7,2,2);
            }
        }
        detailsTexture=new Texture(accents);accents.dispose();
        detailsTexture.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        details=TextureRegion.split(detailsTexture,16,16)[0];
        Pixmap light=new Pixmap(64,64,Pixmap.Format.RGBA8888);
        for(int y=0;y<64;y++)for(int x=0;x<64;x++) {
            float distance=(float)Math.sqrt((x-31.5f)*(x-31.5f)+(y-31.5f)*(y-31.5f));
            float falloff=Math.max(0,1-(int)(distance/4)/8f);
            light.setColor(1,1,1,falloff*falloff*.4f);light.drawPixel(x,y);
        }
        glow=new Texture(light);light.dispose();
        glow.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
    }
    private Texture texture(String path) {
        Texture t=new Texture(Gdx.files.internal(path));
        t.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
        return t;
    }
    public TextureRegion tile(int index) {
        int row=index / cols;
        int col=index % cols;
        if(row>=tiles.length || col>=cols) return tiles[0][0];
        return tiles[row][col];
    }
    public TextureRegion prop(int index) {
        if(index<DungeonMap.TOWN)return tile(index);
        int local=index-DungeonMap.TOWN;
        return town[local/town[0].length][local%town[0].length];
    }
    public TextureRegion detail(int index) {return details[index];}
    @Override public void dispose() {
        tilesTexture.dispose();heroTexture.dispose();slimeTexture.dispose();skeletonTexture.dispose();
        townTexture.dispose();detailsTexture.dispose();glow.dispose();
        actionsTexture.dispose();heartTexture.dispose();
    }
}
