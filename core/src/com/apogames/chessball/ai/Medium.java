package com.apogames.chessball.ai;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.algo.AlphaBetaAI;
import com.apogames.chessball.ai.algo.Evaluator;
import com.apogames.chessball.ai.algo.TurnGenerator;
import com.apogames.chessball.game.enums.ChessBallFigure;

import java.util.Collections;
import java.util.List;

/**
 * Medium AI — same depth-2 search as before. Tactical override (goal/king-capture
 * always taken), then random pick from a 20 % score window. Distinguished from
 * Hard by shallower depth (2 vs 3) and a wider pool (20 % vs 5 %), so Medium
 * still plays strong moves but is consistently outranked by Hard over time.
 */
public class Medium extends AlphaBetaAI {

    private static final int RANDOM_POOL_ABS_FLOOR = 500;
    private static final int RANDOM_POOL_ABS_CAP   = 8_000;
    private static final int RANDOM_POOL_PERCENT   = 10;

    public Medium() {
        super("Medium", 2, 1500L);
    }

    // HTML/GWT: 500 candidates × 400 ms = 200 s rechnerisch — der 10 s Wall-Cap
    // wird sofort gerissen und nur ~25 Kandidaten kriegen überhaupt einen Defense-
    // Check. Auf HTML kürzen wir auf top-30 × 80 ms (~2.5 s Wall) damit Medium das
    // Move-Time-Budget einhält.
    @Override protected long defenseCheckMs()    { return Constants.IS_HTML ? 80L : 400L; }
    @Override protected int  defenseMaxChecked() { return Constants.IS_HTML ? 30  : 500; }

    @Override
    protected List<ChessBallStep> pickFromRanking(ChessBallFigure[][] board, List<RankedTurn> ranking) {
        if (ranking.isEmpty()) return Collections.emptyList();
        RankedTurn top = ranking.get(0);

        // Same immediate-goal preference as Hard — see Hard.pickFromRanking.
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
