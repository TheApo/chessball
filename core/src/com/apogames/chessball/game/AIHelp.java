package com.apogames.chessball.game;

import com.apogames.chessball.ai.ChessBallPlayerAI;

public class AIHelp {

    private final ChessBallPlayerAI ai;
    private final int index;

    public AIHelp(ChessBallPlayerAI ai, int index) {
        this.ai = ai;
        this.index = index;
    }

    public ChessBallPlayerAI getAi() {
        return ai;
    }

    public int getIndex() {
        return index;
    }
}
