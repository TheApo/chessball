package com.apogames.chessball.ai.algo;

import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.game.ChessBallBoard;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.enums.ChessBallFigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enumerates legal turns for a side. A <b>turn</b> is any sequence of up to
 * {@value #MAX_PASSES} ball passes interleaved with up to {@value #MAX_PIECE_MOVES}
 * piece move, in any order. Every reachable end-state is one turn variant.
 *
 * <p>Implementation: DFS over {@code (board, passesUsed, piecesUsed)} with do/undo
 * on a single working board (no per-step copies). End-states are deduped by stable
 * board hash so different action orderings reaching the same state count once.
 *
 * <p><b>Hard caps to keep the tree bounded</b> (without them DFS can blow up to
 * 50 000+ sequences in busy positions and the AI hangs):
 * <ul>
 *   <li>{@value #MAX_END_STATES} unique end-states max.</li>
 *   <li>{@value #MAX_PASS_BRANCH} pass targets per pass node (sorted by Δy progress).</li>
 *   <li>{@value #MAX_PIECE_BRANCH} piece moves per piece-move node (captures first,
 *       then by Manhattan distance to ball).</li>
 *   <li>Strict-backward passes skipped.</li>
 * </ul>
 */
public final class TurnGenerator {

    public static final int MAX_PASSES      = 3;
    public static final int MAX_PIECE_MOVES = 1;

    private static final int W = 9;
    private static final int H = 15;

    private static final int MAX_END_STATES   = 600;
    private static final int MAX_PASS_BRANCH  = 8;
    private static final int MAX_PIECE_BRANCH = 12;

    private TurnGenerator() {}

    /** Returns up to {@value #MAX_END_STATES} unique end-states reachable in one turn. */
    public static List<List<ChessBallStep>> generate(ChessBallFigure[][] board, boolean isWhiteToMove) {
        Context ctx = new Context();
        ctx.sim = new ChessBallBoard();
        ctx.isWhite = isWhiteToMove;
        ChessBallFigure[][] working = copy(board);
        List<ChessBallStep> sequence = new ArrayList<ChessBallStep>(4);
        explore(working, ctx, 0, 0, sequence);
        return new ArrayList<List<ChessBallStep>>(ctx.dedup.values());
    }

    private static final class Context {
        ChessBallBoard sim;
        boolean isWhite;
        Map<Long, List<ChessBallStep>> dedup = new HashMap<Long, List<ChessBallStep>>();
        boolean budgetExhausted() { return dedup.size() >= MAX_END_STATES; }
    }

    private static void explore(ChessBallFigure[][] board, Context ctx,
                                int passesUsed, int piecesUsed, List<ChessBallStep> sequence) {
        if (ctx.budgetExhausted()) return;

        if (!sequence.isEmpty()) {
            long h = hashBoard(board);
            if (!ctx.dedup.containsKey(h)) {
                ctx.dedup.put(h, new ArrayList<ChessBallStep>(sequence));
            }
            if (isTerminal(board)) return;
        }

        if (passesUsed < MAX_PASSES) {
            tryPasses(board, ctx, passesUsed, piecesUsed, sequence);
            if (ctx.budgetExhausted()) return;
        }
        if (piecesUsed < MAX_PIECE_MOVES) {
            tryPieceMoves(board, ctx, passesUsed, piecesUsed, sequence);
        }
    }

    // --- Ball passes ---------------------------------------------------------------

    private static void tryPasses(ChessBallFigure[][] board, Context ctx,
                                  int passesUsed, int piecesUsed, List<ChessBallStep> sequence) {
        int[] ball = findBall(board);
        if (ball == null || !hasFriendlyNeighbour(board, ball[0], ball[1], ctx.isWhite)) return;

        int forwardSign = ctx.isWhite ? +1 : -1;
        ctx.sim.setBoard(board);
        ctx.sim.setCurrentColor(ctx.isWhite ? ChessBallColor.WHITE : ChessBallColor.BLACK);
        ctx.sim.setPossibleStepsForPosition(ball[0], ball[1]);
        List<int[]> targets = collectAndRankPassTargets(ctx.sim, ball, forwardSign);

        for (int i = 0; i < targets.size(); i++) {
            if (ctx.budgetExhausted()) return;
            int tx = targets.get(i)[0];
            int ty = targets.get(i)[1];

            board[ball[0]][ball[1]] = ChessBallFigure.EMPTY;
            board[tx][ty]           = ChessBallFigure.BALL;
            sequence.add(new ChessBallStep(ball[0], ball[1], tx, ty));

            explore(board, ctx, passesUsed + 1, piecesUsed, sequence);

            sequence.remove(sequence.size() - 1);
            board[tx][ty]           = ChessBallFigure.EMPTY;
            board[ball[0]][ball[1]] = ChessBallFigure.BALL;
        }
    }

    /** Returns up to {@link #MAX_PASS_BRANCH} pass targets, sorted by forward-Δy desc.
     *  Strict-backward targets are dropped. */
    private static List<int[]> collectAndRankPassTargets(ChessBallBoard sim, int[] ball, final int forwardSign) {
        ChessBallFigure[][] c = sim.getCircles();
        List<int[]> raw = new ArrayList<int[]>();
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (c[x][y] == ChessBallFigure.EMPTY) continue;
                if ((y - ball[1]) * forwardSign < 0) continue; // skip backward
                raw.add(new int[]{x, y});
            }
        }
        final int by = ball[1];
        Collections.sort(raw, new Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                return Integer.compare((b[1] - by) * forwardSign, (a[1] - by) * forwardSign);
            }
        });
        if (raw.size() > MAX_PASS_BRANCH) return raw.subList(0, MAX_PASS_BRANCH);
        return raw;
    }

    // --- Piece moves ---------------------------------------------------------------

    private static void tryPieceMoves(ChessBallFigure[][] board, Context ctx,
                                      int passesUsed, int piecesUsed, List<ChessBallStep> sequence) {
        int[] ball = findBall(board);
        int bx = ball != null ? ball[0] : -1;
        int by = ball != null ? ball[1] : -1;

        // Collect ALL candidate piece moves, then rank+cap once across all pieces.
        List<int[]> candidates = new ArrayList<int[]>();
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                ChessBallFigure piece = board[x][y];
                if (piece == null || piece == ChessBallFigure.EMPTY || piece == ChessBallFigure.BALL) continue;
                if (piece.isWhite() != ctx.isWhite) continue;

                ctx.sim.setBoard(board);
                ctx.sim.setCurrentColor(ctx.isWhite ? ChessBallColor.WHITE : ChessBallColor.BLACK);
                ctx.sim.setPossibleStepsForPosition(x, y);
                ChessBallFigure[][] circles = ctx.sim.getCircles();
                for (int tx = 0; tx < W; tx++) {
                    for (int ty = 0; ty < H; ty++) {
                        if (circles[tx][ty] == ChessBallFigure.EMPTY) continue;
                        candidates.add(new int[]{x, y, tx, ty});
                    }
                }
            }
        }

        // Rank: captures first, then by closeness-to-ball (smaller Manhattan distance is better).
        final int fbx = bx, fby = by;
        final ChessBallFigure[][] fboard = board;
        Collections.sort(candidates, new Comparator<int[]>() {
            public int compare(int[] a, int[] b) {
                int aCap = score(a);
                int bCap = score(b);
                return Integer.compare(bCap, aCap);
            }
            private int score(int[] m) {
                ChessBallFigure target = fboard[m[2]][m[3]];
                int captureBonus = (target != ChessBallFigure.EMPTY && target != ChessBallFigure.BALL)
                        ? 100_000 + materialOf(target) : 0;
                int dist = Math.abs(m[2] - fbx) + Math.abs(m[3] - fby);
                int closer = -dist; // smaller = better
                return captureBonus + closer;
            }
        });
        int upTo = Math.min(MAX_PIECE_BRANCH, candidates.size());

        for (int i = 0; i < upTo; i++) {
            if (ctx.budgetExhausted()) return;
            int[] m = candidates.get(i);
            int x = m[0], y = m[1], tx = m[2], ty = m[3];
            ChessBallFigure piece = board[x][y];
            ChessBallFigure captured = board[tx][ty];

            board[x][y]   = ChessBallFigure.EMPTY;
            board[tx][ty] = piece;
            sequence.add(new ChessBallStep(x, y, tx, ty));

            explore(board, ctx, passesUsed, piecesUsed + 1, sequence);

            sequence.remove(sequence.size() - 1);
            board[tx][ty] = captured;
            board[x][y]   = piece;
        }
    }

    private static int materialOf(ChessBallFigure f) {
        switch (f) {
            case WHITE_QUEEN: case BLACK_QUEEN:   return 10_000;
            case WHITE_ROOK:  case BLACK_ROOK:    return 7_000;
            case WHITE_BISHOP:case BLACK_BISHOP:  return 5_000;
            case WHITE_KNIGHT:case BLACK_KNIGHT:  return 4_000;
            case WHITE_PAWN:  case BLACK_PAWN:    return 800;
            default: return 0;
        }
    }

    // --- Apply / helpers -----------------------------------------------------------

    public static ChessBallFigure[][] applyTurn(ChessBallFigure[][] board, List<ChessBallStep> turn) {
        ChessBallFigure[][] result = copy(board);
        for (ChessBallStep step : turn) {
            ChessBallFigure f = result[step.getFigureX()][step.getFigureY()];
            result[step.getFigureX()][step.getFigureY()] = ChessBallFigure.EMPTY;
            result[step.getStepFigureX()][step.getStepFigureY()] = f;
        }
        return result;
    }

    private static int[] findBall(ChessBallFigure[][] board) {
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                if (board[x][y] == ChessBallFigure.BALL) return new int[]{x, y};
            }
        }
        return null;
    }

    private static boolean hasFriendlyNeighbour(ChessBallFigure[][] board, int bx, int by, boolean isWhite) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue;
                int x = bx + dx, y = by + dy;
                if (x < 0 || x >= W || y < 0 || y >= H) continue;
                ChessBallFigure f = board[x][y];
                if (f == null || f == ChessBallFigure.EMPTY || f == ChessBallFigure.BALL) continue;
                if (f.isWhite() == isWhite) return true;
            }
        }
        return false;
    }

    private static boolean isTerminal(ChessBallFigure[][] board) {
        boolean wK = false, bK = false;
        int[] ball = null;
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                ChessBallFigure f = board[x][y];
                if (f == ChessBallFigure.WHITE_KING) wK = true;
                else if (f == ChessBallFigure.BLACK_KING) bK = true;
                else if (f == ChessBallFigure.BALL) ball = new int[]{x, y};
            }
        }
        if (!wK || !bK) return true;
        if (ball != null && ball[0] >= 3 && ball[0] <= 5 && (ball[1] == 0 || ball[1] == H - 1)) return true;
        return false;
    }

    static ChessBallFigure[][] copy(ChessBallFigure[][] src) {
        ChessBallFigure[][] dst = new ChessBallFigure[W][H];
        for (int x = 0; x < W; x++) {
            System.arraycopy(src[x], 0, dst[x], 0, H);
        }
        return dst;
    }

    /** FNV-1a-ish hash over board cells. */
    private static long hashBoard(ChessBallFigure[][] board) {
        long h = 1469598103934665603L;
        for (int x = 0; x < W; x++) {
            for (int y = 0; y < H; y++) {
                ChessBallFigure f = board[x][y];
                int v = (f == null) ? 0 : (f.ordinal() + 1);
                h ^= v;
                h *= 1099511628211L;
            }
        }
        return h;
    }
}
