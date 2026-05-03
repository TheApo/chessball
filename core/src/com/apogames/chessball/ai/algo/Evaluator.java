package com.apogames.chessball.ai.algo;

import com.apogames.chessball.game.enums.ChessBallFigure;

/**
 * Static evaluation, AI-POV (AI is "white" in {@link com.apogames.chessball.ai.ChessBallAIInformations};
 * own goal y=14, opponent goal y=0).
 *
 * <p>Priority order (largest magnitude wins):
 * <ol>
 *   <li>{@code ±GOAL} — ball in goal area, or king captured (terminals).</li>
 *   <li>{@code ±W_SHOT_AT_GOAL} — direct 1-pass scoring threat from current ball pos.</li>
 *   <li>Material — chess-standard values × 1000, bishops boosted (strong for ball-passing).</li>
 *   <li>Ball positional — y-progress, deep-zone bonus, friendlies near ball, open lane.</li>
 * </ol>
 *
 * <p>Capturing a queen ({@code +10_000}) clearly beats a row of ball progress
 * ({@code +300}); a 1-pass scoring setup ({@code +100_000}) clearly beats a queen
 * capture. So the AI prioritises tactics correctly.
 */
public final class Evaluator {

    public static final int GOAL = 1_000_000;

    private static final int W = 9;
    private static final int H = 15;
    private static final int OWN_GOAL_Y = 14;
    private static final int ENEMY_GOAL_Y = 0;

    /** Indexed by {@link ChessBallFigure#getFieldValue()} (1..12, 13=ball). */
    private static final int[] MATERIAL = new int[14];
    static {
        MATERIAL[1]  = 0;       // white king (terminal)
        MATERIAL[2]  = 10_000;  // white queen
        MATERIAL[3]  = 5_000;   // white bishop  (strong in chessball — long diagonals for ball passes)
        MATERIAL[4]  = 4_000;   // white knight
        MATERIAL[5]  = 7_000;   // white rook
        MATERIAL[6]  = 800;     // white pawn
        MATERIAL[7]  = 0;       // black king
        MATERIAL[8]  = 10_000;
        MATERIAL[9]  = 5_000;
        MATERIAL[10] = 4_000;
        MATERIAL[11] = 7_000;
        MATERIAL[12] = 800;
    }

    private static final int W_SHOT_AT_GOAL  = 100_000;  // per goal cell reachable in 1 pass
    private static final int W_NEAR_GOAL     = 5_000;    // per row inside last 3 rows
    private static final int W_BALL_PROGRESS = 300;      // per row of ball y advance
    private static final int W_NEAR_BALL     = 400;      // per friendly adjacent to ball
    private static final int W_OPEN_LANE     = 200;      // per empty cell in ball's column ahead

    private Evaluator() {}

