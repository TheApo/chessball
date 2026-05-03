package com.apogames.chessball;

import com.apogames.chessball.game.enums.ChessBallFigure;

public class Utils {

    public static int[][] getCopy(final int[][] original) {
        int[][] copy = new int[original.length][original[0].length];
        for (int y = 0; y < original.length; y++) {
            for (int x = 0; x < original[0].length; x++) {
                copy[y][x] = original[y][x];
            }
        }
        return copy;
    }

    public static float[][] getCopy(final float[][] original) {
        float[][] copy = new float[original.length][original[0].length];
        for (int y = 0; y < original.length; y++) {
            for (int x = 0; x < original[0].length; x++) {
                copy[y][x] = original[y][x];
            }
        }
        return copy;
    }

    public static boolean[][] getCopy(final boolean[][] original) {
        boolean[][] copy = new boolean[original.length][original[0].length];
        for (int y = 0; y < original.length; y++) {
            for (int x = 0; x < original[0].length; x++) {
                copy[y][x] = original[y][x];
            }
        }
        return copy;
    }

    public static ChessBallFigure[][] getCopy(final ChessBallFigure[][] original) {
        ChessBallFigure[][] copy = new ChessBallFigure[original.length][original[0].length];
        for (int y = 0; y < original.length; y++) {
            for (int x = 0; x < original[0].length; x++) {
                copy[y][x] = original[y][x];
            }
        }
        return copy;
    }

    public static float[] getCopy(final float[] original) {
        float[] copy = new float[original.length];
        for (int y = 0; y < original.length; y++) {
            copy[y] = original[y];
        }
        return copy;
    }
}
