package com.apogames.chessball.game.puzzle;

/**
 * Single move from a stored demo: figure of {@code figType} (see
 * {@link com.apogames.chessball.game.enums.ChessBallFigure#getFieldValue()})
 * moves from {@code (startX,startY)} to {@code (destinationX,destinationY)}.
 *
 * Demo segment encoding: {@code startX,startY,destinationX,destinationY,figType}.
 */
public class ChessBallAIMove {

    private final int figure;
    private final int startX;
    private final int startY;
    private final int destinationX;
    private final int destinationY;

    public ChessBallAIMove(int figure, int startX, int startY, int destinationX, int destinationY) {
        this.figure = figure;
        this.startX = startX;
        this.startY = startY;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
    }

    public int getFigure() { return figure; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getDestinationX() { return destinationX; }
    public int getDestinationY() { return destinationY; }

    @Override
    public String toString() {
        return "(" + startX + "," + startY + ")->(" + destinationX + "," + destinationY + ") fig=" + figure;
    }
}
