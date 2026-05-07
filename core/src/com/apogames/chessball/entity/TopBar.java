package com.apogames.chessball.entity;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reusable strip at the top of the canvas (default {@code TOP_BAR_HEIGHT} px).
 * Filled with {@link Constants#COLOR_CLEAR} and a centered title that says what
 * the user is currently looking at (mode, level, opponents). Geometry/visibility
 * lives on {@link ApoEntity}. The title is a list of {@link TextSegment}s
 * rendered side-by-side with per-segment color, so e.g. the black-side player
 * name can be drawn black while the rest stays white.
 */
public class TopBar extends ApoEntity {

    private float[] backgroundColor = Constants.COLOR_CLEAR;
    private BitmapFont font = AssetLoader.font25;

    private List<TextSegment> segments = Collections.emptyList();

    public TopBar() {
        this(0, 0, Constants.GAME_WIDTH, Constants.TOP_BAR_HEIGHT);
    }

    public TopBar(int x, int y, int w, int h) {
        super(x, y, w, h);
        super.setBOpaque(false);
    }

    public TopBar setTitle(String title) {
        if (title == null || title.isEmpty()) {
            this.segments = Collections.emptyList();
        } else {
            List<TextSegment> list = new ArrayList<>(1);
            list.add(new TextSegment(title, Constants.COLOR_WHITE));
            this.segments = list;
        }
        return this;
    }

    public TopBar setSegments(List<TextSegment> segments) {
        this.segments = (segments == null) ? Collections.emptyList() : segments;
        return this;
    }

    public TopBar setBackgroundColor(float[] rgba) { this.backgroundColor = rgba; return this; }
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

        if (segments.isEmpty()) return;

        // Measure all segments first so we can center the combined run.
        float[] widths = new float[segments.size()];
        float totalWidth = 0f;
        for (int i = 0; i < segments.size(); i++) {
            Constants.glyphLayout.setText(font, segments.get(i).getText());
            widths[i] = Constants.glyphLayout.width;
            totalWidth += widths[i];
        }

        float baselineY = dy + h / 2f + 4;
        float drawX = dx + (w - totalWidth) / 2f;

        screen.spriteBatch.begin();
        for (int i = 0; i < segments.size(); i++) {
            TextSegment seg = segments.get(i);
            screen.drawString(seg.getText(), drawX, baselineY,
                    seg.getColor(), font, DrawString.BEGIN, true, false);
            drawX += widths[i];
        }
        screen.spriteBatch.end();
    }
}
