package com.apogames.chessball.solver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure-Java puzzle solvability checker. Given a 137-char level string
 * (135 cells + turn digit + status digit), reports whether the side-to-move
 * can score in the allowed budget (default: 1 piece move + 3 ball passes).
 *
 * Encoding mirrors {@code ChessBallFigure.puzzleLevel}:
 * '0' empty, '1'..'6' WK,WQ,WB,WN,WR,WP, '7'..'c' BK,BQ,BB,BN,BR,BP, 'd' ball.
 *
 * Movement / pass rules ported from {@code ChessBallBoard.addCircle} and
 * {@code setPossibleStepsForPosition}; no libGDX dependency so it runs from
 * unit tests, gradle tasks, or in-game during random-level validation.
 */
public final class PuzzleSolver {

    public static final int W = 9;
    public static final int H = 15;
    public static final int DEFAULT_MAX_MOVES = 1;
    public static final int DEFAULT_MAX_PASSES = 3;

    private PuzzleSolver() {}

    /** A single solver step in the solution path. {@link #isPass} == true means the ball
     *  was passed; otherwise a piece moved. {@link #figure} carries the figure char that
     *  acted (for passes that's always {@code 'd'}). */
    public static final class Step {
        public final boolean isPass;
        public final char figure;
        public final int fromX, fromY, toX, toY;
        public Step(boolean isPass, char figure, int fx, int fy, int tx, int ty) {
            this.isPass = isPass; this.figure = figure;
            this.fromX = fx; this.fromY = fy; this.toX = tx; this.toY = ty;
        }
    }

    /** Convenience: 1 piece move + 3 ball passes (the in-game puzzle budget). */
    public static boolean isSolvable(String levelString) {
        return isSolvable(levelString, DEFAULT_MAX_MOVES, DEFAULT_MAX_PASSES);
    }

    public static boolean isSolvable(String levelString, int maxMoves, int maxPasses) {
        return solveLevel(levelString, maxMoves, maxPasses) != null;
    }

    /** Returns the action sequence that solves the level, or {@code null} if no
     *  solution exists within the given budget. */
    public static List<Step> solveLevel(String levelString) {
        return solveLevel(levelString, DEFAULT_MAX_MOVES, DEFAULT_MAX_PASSES);
    }

    public static List<Step> solveLevel(String levelString, int maxMoves, int maxPasses) {
        if (levelString == null || levelString.length() < W * H + 1) return null;
        char[][] board = parseBoard(levelString);
        boolean blackToMove = levelString.charAt(W * H) == '1';
        List<Step> path = new ArrayList<>();
        if (search(board, blackToMove, maxMoves, maxPasses, new HashSet<>(), path)) return path;
        return null;
    }

    /** Human-readable German label for a figure char (used by formatPath()). */
    public static String figureLabel(char c) {
        switch (c) {
            case '1': case '7': return "Koenig";
            case '2': case '8': return "Dame";
            case '3': case '9': return "Laeufer";
            case '4': case 'a': return "Springer";
            case '5': case 'b': return "Turm";
            case '6': case 'c': return "Bauer";
            case 'd':           return "Ball";
            default:            return "?";
        }
    }

    /** "1. Figur Dame von (4, 9) nach (1, 6)" / "2. Ball von (5, 6) nach (5, 5)" — one
     *  line per step, joined with newlines. Empty string if path is null/empty. */
    public static String formatPath(List<Step> path) {
        if (path == null || path.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            Step s = path.get(i);
            if (i > 0) sb.append('\n');
            sb.append(i + 1).append(". ");
            if (s.isPass) {
                sb.append("Ball von (").append(s.fromX).append(", ").append(s.fromY)
                  .append(") nach (").append(s.toX).append(", ").append(s.toY).append(")");
            } else {
                sb.append("Figur ").append(figureLabel(s.figure))
                  .append(" von (").append(s.fromX).append(", ").append(s.fromY)
                  .append(") nach (").append(s.toX).append(", ").append(s.toY).append(")");
            }
        }
        return sb.toString();
    }

