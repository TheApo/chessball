package com.apogames.chessball.game;

import com.apogames.chessball.backend.SequentiallyThinkingScreenModel;
import com.apogames.chessball.entity.ApoButton;

public abstract class ChessBallModel extends SequentiallyThinkingScreenModel {

    public static final String FUNCTION_EXIT = "main_exit";

    private final ChessBallBoard board = new ChessBallBoard();

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
}
