package com.apogames.chessball.ai.algo;

import com.apogames.chessball.game.enums.ChessBallFigure;

import java.util.Random;

/**
 * Zobrist hashing for ChessBall positions. Tables are generated once with a
 * FIXED seed so persistent transposition-table entries written by self-play
 * stay valid across JVM/process boundaries — different runs (and the bundled
 * trained tt.txt) all hash the same position to the same long.
 *
 * <p>Key = board[9][15] cell contents + side-to-move. Pass/move counters are
 * NOT in the key because the AI's negamax only sees positions at turn boundaries
 * (between full turns), where those counters are always at their starting values.
 */
public final class Zobrist {

    private static final int W = 9;
    private static final int H = 15;
    /** Field-value space (0..13) of {@link ChessBallFigure#getFieldValue()}:
     *  0=EMPTY, 1..6=white K/Q/B/N/R/P, 7..12=black K/Q/B/N/R/P, 13=BALL. */
    private static final int FIGURE_KINDS = 14;
    /** DO NOT CHANGE — every committed tt.txt is pinned to this constant.
     *  Bumping it invalidates all previously trained transposition data. */
    private static final long SEED = 0x9E3779B97F4A7C15L;

    private static final long[][][] PIECE = new long[W][H][FIGURE_KINDS];
    private static final long SIDE_TO_MOVE;

    static {
        Random r = new Random(SEED);
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                for (int k = 0; k < FIGURE_KINDS; k++) {
                    PIECE[x][y][k] = r.nextLong();
                }
            }
        }
        SIDE_TO_MOVE = r.nextLong();
    }

    private Zobrist() {}

    /**
     * Hash the given position. {@code aiSideToMove} mirrors the negamax
     * {@code isMyTurn} flag — same board with the other side to move yields a
     * different hash, so the TT correctly distinguishes the two perspectives.
     */
    public static long hash(ChessBallFigure[][] board, boolean aiSideToMove) {
        long h = 0L;
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                ChessBallFigure f = board[x][y];
                if (f == null) continue;
                int fv = f.getFieldValue();
                if (fv >= 0 && fv < FIGURE_KINDS) {
                    h ^= PIECE[x][y][fv];
                }
            }
        }
        if (aiSideToMove) h ^= SIDE_TO_MOVE;
        return h;
    }
}
