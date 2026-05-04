package com.apogames.chessball.solver;

import com.apogames.chessball.game.puzzle.ChessBallLevels;

/**
 * Standalone entry point: runs {@link PuzzleSolver} against every stored
 * puzzle level and prints which ones are not solvable in 1 piece move + 3 ball passes.
 *
 * Run via: {@code ./gradlew :desktop:solverCheck}
 */
public final class PuzzleSolverMain {

    public static void main(String[] args) {
        int unsolvable = 0;
        long totalNs = 0;
        System.out.println("Checking " + ChessBallLevels.LEVELS.length + " levels (1 move + 3 passes)...\n");
        for (int i = 0; i < ChessBallLevels.LEVELS.length; i++) {
            String level = ChessBallLevels.LEVELS[i];
            char turnDigit = level.charAt(PuzzleSolver.W * PuzzleSolver.H);
            String who = (turnDigit == '1') ? "BLACK" : "WHITE";
            long t0 = System.nanoTime();
            boolean ok = PuzzleSolver.isSolvable(level);
            long ns = System.nanoTime() - t0;
            totalNs += ns;
            String label = ok ? "  OK      " : "UNSOLVABLE";
            String hint = "";
            if (!ok) {
                // Check whether flipping the turn digit makes it solvable.
                String flipped = flipTurn(level);
                boolean flipOk = PuzzleSolver.isSolvable(flipped);
                hint = flipOk
                        ? "  -> SOLVABLE if turn flipped to " + (turnDigit == '1' ? "WHITE" : "BLACK")
                        : "  -> still unsolvable when turn flipped";
            }
            System.out.printf("Level %2d  [%s]  turn=%-5s  %5d ms%s%n",
                    i + 1, label, who, ns / 1_000_000, hint);
            if (!ok) unsolvable++;
        }
        System.out.println();
        System.out.printf("Total unsolvable: %d / %d  (%d ms total)%n",
                unsolvable, ChessBallLevels.LEVELS.length, totalNs / 1_000_000);
    }

    private static String flipTurn(String level) {
        char[] arr = level.toCharArray();
        int idx = PuzzleSolver.W * PuzzleSolver.H;
        arr[idx] = (arr[idx] == '1') ? '0' : '1';
        return new String(arr);
    }
}