    public static int evaluate(ChessBallFigure[][] board) {
        int bx = -1, by = -1;
        int materialBalance = 0;
        boolean whiteKing = false, blackKing = false;

        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                ChessBallFigure f = board[x][y];
                if (f == null || f == ChessBallFigure.EMPTY) continue;
                if (f == ChessBallFigure.BALL) { bx = x; by = y; continue; }
                if (f == ChessBallFigure.WHITE_KING) whiteKing = true;
                else if (f == ChessBallFigure.BLACK_KING) blackKing = true;
                materialBalance += f.isWhite() ? MATERIAL[f.getFieldValue()] : -MATERIAL[f.getFieldValue()];
            }
        }

        // Terminals.
        if (!whiteKing) return -GOAL;
        if (!blackKing) return  GOAL;
        if (by == OWN_GOAL_Y   && bx >= 3 && bx <= 5) return  GOAL;
        if (by == ENEMY_GOAL_Y && bx >= 3 && bx <= 5) return -GOAL;

        if (bx < 0) return materialBalance;

        int score = materialBalance;

        // Direct scoring threat — biggest non-terminal signal.
        score += W_SHOT_AT_GOAL * shotPotential(board, bx, by, true);
        score -= W_SHOT_AT_GOAL * shotPotential(board, bx, by, false);

        // Ball depth in opposing half.
        score += (by - 7) * W_BALL_PROGRESS;
        if (by >= 12) score += W_NEAR_GOAL * (by - 11);
        if (by <=  2) score -= W_NEAR_GOAL * (3 - by);

        // Pass-control proxy — friendlies adjacent to the ball.
        int wAdj = 0, bAdj = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = bx + dx, y = by + dy;
                if (x < 0 || x >= W || y < 0 || y >= H) continue;
                ChessBallFigure f = board[x][y];
                if (f == null || f == ChessBallFigure.EMPTY || f == ChessBallFigure.BALL) continue;
                if (f.isWhite()) wAdj++; else bAdj++;
            }
        }
        score += W_NEAR_BALL * (wAdj - bAdj);

        // Open passing lane in ball's column.
        score += W_OPEN_LANE * (openLane(board, bx, by, +1) - openLane(board, bx, by, -1));

        return score;
    }

    // --- Shot-potential helpers ----------------------------------------------------

    private static int shotPotential(ChessBallFigure[][] board, int bx, int by, boolean forWhite) {
        int goalY = forWhite ? OWN_GOAL_Y : ENEMY_GOAL_Y;
        int hits = 0;
        for (int gx = 3; gx <= 5; gx++) {
            if (canBallReachVia(board, bx, by, gx, goalY, forWhite)) hits++;
        }
        return hits;
    }

    /** Can the ball at {@code (bx,by)} land on goal cell {@code (gx,gy)} via the pattern
     *  of any adjacent friendly piece, with chess-blocker rules? */
    private static boolean canBallReachVia(ChessBallFigure[][] board, int bx, int by,
                                           int gx, int gy, boolean forWhite) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int nx = bx + dx, ny = by + dy;
                if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                ChessBallFigure n = board[nx][ny];
                if (n == ChessBallFigure.EMPTY || n == ChessBallFigure.BALL) continue;
                if (n.isWhite() != forWhite) continue;
                if (canPieceReach(board, n, bx, by, gx, gy)) return true;
            }
        }
        return false;
    }

    private static boolean canPieceReach(ChessBallFigure[][] board, ChessBallFigure piece,
                                         int sx, int sy, int gx, int gy) {
        switch (piece) {
            case WHITE_QUEEN: case BLACK_QUEEN:
                return canRookReach(board, sx, sy, gx, gy) || canBishopReach(board, sx, sy, gx, gy);
            case WHITE_ROOK: case BLACK_ROOK:
                return canRookReach(board, sx, sy, gx, gy);
            case WHITE_BISHOP: case BLACK_BISHOP:
                return canBishopReach(board, sx, sy, gx, gy);
            case WHITE_KNIGHT: case BLACK_KNIGHT: {
                int dx = Math.abs(gx - sx), dy = Math.abs(gy - sy);
                return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
            }
            case WHITE_KING: case BLACK_KING:
                return Math.abs(gx - sx) <= 1 && Math.abs(gy - sy) <= 1 && (gx != sx || gy != sy);
            case WHITE_PAWN: return gx == sx && gy == sy + 1;
            case BLACK_PAWN: return gx == sx && gy == sy - 1;
            default: return false;
        }
    }

    private static boolean canRookReach(ChessBallFigure[][] b, int sx, int sy, int gx, int gy) {
        return (sx == gx || sy == gy) && pathClear(b, sx, sy, gx, gy);
    }
    private static boolean canBishopReach(ChessBallFigure[][] b, int sx, int sy, int gx, int gy) {
        return Math.abs(gx - sx) == Math.abs(gy - sy) && pathClear(b, sx, sy, gx, gy);
    }
    private static boolean pathClear(ChessBallFigure[][] b, int sx, int sy, int gx, int gy) {
        int dx = Integer.signum(gx - sx), dy = Integer.signum(gy - sy);
        int x = sx + dx, y = sy + dy;
        while (x != gx || y != gy) {
            if (b[x][y] != ChessBallFigure.EMPTY) return false;
            x += dx; y += dy;
        }
        return true;
    }

    private static int openLane(ChessBallFigure[][] b, int bx, int by, int dir) {
        int empty = 0;
        int y = by + dir;
        while (y >= 0 && y < H && b[bx][y] == ChessBallFigure.EMPTY) {
            empty++;
            y += dir;
        }
        return empty;
    }
}
