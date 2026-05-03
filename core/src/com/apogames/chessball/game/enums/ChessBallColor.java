package com.apogames.chessball.game.enums;

public enum ChessBallColor {
    WHITE(0),
    BLACK(1);

    private final int imageValue;

    ChessBallColor(int imageValue) {
        this.imageValue = imageValue;
    }

    public int getImageValue() {
        return imageValue;
    }
}
