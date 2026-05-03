package com.apogames.chessball.ai;

import com.apogames.chessball.ai.algo.AlphaBetaAI;
import com.apogames.chessball.game.enums.ChessBallFigure;

import java.util.Collections;
import java.util.List;

/**
 * Medium AI — depth-2 negamax. Always sees 1-move-ahead opponent threats and
 * defends them. 5 % chance of playing rank-2 in non-critical positions.
 */
public class Medium extends AlphaBetaAI {

    public Medium() {
        super("Medium", 2, 1000L);
    }

    @Override protected long defenseCheckMs()    { return 400L; }
    @Override protected int  defenseMaxChecked() { return 500; }

    @Override
    protected List<ChessBallStep> pickFromRanking(ChessBallFigure[][] board, List<RankedTurn> ranking) {
        if (ranking.isEmpty()) return Collections.emptyList();
        if (isCritical(ranking) || ranking.size() < 2) return ranking.get(0).turn;
        if (RNG.nextInt(100) < 50)                     return ranking.get(1).turn;
        return ranking.get(0).turn;
    }
}
