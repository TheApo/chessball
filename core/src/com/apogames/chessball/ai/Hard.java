package com.apogames.chessball.ai;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.algo.AlphaBetaAI;
import com.apogames.chessball.ai.algo.Evaluator;
import com.apogames.chessball.ai.algo.TranspositionTable;
import com.apogames.chessball.ai.algo.TurnGenerator;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

import java.util.Collections;
import java.util.List;

/**
 * Hard AI — depth-3 negamax + α-β with iterative deepening, plus the base
 * {@link AlphaBetaAI#applyDefensePenalty} step which already pushes unsafe
 * candidates below safe ones.
 *
 * <p>Tactical override: if the top candidate scores at or above
 * {@link Evaluator#GOAL} (= a goal or king capture is forced), it is played
 * unconditionally. Otherwise, to avoid playing the exact same line every time
 * Hard meets Hard, a random pick is made among all candidates within 5 % of
 * the best score (always at least a 50-point absolute window so tiny scores
 * don't collapse the pool).
 */
public class Hard extends AlphaBetaAI {

    /** Pool window: candidates within [floor, cap] of top score, scaled by percent.
     *  Cap is set well below a knight's value (4 000) so free material is never
     *  randomized away, and well below a queen's value (10 000) so an accidentally
     *  ranked-high "hang queen" move can never enter the pool. */
    private static final int RANDOM_POOL_ABS_FLOOR = 100;
    private static final int RANDOM_POOL_ABS_CAP   = 1_500;
    private static final int RANDOM_POOL_PERCENT   = 1;

    /** Path of the bundled trained transposition table. Built by self-play
     *  ({@code SelfPlayMain}) and committed to {@code core/assets/ai/tt.txt}. */
    private static final String TT_ASSET_PATH = "ai/tt.txt";

    /** Shared TT — loaded lazily on first {@link Hard} construction so disk IO
     *  happens only once per process. Self-play overrides via
     *  {@link AlphaBetaAI#setTranspositionTable} on its own instances. */
    private static TranspositionTable sharedTT;

    private static synchronized TranspositionTable sharedTT() {
        if (sharedTT == null) {
            sharedTT = new TranspositionTable();
            try {
                FileHandle fh = Gdx.files.internal(TT_ASSET_PATH);
                if (fh.exists()) {
                    sharedTT.loadFrom(fh);
                    Gdx.app.log("Hard-TT", "loaded " + sharedTT.size()
                            + " entries from " + fh.path());
                } else {
                    Gdx.app.log("Hard-TT", "no " + TT_ASSET_PATH + " — Hard plays without learned cache");
                }
            } catch (Throwable t) {
                Gdx.app.log("Hard-TT", "load failed (" + t.getClass().getSimpleName()
                        + "): " + t.getMessage() + " — continuing without cache");
            }
        }
        return sharedTT;
    }

    public Hard() {
        super("Hard", 3, 4000L);
        setTranspositionTable(sharedTT());
    }

    // HTML/GWT runs each scoringDfs node ~5-10× slower than the JVM, so giving
    // every candidate 1 s + unlimited count blows the 10 s wall budget. On HTML we
    // check the top-50 candidates × 100 ms each — combined with the tighter HTML
    // negamax beams this keeps Hard's total move time below ~5 s while still doing
    // real defense work on the moves most likely to be picked.
    @Override protected long defenseCheckMs()    { return Constants.IS_HTML ? 100L  : 1000L; }
    @Override protected int  defenseMaxChecked() { return Constants.IS_HTML ? 50    : Integer.MAX_VALUE; }

    @Override
    protected List<ChessBallStep> pickFromRanking(ChessBallFigure[][] board, List<RankedTurn> ranking) {
        if (ranking.isEmpty()) return Collections.emptyList();
        RankedTurn top = ranking.get(0);

        // Within the GOAL-or-better tier, prefer turns that score IN THIS TURN
        // (eval-after = GOAL) over deeper forced-win sequences. Without this,
        // negamax ties at ±GOAL leave it to static rank order, which can put a
        // "win in 3" ahead of a "win in 1" if the immediate goal happened to
        // create a hang and lost a few points to the defense penalty.
        if (top.score >= Evaluator.GOAL) {
            for (RankedTurn rt : ranking) {
                if (rt.score < Evaluator.GOAL) break;
                ChessBallFigure[][] after = TurnGenerator.applyTurn(board, rt.turn);
                if (Evaluator.evaluate(after) >= Evaluator.GOAL) return rt.turn;
            }
            return top.turn;
        }

        int window = Math.min(RANDOM_POOL_ABS_CAP,
                Math.max(RANDOM_POOL_ABS_FLOOR, Math.abs(top.score) * RANDOM_POOL_PERCENT / 100));
        int threshold = top.score - window;
        int poolSize = 1;
        for (int i = 1; i < ranking.size(); i++) {
            if (ranking.get(i).score >= threshold) poolSize++;
            else break;
        }
        return ranking.get(RNG.nextInt(poolSize)).turn;
    }
}
