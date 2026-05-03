package com.apogames.chessball.ai;

import com.apogames.chessball.ai.algo.AlphaBetaAI;

import java.util.List;

/**
 * Medium AI — depth-2 negamax. Always sees 1-move-ahead opponent threats and
 * defends them. 5 % chance of playing rank-2 in non-critical positions.
 */
public class Medium extends AlphaBetaAI {

    public Medium() {
        super("Medium", 2, 400L);
    }

    @Override
    protected List<ChessBallStep> pickFromRanking(List<RankedTurn> ranking) {
        if (isCritical(ranking) || ranking.size() < 2) return ranking.get(0).turn;
        if (RNG.nextInt(100) < 5)                     return ranking.get(1).turn;
        return ranking.get(0).turn;
    }
}
