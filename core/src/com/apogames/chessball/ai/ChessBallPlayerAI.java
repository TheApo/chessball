package com.apogames.chessball.ai;

import java.util.List;

public abstract class ChessBallPlayerAI {

    public abstract String getName();

    public abstract List<ChessBallStep> update(ChessBallAIInformations informations);

    public String getCurrentString() {
        return null;
    }

}
