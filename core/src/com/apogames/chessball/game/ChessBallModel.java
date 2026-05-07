package com.apogames.chessball.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.backend.SequentiallyThinkingScreenModel;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.entity.TextSegment;
import com.apogames.chessball.entity.TopBar;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ChessBallModel extends SequentiallyThinkingScreenModel {

    public static final String FUNCTION_EXIT = "main_exit";

    private final ChessBallBoard board = new ChessBallBoard();

    /** Shared title bar instance — segments are queried per render via {@link #getTopBarSegments()}. */
    protected final TopBar topBar = new TopBar();

    public ChessBallModel(MainPanel mainPanel) {
        super(mainPanel);
    }

    public ChessBallBoard getBoard() {
        return board;
    }

    /**
     * Proxy to {@link MainPanel#getButtonByFunction(String)}.
     */
    public ApoButton getButtonByFunction(String function) {
        return getMainPanel().getButtonByFunction(function);
    }

    protected abstract float getScale();

    /**
     * Title shown in the in-app top bar — colored segments rendered side by side.
     * Subclasses build the list to color individual parts (e.g. player names by side).
     */
    protected abstract List<TextSegment> getTopBarSegments();

    /** Convenience: a single white-colored title. */
    protected static List<TextSegment> singleSegment(String text) {
        return Collections.singletonList(new TextSegment(text, Constants.COLOR_WHITE));
    }

    /**
     * Returns the given title in white, followed by " - " (white) and the side-to-move
     * label ("schwarz"/"weiss") drawn in that side's color. Used in Tutorial and
     * Puzzle so the top bar communicates whose turn it is at a glance.
     */
    protected List<TextSegment> withSideSuffix(String title) {
        I18NBundle i18n = Localization.getInstance().getCommon();
        boolean white = getBoard().getCurrentColor() == ChessBallColor.WHITE;
        String sideLabel = i18n.get(white ? "topbar.side.white" : "topbar.side.black");
        float[] sideColor = white ? Constants.COLOR_WHITE : Constants.COLOR_BLACK;
        List<TextSegment> segments = new ArrayList<>(2);
        segments.add(new TextSegment(title + " - ", Constants.COLOR_WHITE));
        segments.add(new TextSegment(sideLabel, sideColor));
        return segments;
    }

    /** Renders the title bar (Constants.COLOR_CLEAR strip + centered title). Call once
     *  per frame, before background content. */
    protected void renderTopBar() {
        topBar.setSegments(getTopBarSegments()).render(getMainPanel(), 0, 0);
    }

    /**
     * Draws a 3px border around the entire canvas in the side-to-move color
     * (white = White's turn, black = Black's turn). Used by Game + Puzzle to
     * signal whose move it is. Call last in {@code render()} so the border
     * sits on top of all other content.
     */
    protected void renderTurnBorder() {
        float[] color = (getBoard().getCurrentColor() == ChessBallColor.WHITE)
                ? Constants.COLOR_WHITE : Constants.COLOR_BLACK;
        float t = Constants.APP_BORDER_THICKNESS;
        float half = t / 2f;
        getMainPanel().getRenderer().begin(ShapeType.Filled);
        getMainPanel().getRenderer().setColor(color[0], color[1], color[2], color[3]);
        getMainPanel().getRenderer().drawThickRectOutline(
                half, half, Constants.GAME_WIDTH - t, Constants.GAME_HEIGHT - t, t);
        getMainPanel().getRenderer().end();
    }
}
