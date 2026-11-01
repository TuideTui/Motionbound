package br.edu.motionbound.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** Bundled Kenney Pixel, rasterized without smoothing at the current display density. */
final class UiFont implements Disposable {
    private static final Font PIXEL = loadPixelFont();
    private static final Color SHADOW = new Color(.06f,.04f,.07f,.8f);
    private final Texture texture;
    private final TextureRegion[] glyphs = new TextureRegion[256];
    private final float[] advances = new float[256];
    private final float cellWidth, lineHeight, density;

    UiFont(int size, boolean bold, float density) {
        this.density = density;
        Font font = PIXEL.deriveFont((float)Math.max(12, Math.round(size * density)));
        BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D measure = probe.createGraphics();
        measure.setFont(font);
        FontMetrics metrics = measure.getFontMetrics();
        int cw = 1;
        for (int c = 32; c < 256; c++) cw = Math.max(cw, metrics.charWidth(c) + 4);
        java.awt.Rectangle bounds = font.createGlyphVector(measure.getFontRenderContext(),
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789ÁÀÂÃÉÊÍÓÔÕÚÇáàâãéêíóôõúç!?gjpqy").getPixelBounds(null, 0, 0);
        int ascent = -bounds.y, ch = bounds.height + 4;
        cellWidth = cw / density;
        lineHeight = ch / density;
        BufferedImage atlas = new BufferedImage(cw * 16, ch * 14, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = atlas.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setFont(font);
        graphics.setColor(java.awt.Color.WHITE);
        for (int c = 32; c < 256; c++) {
            int index = c - 32;
            graphics.drawString(String.valueOf((char)c), index % 16 * cw + 2,
                    index / 16 * ch + ascent + 2);
            advances[c] = metrics.charWidth(c) / density;
        }
        graphics.dispose();
        measure.dispose();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(atlas, "png", bytes);
            byte[] png = bytes.toByteArray();
            Pixmap pixmap = new Pixmap(png, 0, png.length);
            texture = new Texture(pixmap);
            pixmap.dispose();
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Não foi possível criar a fonte da interface", e);
        }
        for (int c = 32; c < 256; c++) {
            int index = c - 32;
            glyphs[c] = new TextureRegion(texture, index % 16 * cw, index / 16 * ch, cw, ch);
        }
    }

    float width(String text) {
        float width = 0;
        for (int i = 0; i < text.length(); i++) width += advances[safe(text.charAt(i))];
        return width;
    }

    void draw(SpriteBatch batch, String text, float x, float top, Color color) {
        x = Math.round(x * density) / density;
        top = Math.round(top * density) / density;
        float shadow = Math.max(1, Math.round(density)) / density;
        // Light inscriptions on stone get a shadow; dark ink on parchment stays crisp.
        if (color.r + color.g + color.b > 1.7f) drawGlyphs(batch, text, x + shadow, top - shadow, SHADOW);
        drawGlyphs(batch, text, x, top, color);
    }

    private void drawGlyphs(SpriteBatch batch, String text, float x, float top, Color color) {
        batch.setColor(color);
        for (int i = 0; i < text.length(); i++) {
            int c = safe(text.charAt(i));
            if (c != ' ') batch.draw(glyphs[c], x - 2 / density, top - lineHeight, cellWidth, lineHeight);
            x += advances[c];
        }
        batch.setColor(Color.WHITE);
    }

    private int safe(char c) { return c >= 32 && c < 256 ? c : '?'; }
    private static Font loadPixelFont() {
        try (java.io.InputStream stream = Gdx.files.internal("fonts/KenneyPixel.ttf").read()) {
            return Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (java.io.IOException | java.awt.FontFormatException e) {
            throw new IllegalStateException("Fonte Kenney Pixel ausente ou inválida", e);
        }
    }
    @Override public void dispose() { texture.dispose(); }
}
