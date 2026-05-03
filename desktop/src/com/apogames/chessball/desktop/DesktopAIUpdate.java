package com.apogames.chessball.desktop;

import com.apogames.chessball.Constants;
import com.apogames.chessball.IAIUpdate;
import com.apogames.chessball.ai.ChessBallAIInformations;
import com.apogames.chessball.ai.ChessBallPlayerAI;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.game.ChessBallBoard;
import com.apogames.chessball.game.enums.ChessBallFigure;

import java.util.ArrayList;
import java.util.List;

public class DesktopAIUpdate implements IAIUpdate, Runnable {

    private ChessBallBoard board;
    private ChessBallPlayerAI playerAI;
    private boolean isBlack;

    private boolean isRunning = false;

    private List<ChessBallStep> update;

    @Override
    public void update(ChessBallBoard board, ChessBallPlayerAI ai, boolean isBlack) {
        this.update = null;

        this.board = board.getClone();
        this.playerAI = ai;
        this.isBlack = isBlack;

        this.isRunning = true;
        Thread t = new Thread(this);
        t.start();
    }

    @Override
    public void run() {
        try {
            this.update();
        } catch (Throwable t) {
            // Don't soft-lock the game if the AI throws — return an empty turn
            // so the engine moves on. Log so we can diagnose.
            System.err.println("[AI] " + this.playerAI.getName() + " crashed: " + t);
            t.printStackTrace();
            this.update = new ArrayList<>();
        } finally {
            this.isRunning = false;
        }
    }

    private void update() {
        List<ChessBallStep> aiUpdate = this.playerAI.update(new ChessBallAIInformations(this.board, this.isBlack));
        if (this.isBlack) {
            List<ChessBallStep> correctedUpdate = new ArrayList<>();
            if (aiUpdate != null) {
                for (ChessBallStep step : aiUpdate) {
                    int figureX = this.board.getBoard().length - 1 - step.getFigureX();
                    int figureY = this.board.getBoard()[0].length - 1 - step.getFigureY();
                    int stepFigureX = this.board.getBoard().length - 1 - step.getStepFigureX();
                    int stepFigureY = this.board.getBoard()[0].length - 1 - step.getStepFigureY();
                    correctedUpdate.add(new ChessBallStep(figureX, figureY, stepFigureX, stepFigureY));
                }
            }
            this.update = correctedUpdate;
        } else {
            this.update = aiUpdate;
        }
        if (this.update == null) {
            this.update = new ArrayList<>();
        }
        this.checkUpdate();
    }

    private void checkUpdate() {
        List<ChessBallStep> realUpdate = new ArrayList<>();
        int foundFigure = 0;
        int foundPasses = 0;
        for (ChessBallStep step : this.update) {
            ChessBallFigure figure = this.board.getBoard()[step.getFigureX()][step.getFigureY()];
            if (figure != ChessBallFigure.EMPTY) {
                if (figure == ChessBallFigure.BALL && foundPasses < Constants.MAX_PASSES) {
                    this.board.getBoard()[step.getFigureX()][step.getFigureY()] = ChessBallFigure.EMPTY;
                    this.board.getBoard()[step.getStepFigureX()][step.getStepFigureY()] = figure;
                    foundPasses += 1;
                    realUpdate.add(step);
                } else if (figure != ChessBallFigure.BALL && isMineFigure(figure) && foundFigure < Constants.MAX_MOVES) {
                    this.board.getBoard()[step.getFigureX()][step.getFigureY()] = ChessBallFigure.EMPTY;
                    this.board.getBoard()[step.getStepFigureX()][step.getStepFigureY()] = figure;
                    foundFigure += 1;
                    realUpdate.add(step);
                }
            }
        }
        this.update = realUpdate;
    }

    private boolean isMineFigure(ChessBallFigure figure) {
        return (this.isBlack && !figure.isWhite()) || (!this.isBlack && figure.isWhite());
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public List<ChessBallStep> getUpdate() {
        return this.update;
    }

    @Override
    public void reset() {
        this.update = null;
        this.isRunning = false;
    }
}
