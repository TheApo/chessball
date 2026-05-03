package com.apogames.chessball.ai;

import com.apogames.chessball.ai.algo.AlphaBetaAI;
import com.apogames.chessball.game.enums.ChessBallFigure;

import java.util.Collections;
import java.util.List;

/**
 * Hard AI — depth-3 negamax + α-β with iterative deepening, plus the base
 * {@link AlphaBetaAI#applyDefensePenalty} step which already pushes unsafe
 * candidates below safe ones. Picks the top of the defense-aware ranking.
 *
 * <p>Defense check is the safety net for tactical blunders: per candidate up
 * to 1000 ms to enumerate ALL 4-action opponent responses (pass+pass+move+pass
 * etc.) and check whether any reaches the goal. With memoization in
 * {@code scoringDfs} most checks return in &lt;50 ms; the 1 s cap covers
 * dense positions where a 4-action killer requires deep search.
 */
public class Hard extends AlphaBetaAI {

    public Hard() { super("Hard", 3, 800L); }

    // Per-candidate is a TIMEOUT (max). 1000 ms gives the DFS room to find
    // 4-action killer sequences (e.g. pass-pass-queenmove-pass). The 10 s wall
    // cap in applyDefensePenalty bounds the total — at 1 s per check it covers
    // ~10 candidates, and the candidates are sorted by score so the rank-1
    // (most-likely-picked) move is always among them.
    @Override protected long defenseCheckMs()    { return 1000L; }
    @Override protected int  defenseMaxChecked() { return Integer.MAX_VALUE; }

    @Override
    protected List<ChessBallStep> pickFromRanking(ChessBallFigure[][] board, List<RankedTurn> ranking) {
        if (ranking.isEmpty()) return Collections.emptyList();
        return ranking.get(0).turn;
    }
}
