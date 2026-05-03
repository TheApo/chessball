package com.apogames.chessball.ai;

import com.apogames.chessball.ai.algo.AlphaBetaAI;

import java.util.List;

/**
 * Hard AI — depth-3 negamax via iterative deepening with a 700 ms budget.
 * Falls back gracefully to depth-2 results if depth-3 doesn't complete in time.
 * Always plays the best-ranked move. Targets ~Elo 1500.
 */
public class Hard extends AlphaBetaAI {

    public Hard() {
        super("Hard", 3, 700L);
    }

    @Override
    protected List<ChessBallStep> pickFromRanking(List<RankedTurn> ranking) {
        return ranking.get(0).turn;
    }
}
