package com.apogames.chessball;

import com.apogames.chessball.ai.ChessBallAIInformations;
import com.apogames.chessball.ai.ChessBallPlayerAI;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.game.ChessBallBoard;

import java.util.ArrayList;
import java.util.List;

public class DefaultAIUpdate implements IAIUpdate {

    private List<ChessBallStep> update;

    @Override
    public void update(ChessBallBoard board, ChessBallPlayerAI ai, boolean isBlack) {
        this.update = null;
        List<ChessBallStep> update = ai.update(new ChessBallAIInformations(board, isBlack));
        if (isBlack) {
            List<ChessBallStep> correctedUpdate = new ArrayList<>();
            if (update != null) {
                for (int i = 0; i < update.size(); i++) {
                    ChessBallStep step = update.get(i);
                    int figureX = board.getBoard().length - 1 - step.getFigureX();
                    int figureY = board.getBoard()[0].length - 1 - step.getFigureY();
                    int stepFigureX = board.getBoard().length - 1 - step.getStepFigureX();
                    int stepFigureY = board.getBoard()[0].length - 1 - step.getStepFigureY();
                    correctedUpdate.add(new ChessBallStep(figureX, figureY, stepFigureX, stepFigureY));
                }
            }
            this.update = correctedUpdate;
        } else {
            this.update = update;
        }
        if (this.update == null) {
            this.update = new ArrayList<>();
        }
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public List<ChessBallStep> getUpdate() {
        return this.update;
    }

    @Override
    public void reset() {
        this.update = null;
    }

}
