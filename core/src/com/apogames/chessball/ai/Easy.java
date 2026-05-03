package com.apogames.chessball.ai;

import com.apogames.chessball.ai.algo.AlphaBetaAI;

import java.util.List;

/**
 * Easy AI — depth-1 search. Sees obvious threats via the eval (which already
 * scores a 1-pass scoring threat at ±100 000 and hanging pieces via material),
 * but does NOT see opponent's reply. Picks competently with occasional weaker
 * choices in calm positions; in any tactical position ({@link #isCritical(List)}
 * true) it always picks the best move.
 */
public class Easy extends AlphaBetaAI {

    public Easy() {
        super("Easy", 1, 250L);
    }

    @Override
    protected List<ChessBallStep> pickFromRanking(List<RankedTurn> ranking) {
        if (isCritical(ranking) || ranking.size() == 1) {
            return ranking.get(0).turn;
        }
        int roll = RNG.nextInt(100);
        int idx;
        if (roll < 60)        idx = 0;
        else if (roll < 85)   idx = 1;
        else                  idx = Math.min(2, ranking.size() - 1);
        return ranking.get(idx).turn;
    }
}
