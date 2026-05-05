package com.apogames.chessball.entity;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable rounded panel + colored border + ordered list of text lines.
 * Used by both the puzzle start briefing and the game-over win dialog.
 *
 * <p>Border color is settable and can change per-render (e.g. white/black
 * depending on side-to-move). Geometry (x/y/width/height/visibility) lives
 * on {@link ApoEntity}.
 */
public class Dialog extends ApoEntity {

    /** One text line inside the dialog, positioned by Y offset relative to the dialog top. */
    public static class DialogLine {
        public final String text;
        public final BitmapFont font;
        public final int relativeY;
        public final int relativeX;
        public final boolean centered;
        public final DrawString align;
        public final float[] color;

        public DialogLine(String text, BitmapFont font, int relativeY,
                          int relativeX, boolean centered, DrawString align, float[] color) {
            this.text = text;
            this.font = font;
            this.relativeY = relativeY;
            this.relativeX = relativeX;
            this.centered = centered;
            this.align = align;
            this.color = color;
        }
    }

    private int radius = 12;
    private float[] panelColor = new float[]{0.05f, 0.40f, 0.05f, 0.88f};
    private float[] borderColor = new float[]{0.02f, 0.20f, 0.02f, 1f};
    private float borderThickness = 3f;

    private final List<DialogLine> lines = new ArrayList<>();

    public Dialog(int x, int y, int w, int h) {
        super(x, y, w, h);
        super.setBOpaque(false);
    }

    public Dialog setRadius(int radius) { this.radius = radius; return this; }
    public Dialog setPanelColor(float[] rgba) { this.panelColor = rgba; return this; }
    public Dialog setBorderColor(float[] rgba) { this.borderColor = rgba; return this; }
    public Dialog setBorderThickness(float px) { this.borderThickness = px; return this; }

    public Dialog clearLines() { this.lines.clear(); return this; }

    /** Add a horizontally centered line. */
    public Dialog addCenteredLine(String text, BitmapFont font, int relativeY, float[] color) {
        this.lines.add(new DialogLine(text, font, relativeY, 0, true, DrawString.MIDDLE, color));
        return this;
    }

    /** Add a line at an absolute X (relative to dialog left). */
    public Dialog addLine(String text, BitmapFont font, int relativeX, int relativeY,
                          DrawString align, float[] color) {
        this.lines.add(new DialogLine(text, font, relativeY, relativeX, false, align, color));
        return this;
    }

    @Override
    public void render(GameScreen screen, int offsetX, int offsetY) {
        if (!isVisible()) return;
        float dx = getX() + offsetX;
        float dy = getY() + offsetY;
        float w = getWidth();
        float h = getHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        screen.getRenderer().begin(ShapeType.Filled);
        screen.getRenderer().setColor(panelColor[0], panelColor[1], panelColor[2], panelColor[3]);
        screen.getRenderer().roundedRect(dx, dy, w, h, radius);
        screen.getRenderer().end();

        Gdx.gl20.glLineWidth(borderThickness);
        screen.getRenderer().begin(ShapeType.Line);
        screen.getRenderer().setColor(borderColor[0], borderColor[1], borderColor[2], borderColor[3]);
        screen.getRenderer().roundedRectLine(dx, dy, w, h, radius);
        screen.getRenderer().end();
        Gdx.gl20.glLineWidth(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (lines.isEmpty()) return;

        float centerX = dx + w / 2f;
        screen.spriteBatch.begin();
        for (DialogLine line : lines) {
            float textX = line.centered ? centerX : (dx + line.relativeX);
            float textY = dy + line.relativeY;
            float[] color = line.color != null ? line.color : Constants.COLOR_WHITE;
            BitmapFont font = line.font != null ? line.font : AssetLoader.font20;
            screen.drawString(line.text, textX, textY, color, font, line.align);
        }
        screen.spriteBatch.end();
    }
}
