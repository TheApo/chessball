package com.apogames.chessball.game.enums;

/**
 * Encoding aligned with ChessballAndroid (canonical demo format).
 *
 * <ul>
 *   <li>{@code imageValue} — sprite frame index in {@code AssetLoader.figures}.</li>
 *   <li>{@code fieldValue} — numeric id used in board state and demo {@code figType} field
 *       (0=empty, 1..6=white K/Q/B/N/R/P, 7..12=black K/Q/B/N/R/P, 13=ball).</li>
 *   <li>{@code puzzleLevel} — single-hex-char encoding of {@code fieldValue}; used in
 *       stored puzzle level strings.</li>
 * </ul>
 */
public enum ChessBallFigure {

    EMPTY(-1, 0, "0"),
    WHITE_KING(0, 1, "1"),
    WHITE_QUEEN(1, 2, "2"),
    WHITE_BISHOP(2, 3, "3"),
    WHITE_KNIGHT(3, 4, "4"),
    WHITE_ROOK(4, 5, "5"),
    WHITE_PAWN(5, 6, "6"),
    BLACK_KING(6, 7, "7"),
    BLACK_QUEEN(7, 8, "8"),
    BLACK_BISHOP(8, 9, "9"),
    BLACK_KNIGHT(9, 10, "a"),
    BLACK_ROOK(10, 11, "b"),
    BLACK_PAWN(11, 12, "c"),
    BALL(12, 13, "d"),
    YELLOW_CIRCLE(13, -1, ""),
    RED_CIRCLE(14, -1, "");

    private final int imageValue;
    private final int fieldValue;
    private final String puzzleLevel;

    ChessBallFigure(int imageValue, int fieldValue, String puzzleLevel) {
        this.imageValue = imageValue;
        this.fieldValue = fieldValue;
        this.puzzleLevel = puzzleLevel;
    }

    public int getImageValue() {
        return imageValue;
    }

    public int getFieldValue() {
        return fieldValue;
    }

    public String getPuzzleLevel() {
        return puzzleLevel;
    }

    public boolean isWhite() {
        return this == WHITE_KING
            || this == WHITE_QUEEN
            || this == WHITE_BISHOP
            || this == WHITE_KNIGHT
            || this == WHITE_ROOK
            || this == WHITE_PAWN;
    }

    /** Lookup by single-hex-char puzzle string (used by stored puzzle levels). */
    public static ChessBallFigure getFigure(String puzzleLevel) {
        for (ChessBallFigure figure : values()) {
            if (figure.puzzleLevel.equalsIgnoreCase(puzzleLevel)) {
                return figure;
            }
        }
        return EMPTY;
    }

    /** Lookup by raw field value (used by demo move parsing). */
    public static ChessBallFigure fromFieldValue(int fieldValue) {
        for (ChessBallFigure figure : values()) {
            if (figure.fieldValue == fieldValue && figure != YELLOW_CIRCLE && figure != RED_CIRCLE) {
                return figure;
            }
        }
        return EMPTY;
    }
}
