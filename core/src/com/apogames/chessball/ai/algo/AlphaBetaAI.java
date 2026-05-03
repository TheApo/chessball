package com.apogames.chessball.ai.algo;

import com.apogames.chessball.ai.ChessBallAIInformations;
import com.apogames.chessball.ai.ChessBallPlayerAI;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.game.enums.ChessBallFigure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Real negamax with alpha-beta pruning + iterative deepening.
 *
 * <p>Move ordering by static evaluation at every node — this is what makes the
 * α-β cutoffs actually fire. A safety beam (per-depth cap on candidates examined)
 * keeps the search inside the time budget. Goal-scoring and goal-blocking turns
 * always rank near ±{@link Evaluator#GOAL} and survive any sane beam.
 *
 * <p>Difficulty subclasses choose from the final root ranking via
 * {@link #pickFromRanking(List)}.
 */
public abstract class AlphaBetaAI extends ChessBallPlayerAI {

    /** A turn paired with its score from AI POV (positive = good for AI). */
    public static final class RankedTurn {
        public final List<ChessBallStep> turn;
        public final int score;
        public RankedTurn(List<ChessBallStep> turn, int score) {
            this.turn = turn;
            this.score = score;
        }
    }

    protected static final Random RNG = new Random();
    /** Score gap above which "best" is forced (no mistakes injected). 30k ≈ a knight. */
    protected static final int CRITICAL_GAP = 30_000;

    /** Per-depth beam widths (index = remaining depth). [0]=leaf=unused. */
    private static final int[] BEAM = {0, 12, 18, 28, 40};
    private static final int ROOT_BEAM = 50;

    private final String name;
    private final int maxDepth;
    private final long timeBudgetMs;

    protected AlphaBetaAI(String name, int maxDepth, long timeBudgetMs) {
        this.name = name;
        this.maxDepth = Math.max(1, maxDepth);
        this.timeBudgetMs = timeBudgetMs;
    }

    @Override public String getName() { return name; }

    @Override
    public List<ChessBallStep> update(ChessBallAIInformations info) {
        ChessBallFigure[][] board = info.getBoard();
        long deadline = System.currentTimeMillis() + timeBudgetMs;

        List<List<ChessBallStep>> rootTurns = TurnGenerator.generate(board, true);
        if (rootTurns.isEmpty()) return Collections.emptyList();

        // Initial static ranking — used both as iterative-deepening seed and as
        // the fallback if every deeper iteration times out.
        List<RankedTurn> ranking = staticRank(board, rootTurns);
        if (ranking.size() > ROOT_BEAM) ranking = ranking.subList(0, ROOT_BEAM);

        // Iterative deepening: try depths 2, 3, ..., maxDepth — keep last completed.
        for (int d = 2; d <= maxDepth; d++) {
            if (System.currentTimeMillis() >= deadline) break;
            List<RankedTurn> deeper = scoreRoot(board, ranking, d, deadline);
            if (deeper == null) break; // mid-iteration timeout — keep previous ranking
            ranking = deeper;
        }
        return pickFromRanking(ranking);
    }

    /** Score every root turn at full {@code depth}. Returns null on mid-iteration timeout. */
    private List<RankedTurn> scoreRoot(ChessBallFigure[][] board, List<RankedTurn> ordered,
                                       int depth, long deadline) {
        List<RankedTurn> out = new ArrayList<RankedTurn>(ordered.size());
        int alpha = -Evaluator.GOAL * 2;
        int beta  =  Evaluator.GOAL * 2;
        for (RankedTurn rt : ordered) {
            if (System.currentTimeMillis() >= deadline) return null;
            ChessBallFigure[][] after = TurnGenerator.applyTurn(board, rt.turn);
            // Negamax convention: child is opponent (color = -1), so we negate the result.
            int v = -negamax(after, depth - 1, -beta, -alpha, false, deadline);
            out.add(new RankedTurn(rt.turn, v));
            if (v > alpha) alpha = v;
        }
        sortDesc(out);
        return out;
    }

    /**
     * Negamax with alpha-beta. {@code isMyTurn} flag tracks whose pieces move at this
     * node; the eval is always from AI POV (white), so negamax negates between plies.
     */
    private int negamax(ChessBallFigure[][] board, int depth, int alpha, int beta,
                        boolean isMyTurn, long deadline) {
        int eval = Evaluator.evaluate(board);
        if (eval >= Evaluator.GOAL || eval <= -Evaluator.GOAL) {
            return isMyTurn ? eval : -eval;
        }
        if (depth == 0 || System.currentTimeMillis() >= deadline) {
            return isMyTurn ? eval : -eval;
        }

        List<List<ChessBallStep>> turns = TurnGenerator.generate(board, isMyTurn);
        if (turns.isEmpty()) {
            return isMyTurn ? eval : -eval;
        }

        // Move ordering: rank by static eval after applying. For my turn we want
        // descending (best for me first); for opponent's turn we want descending
        // FROM OPPONENT POV = ascending from AI POV.
        List<RankedTurn> ordered = staticRank(board, turns);
        if (!isMyTurn) Collections.reverse(ordered);
        int beam = depth < BEAM.length ? BEAM[depth] : BEAM[BEAM.length - 1];
        int upTo = Math.min(beam, ordered.size());

        int best = -Evaluator.GOAL * 2;
        for (int i = 0; i < upTo; i++) {
            ChessBallFigure[][] after = TurnGenerator.applyTurn(board, ordered.get(i).turn);
            int v = -negamax(after, depth - 1, -beta, -alpha, !isMyTurn, deadline);
            if (v > best) best = v;
            if (best > alpha) alpha = best;
            if (alpha >= beta) break;
        }
        return best;
    }

    /** Static-eval ranking (descending = best for AI first). */
    private static List<RankedTurn> staticRank(ChessBallFigure[][] board,
                                               List<List<ChessBallStep>> turns) {
        List<RankedTurn> out = new ArrayList<RankedTurn>(turns.size());
        for (List<ChessBallStep> t : turns) {
            ChessBallFigure[][] after = TurnGenerator.applyTurn(board, t);
            out.add(new RankedTurn(t, Evaluator.evaluate(after)));
        }
        sortDesc(out);
        return out;
    }

    private static void sortDesc(List<RankedTurn> ranking) {
        Collections.sort(ranking, new Comparator<RankedTurn>() {
            public int compare(RankedTurn a, RankedTurn b) {
                return Integer.compare(b.score, a.score);
            }
        });
    }

    /** Difficulty hook: choose a turn from the final ranking (descending sorted). */
    protected abstract List<ChessBallStep> pickFromRanking(List<RankedTurn> ranking);

    /** True if rank[0] is decisively better than rank[1] — forces a mistake-free pick. */
    protected static boolean isCritical(List<RankedTurn> ranking) {
        if (ranking.size() < 2) return true;
        return ranking.get(0).score - ranking.get(1).score >= CRITICAL_GAP;
    }
}