    public static char[][] parseBoard(String levelString) {
        char[][] b = new char[W][H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                b[x][y] = levelString.charAt(y * W + x);
            }
        }
        return b;
    }

    // --- Figure helpers --------------------------------------------------------

    private static boolean isWhite(char c)     { return c >= '1' && c <= '6'; }
    private static boolean isBlack(char c)     { return c >= '7' && c <= 'c'; }
    private static boolean isKing(char c)      { return c == '1' || c == '7'; }
    private static boolean isQueen(char c)     { return c == '2' || c == '8'; }
    private static boolean isBishop(char c)    { return c == '3' || c == '9'; }
    private static boolean isKnight(char c)    { return c == '4' || c == 'a'; }
    private static boolean isRook(char c)      { return c == '5' || c == 'b'; }
    private static boolean isWhitePawn(char c) { return c == '6'; }
    private static boolean isBlackPawn(char c) { return c == 'c'; }
    private static boolean isPawn(char c)      { return c == '6' || c == 'c'; }
    private static boolean isOwnPiece(char c, boolean blackToMove) {
        return blackToMove ? isBlack(c) : isWhite(c);
    }

    private static boolean canGoCell(int x, int y) {
        if (y == 0 || y == H - 1) return x >= 3 && x <= 5;
        return true;
    }

    // --- Search -----------------------------------------------------------------

    public static boolean solve(char[][] startBoard, boolean blackToMove,
                                int maxMoves, int maxPasses) {
        return search(startBoard, blackToMove, maxMoves, maxPasses, new HashSet<>(), null);
    }

    /**
     * Interleaved search: at each step the player may either move a piece (if
     * {@code movesLeft > 0}) or pass the ball (if {@code passesLeft > 0}). Mirrors the
     * in-game turn loop where {@code isOneStepPossible} only ends the turn when both
     * counters hit zero (or no passes are reachable). Memoised on
     * (board, movesLeft, passesLeft). When {@code path != null}, accumulates the
     * action sequence on the success branch.
     */
    private static boolean search(char[][] board, boolean blackToMove,
                                  int movesLeft, int passesLeft,
                                  Set<String> visited, List<Step> path) {
        if (isGoal(board, blackToMove)) return true;
        if (movesLeft <= 0 && passesLeft <= 0) return false;

        String key = boardKey(board, movesLeft, passesLeft);
        if (!visited.add(key)) return false;

        if (movesLeft > 0) {
            for (int sx = 0; sx < W; sx++) {
                for (int sy = 0; sy < H; sy++) {
                    char fig = board[sx][sy];
                    if (!isOwnPiece(fig, blackToMove)) continue;
                    for (int[] target : pieceTargets(board, sx, sy)) {
                        char[][] next = applyPieceMove(board, sx, sy, target[0], target[1]);
                        if (path != null) path.add(new Step(false, fig, sx, sy, target[0], target[1]));
                        if (search(next, blackToMove, movesLeft - 1, passesLeft, visited, path)) return true;
                        if (path != null) path.remove(path.size() - 1);
                    }
                }
            }
        }

        if (passesLeft > 0) {
            int[] ball = findBall(board);
            if (ball != null) {
                for (int[] t : ballPassTargets(board, blackToMove, ball[0], ball[1])) {
                    char[][] next = copyBoard(board);
                    next[ball[0]][ball[1]] = '0';
                    next[t[0]][t[1]] = 'd';
                    if (path != null) path.add(new Step(true, 'd', ball[0], ball[1], t[0], t[1]));
                    if (search(next, blackToMove, movesLeft, passesLeft - 1, visited, path)) return true;
                    if (path != null) path.remove(path.size() - 1);
                }
            }
        }

        return false;
    }

    private static boolean isGoal(char[][] board, boolean blackToMove) {
        int goalY = blackToMove ? 0 : H - 1;
        for (int x = 3; x <= 5; x++) {
            if (board[x][goalY] == 'd') return true;
        }
        return false;
    }

    private static int[] findBall(char[][] board) {
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (board[x][y] == 'd') return new int[]{x, y};
            }
        }
        return null;
    }

    private static char[][] copyBoard(char[][] b) {
        char[][] r = new char[W][H];
        for (int x = 0; x < W; x++) System.arraycopy(b[x], 0, r[x], 0, H);
        return r;
    }

    private static String boardKey(char[][] b, int movesLeft, int passesLeft) {
        StringBuilder sb = new StringBuilder(W * H + 6);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) sb.append(b[x][y]);
        }
        sb.append('|').append(movesLeft).append('|').append(passesLeft);
        return sb.toString();
    }

    private static char[][] applyPieceMove(char[][] b, int sx, int sy, int tx, int ty) {
        char[][] r = copyBoard(b);
        char piece = r[sx][sy];
        r[sx][sy] = '0';
        r[tx][ty] = piece;
        // Pawn promotion (one rank before the goal — matches checkToSetFigure).
        if (piece == '6' && ty == H - 2) r[tx][ty] = '2';
        if (piece == 'c' && ty == 1)     r[tx][ty] = '8';
        return r;
    }

    // --- Move generation --------------------------------------------------------

    /** Targets for moving the piece at (sx, sy). bYellow=true (piece movement). */
    static List<int[]> pieceTargets(char[][] board, int sx, int sy) {
        char fig = board[sx][sy];
        List<int[]> out = new ArrayList<>();
        addPatternTargets(board, fig, sx, sy, out, true);
        return out;
    }

    /** Ball-pass targets: for each friendly neighbor, extend the ball using that
     *  piece's pattern with bYellow=false. De-duplicated. */
    static List<int[]> ballPassTargets(char[][] board, boolean blackToMove, int bx, int by) {
        Set<Long> seen = new HashSet<>();
        List<int[]> out = new ArrayList<>();
        int[][] dirs = {{-1,-1},{0,-1},{1,-1},{-1,0},{1,0},{-1,1},{0,1},{1,1}};
        for (int[] d : dirs) {
            int nx = bx + d[0], ny = by + d[1];
            if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
            char neighbor = board[nx][ny];
            boolean friendly = blackToMove ? isBlack(neighbor) : isWhite(neighbor);
            if (!friendly) continue;
            List<int[]> targets = new ArrayList<>();
            addPatternTargets(board, neighbor, bx, by, targets, false);
            for (int[] t : targets) {
                long key = (long) t[0] * 100 + t[1];
                if (seen.add(key)) out.add(t);
            }
        }
        return out;
    }

    /** Dispatch by piece kind; matches the order/conditions of
     *  {@code ChessBallBoard.setPossibleStepsForPosition(figure, bYellow)}. */
    private static void addPatternTargets(char[][] board, char fig, int sx, int sy,
                                          List<int[]> out, boolean bYellow) {
        if (isBishop(fig) || isQueen(fig) || isKing(fig)) {
            boolean bNext = !isKing(fig);
            walkDirection(board, fig, sx, sy,  1,  1, bNext, bYellow, out);
            walkDirection(board, fig, sx, sy, -1, -1, bNext, bYellow, out);
            walkDirection(board, fig, sx, sy,  1, -1, bNext, bYellow, out);
            walkDirection(board, fig, sx, sy, -1,  1, bNext, bYellow, out);
        }
        if (isRook(fig) || isQueen(fig) || isKing(fig)) {
            boolean bNext = !isKing(fig);
            walkDirection(board, fig, sx, sy,  1,  0, bNext, bYellow, out);
            walkDirection(board, fig, sx, sy, -1,  0, bNext, bYellow, out);
            walkDirection(board, fig, sx, sy,  0,  1, bNext, bYellow, out);
            walkDirection(board, fig, sx, sy,  0, -1, bNext, bYellow, out);
        }
        if (isKnight(fig)) {
            int[][] L = {{1,-2},{1,2},{-1,-2},{-1,2},{2,-1},{2,1},{-2,-1},{-2,1}};
            for (int[] d : L) walkDirection(board, fig, sx, sy, d[0], d[1], false, bYellow, out);
        }
        if (isPawn(fig)) {
            int dy = isWhitePawn(fig) ? 1 : -1;
            // Pawn forward: addCircle then post-check removes if non-empty (so net: only empty).
            // walkDirection already only places when EMPTY for bYellow=false (ball pass) and
            // for bYellow=true the post-check below mirrors the original removal.
            int beforeForward = out.size();
            walkDirection(board, fig, sx, sy, 0, dy, false, bYellow, out);
            if (bYellow && sy + dy >= 0 && sy + dy < H && board[sx][sy + dy] != '0') {
                trimAfter(out, beforeForward, sx, sy + dy);
            }
            // Diagonal captures: original adds via addCircle then strips if EMPTY. Only enabled
            // for bYellow=true (piece movement). For ball pass with pawn pattern: forward only.
            if (bYellow) {
                addPawnDiagonal(board, fig, sx, sy, -1, dy, out);
                addPawnDiagonal(board, fig, sx, sy,  1, dy, out);
            }
        }
    }

    /** Adds the diagonal capture target only if it actually is an enemy piece
     *  (matches addCircle + post-strip-if-empty for pawns). */
    private static void addPawnDiagonal(char[][] board, char fig, int sx, int sy,
                                        int dx, int dy, List<int[]> out) {
        int tx = sx + dx, ty = sy + dy;
        if (tx < 0 || tx >= W || ty < 0 || ty >= H) return;
        char target = board[tx][ty];
        if (target == '0' || target == 'd') return;
        if (isWhite(target) == isWhite(fig)) return;
        if ((ty == 0 || ty == H - 1) && !canGoCell(tx, ty)) return;
        out.add(new int[]{tx, ty});
    }

    /** Removes any entry from {@code out} (after index {@code from}) at (x, y). */
    private static void trimAfter(List<int[]> out, int from, int x, int y) {
        for (int i = out.size() - 1; i >= from; i--) {
            int[] e = out.get(i);
            if (e[0] == x && e[1] == y) out.remove(i);
        }
    }

    /**
     * Recursive directional walk — line-of-sight movement with the same goal-row
     * (canGo) and bYellow gating as ChessBallBoard.addCircle.
     */
    private static void walkDirection(char[][] board, char figure, int sx, int sy,
                                      int dx, int dy, boolean bNext, boolean bYellow,
                                      List<int[]> out) {
        int tx = sx + dx, ty = sy + dy;
        if (tx < 0 || tx >= W || ty < 0 || ty >= H) return;

        boolean kingOrBall = isKing(figure) || figure == 'd';
        if (!kingOrBall) {
            if (ty < 1 || ty > H - 2) {  // y == 0 or y == 14 (goal rows)
                boolean isGo = canGoCell(tx, ty);
                char target = board[tx][ty];
                if (isGo && bYellow && target == '0') return;
                if (isGo && !bYellow && target != '0') return;
                if (!isGo) return;
            }
        } else {
            if (!canGoCell(tx, ty)) return;
        }

        char target = board[tx][ty];
        boolean diff = isWhite(target) != isWhite(figure);
        boolean placeable = (target == '0') || (diff && bYellow && target != 'd');
        if (!placeable) return;

        out.add(new int[]{tx, ty});

        if (bNext && target == '0') {
            walkDirection(board, figure, tx, ty, dx, dy, bNext, bYellow, out);
        }
    }
}
