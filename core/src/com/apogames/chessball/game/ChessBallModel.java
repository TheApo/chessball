package com.apogames.chessball.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.backend.SequentiallyThinkingScreenModel;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.entity.TextSegment;
import com.apogames.chessball.entity.TopBar;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ChessBallModel extends SequentiallyThinkingScreenModel {

    public static final String FUNCTION_EXIT = "main_exit";

    private final ChessBallBoard board = new ChessBallBoard();

    /** Shared title bar instance — segments are queried per render via {@link #getTopBarSegments()}. */
    protected final TopBar topBar = new TopBar();

    /** Persistent click-mode selection (board cell). x = -1 means no selection.
     *  Set when the user clicks (release within {@link #CLICK_DRAG_THRESHOLD_SQ} of press)
     *  on an own movable piece; cleared on a successful move, deselect-click, or drag. */
    protected final GridPoint2 selectedPosition = new GridPoint2(-1, -1);

    /** Pixel coordinates where the most recent {@code mousePressed} occurred — used by
     *  {@link #wasClick(int, int)} to distinguish a click from a drag at release time. */
    protected int pressPixelX = 0;
    protected int pressPixelY = 0;

    /** Squared pixel distance below which a press→release counts as a click (vs drag).
     *  8 px is small enough that any deliberate drag exceeds it, large enough to absorb
     *  hand jitter on touch devices. */
    protected static final int CLICK_DRAG_THRESHOLD_SQ = 8 * 8;

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

    /** True when a release at (x,y) is close enough to {@link #pressPixelX}/{@link #pressPixelY}
     *  to count as a click rather than a drag. */
    protected boolean wasClick(int releaseX, int releaseY) {
        int dx = releaseX - pressPixelX;
        int dy = releaseY - pressPixelY;
        return dx * dx + dy * dy < CLICK_DRAG_THRESHOLD_SQ;
    }

    protected boolean hasClickSelection() {
        return selectedPosition.x >= 0;
    }

    /** Promote the press-time pickup to a persistent click selection. The press
     *  already painted move-target circles via {@code setPossibleStepsForPosition},
     *  so we only need to remember the source cell and keep the highlight on it. */
    protected void promoteClickSelection(GridPoint2 source) {
        selectedPosition.set(source.x, source.y);
        getBoard().getMouseOver().set(source.x, source.y);
    }

    protected void clearClickSelection() {
        selectedPosition.x = -1;
        getBoard().deleteCircle();
        getBoard().getMouseOver().x = -1;
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
