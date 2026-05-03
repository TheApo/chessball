package com.apogames.chessball.ai;

public class ChessBallStep {

    private final int figureX;
    private final int figureY;

    private final int stepFigureX;
    private final int stepFigureY;

    public ChessBallStep(final int figureX, final int figureY, final int stepFigureX, final int stepFigureY) {
        this.figureX = figureX;
        this.figureY = figureY;
        this.stepFigureX = stepFigureX;
        this.stepFigureY = stepFigureY;
    }

    public int getFigureX() {
        return figureX;
    }

    public int getFigureY() {
        return figureY;
    }

    public int getStepFigureX() {
        return stepFigureX;
    }

    public int getStepFigureY() {
        return stepFigureY;
    }
}
