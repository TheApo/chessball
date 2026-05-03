package com.apogames.chessball;

import com.apogames.chessball.ai.ChessBallPlayerAI;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.game.ChessBallBoard;

import java.util.List;

public interface IAIUpdate {

    void update(ChessBallBoard board, ChessBallPlayerAI ai, boolean isBlack);

    boolean isRunning();

    List<ChessBallStep> getUpdate();

    void reset();
}
