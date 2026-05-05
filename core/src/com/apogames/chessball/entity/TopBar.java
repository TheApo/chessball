package com.apogames.chessball.entity;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * Reusable strip at the top of the canvas (default {@code TOP_BAR_HEIGHT} px).
 * Filled with {@link Constants#COLOR_CLEAR} and a centered title that says what
 * the user is currently looking at (mode, level, opponents). Geometry/visibility
 * lives on {@link ApoEntity}.
 */
public class TopBar extends ApoEntity {

    private float[] backgroundColor = Constants.COLOR_CLEAR;
    private float[] textColor = Constants.COLOR_WHITE;
    private BitmapFont font = AssetLoader.font25;

    private String title = "";

    public TopBar() {
        this(0, 0, Constants.GAME_WIDTH, Constants.TOP_BAR_HEIGHT);
    }

    public TopBar(int x, int y, int w, int h) {
        super(x, y, w, h);
        super.setBOpaque(false);
    }

    public TopBar setTitle(String title) { this.title = title == null ? "" : title; return this; }
    public TopBar setBackgroundColor(float[] rgba) { this.backgroundColor = rgba; return this; }
    public TopBar setTextColor(float[] rgba) { this.textColor = rgba; return this; }
    public TopBar setFont(BitmapFont font) { this.font = font; return this; }

    @Override
    public void render(GameScreen screen, int offsetX, int offsetY) {
        if (!isVisible()) return;
        float dx = getX() + offsetX;
        float dy = getY() + offsetY;
        float w = getWidth();
        float h = getHeight();

        screen.getRenderer().begin(ShapeType.Filled);
        screen.getRenderer().setColor(backgroundColor[0], backgroundColor[1],
                backgroundColor[2], backgroundColor[3]);
        screen.getRenderer().rect(dx, dy, w, h);
        screen.getRenderer().end();

        if (title.isEmpty()) return;

        screen.spriteBatch.begin();
        screen.drawString(title, dx + w / 2f, dy + h / 2f + 4,
                textColor, font, DrawString.MIDDLE, true, false);
        screen.spriteBatch.end();
    }
}
