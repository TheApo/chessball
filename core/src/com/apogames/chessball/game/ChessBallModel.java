package com.apogames.chessball.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.backend.SequentiallyThinkingScreenModel;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.entity.TopBar;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public abstract class ChessBallModel extends SequentiallyThinkingScreenModel {

    public static final String FUNCTION_EXIT = "main_exit";

    private final ChessBallBoard board = new ChessBallBoard();

    /** Shared title bar instance — title is queried per render via {@link #getTopBarTitle()}. */
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

    /** Title shown in the in-app top bar — depends on the current state of the model. */
    protected abstract String getTopBarTitle();

    /** Renders the title bar (Constants.COLOR_CLEAR strip + centered title). Call once
     *  per frame, before background content. */
    protected void renderTopBar() {
        topBar.setTitle(getTopBarTitle()).render(getMainPanel(), 0, 0);
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
