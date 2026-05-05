package com.apogames.chessball.ai.algo;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.ChessBallAIInformations;
import com.apogames.chessball.ai.ChessBallPlayerAI;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Per-depth beam widths (index = remaining depth). [0]=leaf=unused.
     *  GWT-compiled HTML/JS runs each negamax node ~5-10× slower than the JVM,
     *  so we use much tighter beams there. The HTML values are tuned so that
     *  Hard's depth-3 negamax reliably FINISHES inside the 4 s budget on a mid
     *  laptop browser — without that, iterative deepening only completes depth 1
     *  and Hard plays worse than Easy. */
    private static final int[] BEAM      = {0, 12, 18, 28, 40};
    private static final int[] BEAM_HTML = {0,  4,  6, 10, 14};
    /** Negamax search beam at the root. */
    private static final int ROOT_BEAM      = 120;
    private static final int ROOT_BEAM_HTML = 25;

    private static int rootBeam() {
        return Constants.IS_HTML ? ROOT_BEAM_HTML : ROOT_BEAM;
    }
    private static int beamFor(int depth) {
        int[] b = Constants.IS_HTML ? BEAM_HTML : BEAM;
        return depth < b.length ? b[depth] : b[b.length - 1];
    }

    private final String name;
    private final int maxDepth;
    private final long timeBudgetMs;
    /** Set per-update from {@link ChessBallAIInformations#isBlack()}. The AI thinks
     *  in white-POV always; this flag tells {@link #describeTurn} whether to un-mirror
     *  coordinates so log lines match what the user sees on screen. */
    private boolean isBlack;

    protected AlphaBetaAI(String name, int maxDepth, long timeBudgetMs) {
        this.name = name;
        this.maxDepth = Math.max(1, maxDepth);
        this.timeBudgetMs = timeBudgetMs;
    }

    @Override public String getName() { return name; }

    @Override
    public List<ChessBallStep> update(ChessBallAIInformations info) {
        ChessBallFigure[][] board = info.getBoard();
        this.isBlack = info.isBlack();
        long deadline = System.currentTimeMillis() + timeBudgetMs;

        List<List<ChessBallStep>> rootTurns = TurnGenerator.generate(board, true);
        if (rootTurns.isEmpty()) return Collections.emptyList();

        // Static ranking of EVERY generated candidate — defense filter operates
        // on this full pool (so candidates that rank low statically but matter
        // defensively, e.g. king retreats, still get the safety check).
        List<RankedTurn> fullRanking = staticRank(board, rootTurns);

        // Negamax pool: top rootBeam() by static. Iterative deepening refines scores.
        int rootBeam = rootBeam();
        List<RankedTurn> negamaxRanking = fullRanking.size() > rootBeam
                ? new ArrayList<RankedTurn>(fullRanking.subList(0, rootBeam))
                : new ArrayList<RankedTurn>(fullRanking);

        for (int d = 2; d <= maxDepth; d++) {
            if (System.currentTimeMillis() >= deadline) break;
            List<RankedTurn> deeper = scoreRoot(board, negamaxRanking, d, deadline);
            if (deeper == null) break;
            negamaxRanking = deeper;
        }

        // Merge negamax-refined scores back into the full pool.
        Map<List<ChessBallStep>, Integer> ngmScore = new HashMap<List<ChessBallStep>, Integer>();
        for (RankedTurn rt : negamaxRanking) ngmScore.put(rt.turn, rt.score);
        List<RankedTurn> merged = new ArrayList<RankedTurn>(fullRanking.size());
        for (RankedTurn rt : fullRanking) {
            Integer ngm = ngmScore.get(rt.turn);
            merged.add(ngm != null ? new RankedTurn(rt.turn, ngm) : rt);
        }
        sortDesc(merged);

        merged = applyDefensePenalty(board, merged);
        return pickFromRanking(board, merged);
    }

    /**
     * For each top candidate, ask "can the opponent score (or capture our king) in
     * their next turn?" — if yes, subtract {@link Evaluator#GOAL} from the candidate's
     * score. After re-sorting, safe candidates rank above unsafe ones, so every
     * difficulty (even Easy's probabilistic pick) prefers safe moves.
     *
     * <p>Subclasses tune via {@link #defenseCheckMs()} and {@link #defenseMaxChecked()}.
     */
    protected List<RankedTurn> applyDefensePenalty(ChessBallFigure[][] board, List<RankedTurn> ranking) {
        int n = Math.min(defenseMaxChecked(), ranking.size());
        if (n <= 0) return ranking;
        List<RankedTurn> out = new ArrayList<RankedTurn>(ranking);
        long perCheckMs = defenseCheckMs();
        long totalDeadline = System.currentTimeMillis() + 10_000L;

        // BASELINE: total MATERIAL of our valuable pieces the opponent can capture
        // with the board AS IT IS NOW (before our move). A candidate that doesn't
        // increase this baseline doesn't deserve a hang penalty — that hang already
        // existed and isn't this move's fault. Material-weighted so hanging the
        // queen (10 000) hurts more than hanging a knight (4 000).
        int hangBefore = TurnGenerator.sumHangingValuableMaterial(board, false);

        Gdx.app.log("AI-Defense",
                getName() + ": " + ranking.size() + " candidates, baseline hang material = "
                + hangBefore + " (perCheckMs " + perCheckMs + ", 10 s wall cap)");

        int checkedCount = 0;
        int unsafeCount = 0;
        int newHangCount = 0;
        int fixedHangCount = 0;
        for (int i = 0; i < n; i++) {
            if (System.currentTimeMillis() >= totalDeadline) {
                Gdx.app.log("AI-Defense", "  ...total budget exhausted after " + checkedCount + " checks");
                break;
            }
            RankedTurn rt = out.get(i);
            ChessBallFigure[][] after = TurnGenerator.applyTurn(board, rt.turn);

            // Already-winning terminal: ball in our goal or opponent's king captured.
            // The game ends — defensive concerns are moot. Without this guard, hang/unsafe
            // penalties would push terminal-winning moves below GOAL, breaking the
            // "always take an immediate goal" override in the difficulty pickers.
            int afterEval = Evaluator.evaluate(after);
            if (afterEval >= Evaluator.GOAL) {
                checkedCount++;
                continue;
            }

            boolean unsafe = TurnGenerator.canScoreInOneTurn(after, false, perCheckMs);
            int hangAfter = TurnGenerator.sumHangingValuableMaterial(after, false);
            int hangDelta = hangAfter - hangBefore;
            checkedCount++;
            if (unsafe) unsafeCount++;
            if (hangDelta > 0) newHangCount++;
            else if (hangDelta < 0) fixedHangCount++;

            int newScore = rt.score;
            if (unsafe) {
                newScore = Math.min(newScore, -Evaluator.GOAL);
            }
            // Subtract the material delta directly: hanging a queen costs 10 000.
            newScore -= hangDelta;
            if (newScore != rt.score) {
                out.set(i, new RankedTurn(rt.turn, newScore));
            }
        }
        sortDesc(out);
        Gdx.app.log("AI-Defense",
                getName() + ": " + checkedCount + " checked, " + unsafeCount + " unsafe, "
                + newHangCount + " creates-new-hang, " + fixedHangCount + " fixes-hang. Top1 "
                + describeTurn(out.get(0).turn, isBlack) + " (final score " + out.get(0).score + ")");
        // Per-term breakdown of top-3 candidates so we can see WHY a move was picked.
        int dump = Math.min(3, out.size());
        for (int i = 0; i < dump; i++) {
            RankedTurn rt = out.get(i);
            ChessBallFigure[][] after = TurnGenerator.applyTurn(board, rt.turn);
            Gdx.app.log("AI-Eval",
                    "  #" + (i + 1) + " " + describeTurn(rt.turn, isBlack)
                    + " score=" + rt.score + " | " + Evaluator.describeBreakdown(after));
        }
        return out;
    }

    /** Format a turn for logging in REAL-board coordinates. AI internals always
     *  treat the AI as white, so when the AI plays black the on-screen coords
     *  are mirrored x↔(W-1-x), y↔(H-1-y); we un-mirror here so log lines match
     *  what the user sees. */
    private static String describeTurn(List<ChessBallStep> turn, boolean mirrored) {
        if (turn == null || turn.isEmpty()) return "<empty>";
        StringBuilder sb = new StringBuilder();
        int w = 9, h = 15;
        for (int i = 0; i < turn.size(); i++) {
            ChessBallStep s = turn.get(i);
            int fx = s.getFigureX(), fy = s.getFigureY();
            int tx = s.getStepFigureX(), ty = s.getStepFigureY();
            if (mirrored) {
                fx = w - 1 - fx; fy = h - 1 - fy;
                tx = w - 1 - tx; ty = h - 1 - ty;
            }
            if (i > 0) sb.append(' ');
            sb.append('(').append(fx).append(',').append(fy).append(")->(")
              .append(tx).append(',').append(ty).append(')');
        }
        return sb.toString();
    }

    /** Per-candidate budget for the defense check. Override per difficulty. */
    protected long defenseCheckMs()    { return 80L; }

    /** Number of top candidates to defense-check. Override per difficulty. */
    protected int  defenseMaxChecked() { return 8; }

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
        int beam = beamFor(depth);
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

    /** Difficulty hook: choose a turn from the final ranking (descending sorted).
     *  {@code board} is the position before the AI's move so subclasses can run
     *  additional checks (e.g. opponent-scoring lookahead) per candidate. */
    protected abstract List<ChessBallStep> pickFromRanking(ChessBallFigure[][] board, List<RankedTurn> ranking);

    /** True if rank[0] is decisively better than rank[1] — forces a mistake-free pick. */
    protected static boolean isCritical(List<RankedTurn> ranking) {
        if (ranking.size() < 2) return true;
        return ranking.get(0).score - ranking.get(1).score >= CRITICAL_GAP;
    }
}
