package br.edu.motionbound.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/** Original hand-authored pixel symbols. Silhouette and fill both identify each resource. */
final class UiIcons implements Disposable {
    enum Icon { HEART, BOLT, CRYSTAL, SCROLL, SKULL, CLOCK, COG, DOOR, PAUSE, LOCK, DOWN_ARROW, CROSSHAIR }
    private final Texture atlas;
    private static final int CELL = 16;

    UiIcons() {
        Pixmap pixels = new Pixmap(CELL * Icon.values().length, CELL, Pixmap.Format.RGBA8888);
        String[][] designs = {
            {"  rrr  rrr  ", " rRrrrRRrrr ", "rRRRrrRRrrrr", "rRRrrrrrrrrr", "rrrrrrrrrrrr", " rrrrrrrrrr ", "  rrrrrrrr  ", "   rrrrrr   ", "    rrrr    ", "     rr     "},
            {"      yy  ", "     yYy  ", "    yYY   ", "   yYY    ", "  yYYYYyy ", " yYYYYYy  ", "    yYy   ", "   yYy    ", "  yYy     ", "  yy      "},
            {"     bb     ", "    bBBb    ", "   bBWWBb   ", "  bBBWWBBb  ", " bBBBWWBBBb ", "bBBBBWBBBBBb", " bBBBBBBBBb ", "  bBBBBBBb  ", "   bBBBBb   ", "    bBBb    ", "     bb     "},
            {" yyyyyyyyyy ", "yWWWWWWWWWWy", "yyyyyyyyyWWy", "  yWWWWWWWWy", "  yWddddWWWy", "  yWWWWWWWWy", "  yWdddddWWy", "  yWWWWWWWWy", "  yWWWWWWWWy", " yyyyyyyyyyy", "yWWWWWWWWWWy", " yyyyyyyyyy "},
            {"   WWWWWW   ", "  WWWWWWWW  ", " WWWWWWWWWW ", " WWWWWWWWWW ", " WWddWWddWW ", " WWddWWddWW ", " WWWWddWWWW ", "  WWWWWWWW  ", "   WdWWdW   ", "   WWWWWW   "},
            {" yyyyyyyyyy ", " yyWWWWWWyy ", "  yWWWWWWy  ", "   yWWWWy   ", "    yWWy    ", "     yy     ", "    yyyy    ", "   yyYYyy   ", "  yyYYYYyy  ", " yyYYYYYYyy ", " yyyyyyyyyy "},
            {"    ss    ", " ssSssSss ", "  sSSSSs  ", " sSSssSSs ", "ssSs  sSss", "ssSs  sSss", " sSSssSSs ", "  sSSSSs  ", " ssSssSss ", "    ss    "},
            {"  yyyyyyyyy ", "  yWWWWWWWy ", "  yWddddWWy ", "  yWddddWWy ", "  yWddddWWy ", "  yWddydWWy ", "  yWddddWWy ", "  yWddddWWy ", "  yWddddWWy ", " yyyyyyyyyyy"},
            {" WWW  WWW ", " WWW  WWW ", " WWW  WWW ", " WWW  WWW ", " WWW  WWW ", " WWW  WWW ", " WWW  WWW ", " WWW  WWW "},
            {"   sssss   ", "  ss   ss  ", "  ss   ss  ", "  ss   ss  ", " yyyyyyyyy ", " yYYYYYYYy ", " yYYYdYYYy ", " yYYdddYYy ", " yYYYdYYYy ", " yyyyyyyyy "},
            {"   yy   ", "   YY   ", "   YY   ", "   YY   ", "yYYYYYYy", " yYYYYy ", "  yYYy  ", "   yy   "},
            {"   yyyy   ", " yy    yy ", " y  YY  y ", "y   YY   y", "yYY WW YYy", "yYY WW YYy", "y   YY   y", " y  YY  y ", " yy    yy ", "   yyyy   "}
        };
        for (int i = 0; i < designs.length; i++) {
            String[] rows = designs[i];
            int left = (CELL - rows[0].length()) / 2, top = (CELL - rows.length) / 2;
            // One-pixel ink outline is part of the silhouette, rather than a smooth shadow.
            for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
                if (rows[y].charAt(x) == ' ') continue;
                pixels.setColor(Color.valueOf("201923"));
                for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++)
                    pixels.drawPixel(i * CELL + left + x + dx, top + y + dy);
            }
            for (int y = 0; y < rows.length; y++) for (int x = 0; x < rows[y].length(); x++) {
                char c = rows[y].charAt(x); if (c == ' ') continue;
                pixels.setColor(switch (c) {
                    case 'r' -> Color.valueOf("c84861"); case 'R' -> Color.valueOf("ff98a0");
                    case 'y' -> Color.valueOf("b47743"); case 'Y' -> Color.valueOf("f5cf73");
                    case 'b' -> Color.valueOf("416aab"); case 'B' -> Color.valueOf("77c3ef");
                    case 'W' -> Color.valueOf("f4dfb1"); case 's' -> Color.valueOf("66718b");
                    case 'S' -> Color.valueOf("b1b9cb"); default -> Color.valueOf("594350");
                });
                pixels.drawPixel(i * CELL + left + x, top + y);
            }
        }
        atlas = new Texture(pixels); pixels.dispose();
        atlas.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    void draw(SpriteBatch batch, Icon icon, float x, float y, float size) { fill(batch, icon, x, y, size, 1); }
    void fill(SpriteBatch batch, Icon icon, float x, float y, float size, float amount) {
        amount = MathUtils.clamp(amount, 0, 1);
        batch.setColor(.36f, .30f, .39f, 1);
        batch.draw(atlas, x, y, size, size, icon.ordinal() * CELL, 0, CELL, CELL, false, false);
        if(icon==Icon.HEART) {
            int columns=Math.round(CELL*amount);batch.setColor(Color.WHITE);
            if(columns>0)batch.draw(atlas,x,y,size*columns/CELL,size,icon.ordinal()*CELL,0,columns,CELL,false,false);
            return;
        }
        int rows = Math.round(CELL * amount);
        batch.setColor(Color.WHITE);
        if (rows > 0) batch.draw(atlas, x, y, size, size * rows / CELL,
                icon.ordinal() * CELL, CELL - rows, CELL, rows, false, false);
    }
    void spent(SpriteBatch batch,Icon icon,float x,float y,float size,float lower,float upper,float alpha) {
        int bottom=Math.round(CELL*MathUtils.clamp(lower,0,1)),top=Math.round(CELL*MathUtils.clamp(upper,0,1));
        if(top<=bottom)return;
        batch.setColor(.5f,.85f,1,alpha);
        batch.draw(atlas,x,y+size*bottom/CELL,size,size*(top-bottom)/CELL,
                icon.ordinal()*CELL,CELL-top,CELL,top-bottom,false,false);
        batch.setColor(Color.WHITE);
    }
    @Override public void dispose() { atlas.dispose(); }
}
