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
    private static final int W_NEAR_BALL     = 1_000;    // per friendly adjacent to ball (vs. opp adj)
    private static final int W_BALL_REACHABLE = 500;     // per non-adj piece that can move adjacent to ball
    private static final int BALL_REACH_CAP   = 2;       // max counted reachers per side
    private static final int W_KING_THREAT   = 50_000;   // own king attackable by enemy in 1 chess move
    private static final int W_OPEN_LANE     = 200;      // per empty cell in ball's column ahead

    private Evaluator() {}

    /** Same maths as {@link #evaluate(ChessBallFigure[][])} but returns a human-readable
     *  per-term breakdown for diagnostics. Slow — only call from logging paths. */
    public static String describeBreakdown(ChessBallFigure[][] board) {
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
        if (!whiteKing) return "TERMINAL: white king captured (-GOAL)";
        if (!blackKing) return "TERMINAL: black king captured (+GOAL)";
        if (by == OWN_GOAL_Y && bx >= 3 && bx <= 5) return "TERMINAL: white scored (+GOAL)";
        if (by == ENEMY_GOAL_Y && bx >= 3 && bx <= 5) return "TERMINAL: black scored (-GOAL)";
        if (bx < 0) return "material=" + materialBalance + " (no ball)";

        int shotW = W_SHOT_AT_GOAL * shotPotential(board, bx, by, true);
        int shotB = W_SHOT_AT_GOAL * shotPotential(board, bx, by, false);
        int progress = (by - 7) * W_BALL_PROGRESS;
        int nearGoal = 0;
        if (by >= 12) nearGoal = W_NEAR_GOAL * (by - 11);
        if (by <= 2)  nearGoal = -W_NEAR_GOAL * (3 - by);
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
        int nearBall = W_NEAR_BALL * (wAdj - bAdj);
        int wReach = Math.min(BALL_REACH_CAP, countCanMoveAdjacentToBall(board, bx, by, true));
        int bReach = Math.min(BALL_REACH_CAP, countCanMoveAdjacentToBall(board, bx, by, false));
        int reach = W_BALL_REACHABLE * (wReach - bReach);
        boolean wKingAtt = isKingAttacked(board, true);
        boolean bKingAtt = isKingAttacked(board, false);
        int kingThreat = (bKingAtt ? W_KING_THREAT : 0) - (wKingAtt ? W_KING_THREAT : 0);
        int lane = W_OPEN_LANE * (openLane(board, bx, by, +1) - openLane(board, bx, by, -1));
        int total = materialBalance + shotW - shotB + progress + nearGoal + nearBall + reach + kingThreat + lane;
        return "material=" + materialBalance
                + " shot(w=" + (shotW/W_SHOT_AT_GOAL) + ",b=" + (shotB/W_SHOT_AT_GOAL) + ")=" + (shotW - shotB)
                + " progress=" + progress
                + " nearGoal=" + nearGoal
                + " nearBall(w=" + wAdj + ",b=" + bAdj + ")=" + nearBall
                + " reach(w=" + wReach + ",b=" + bReach + ")=" + reach
                + " kingThreat(wAtt=" + wKingAtt + ",bAtt=" + bKingAtt + ")=" + kingThreat
                + " lane=" + lane
                + " | total=" + total;
    }

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

        // Ball reach: pieces that can MOVE adjacent to the ball next turn (not already adjacent).
        // Counts pieces that aren't yet next to the ball but could be after one chess move —
        // captures the "ball is safe iff opponent must first move a piece to reach it" intuition.
        int wReach = Math.min(BALL_REACH_CAP, countCanMoveAdjacentToBall(board, bx, by, true));
        int bReach = Math.min(BALL_REACH_CAP, countCanMoveAdjacentToBall(board, bx, by, false));
        score += W_BALL_REACHABLE * (wReach - bReach);

        // King safety: any enemy piece that can chess-move onto our king's square is a 1-ply
        // capture threat. The negamax+1-turn defense filter only sees this if the king-capture
        // is the OPPONENT'S immediate next move. A 2-turn threat (e.g. clear blocker, then
        // capture) is missed at depth 3, so we add a strong static penalty here so leaving the
        // king attacked is always expensive — defending becomes the obvious choice unless a
        // forced win is already on the board.
        if (isKingAttacked(board, true))  score -= W_KING_THREAT;
        if (isKingAttacked(board, false)) score += W_KING_THREAT;

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

    /** True if any enemy piece could chess-move onto the given side's king square in
     *  one move. Returns false if the king is already gone (the terminal branch in
     *  evaluate() handles that). Early-exits on the first found attacker. */
    private static boolean isKingAttacked(ChessBallFigure[][] board, boolean forWhiteKing) {
        ChessBallFigure kingType = forWhiteKing ? ChessBallFigure.WHITE_KING : ChessBallFigure.BLACK_KING;
        int kx = -1, ky = -1;
        for (int x = 0; x < W && kx < 0; x++) {
            for (int y = 0; y < H; y++) {
                if (board[x][y] == kingType) { kx = x; ky = y; break; }
            }
        }
        if (kx < 0) return false;
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                ChessBallFigure f = board[x][y];
                if (f == null || f == ChessBallFigure.EMPTY || f == ChessBallFigure.BALL) continue;
                if (f.isWhite() == forWhiteKing) continue;
                if (canPieceReach(board, f, x, y, kx, ky)) return true;
            }
        }
        return false;
    }

    /** Counts pieces of the given color that are NOT already adjacent to the ball
     *  but could move to one of the 8 adjacent cells in one chess move. Early-exits
     *  once {@link #BALL_REACH_CAP} matches are found (the score caps anyway). */
    private static int countCanMoveAdjacentToBall(ChessBallFigure[][] board, int bx, int by, boolean forWhite) {
        int count = 0;
        for (int x = 0; x < W && count < BALL_REACH_CAP; x++) {
            for (int y = 0; y < H && count < BALL_REACH_CAP; y++) {
                ChessBallFigure f = board[x][y];
                if (f == null || f == ChessBallFigure.EMPTY || f == ChessBallFigure.BALL) continue;
                if (f.isWhite() != forWhite) continue;
                // Already adjacent — handled by W_NEAR_BALL, don't double-count.
                if (Math.abs(x - bx) <= 1 && Math.abs(y - by) <= 1) continue;
                if (canMoveToAdjacentCell(board, f, x, y, bx, by)) count++;
            }
        }
        return count;
    }

    /** True if {@code piece} at {@code (sx,sy)} can chess-move into any of the 8 cells
     *  surrounding the ball — the destination must be empty or hold a capturable enemy
     *  (not the ball itself, and not a same-color piece). */
    private static boolean canMoveToAdjacentCell(ChessBallFigure[][] board, ChessBallFigure piece,
                                                  int sx, int sy, int bx, int by) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int gx = bx + dx, gy = by + dy;
                if (gx < 0 || gx >= W || gy < 0 || gy >= H) continue;
                ChessBallFigure target = board[gx][gy];
                if (target == ChessBallFigure.BALL) continue;
                if (target != null && target != ChessBallFigure.EMPTY
                        && target.isWhite() == piece.isWhite()) continue;
                if (canPieceReach(board, piece, sx, sy, gx, gy)) return true;
            }
        }
        return false;
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
