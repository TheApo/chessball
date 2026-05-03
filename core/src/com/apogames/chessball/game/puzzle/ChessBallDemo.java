package com.apogames.chessball.game.puzzle;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed representation of a demo "solution" string from the {@code chessball.solution}
 * column. The string is split by {@code #} into turns, each turn contains
 * {@code ;}-separated 5-int move segments (see {@link ChessBallAIMove}).
 */
public class ChessBallDemo {

    private final String raw;
    private final List<List<ChessBallAIMove>> turns;

    public ChessBallDemo(String raw) {
        this.raw = raw;
        this.turns = parse(raw);
    }

    public String getRaw() {
        return raw;
    }

    public int getTurnCount() {
        return turns.size();
    }

    /**
     * Indices of turns whose final move is a goal-scoring shot
     * (ball lands on row {@code 0} or {@code 14}, by a non-king piece).
     */
    public List<Integer> getGoalTurnIndices() {
        List<Integer> indices = new ArrayList<Integer>();
        for (int i = 0; i < turns.size(); i++) {
            List<ChessBallAIMove> turn = turns.get(i);
            if (turn.isEmpty()) continue;
            ChessBallAIMove last = turn.get(turn.size() - 1);
            int destY = last.getDestinationY();
            if (destY != 0 && destY != 14) continue;
            int fig = last.getFigure();
            if (fig == 1 /* white king */ || fig == 7 /* black king */) continue;
            indices.add(i);
        }
        return indices;
    }

    /** @return moves of turn {@code index}, or {@code null} if out of range. */
    public List<ChessBallAIMove> getTurn(int index) {
        if (index < 0 || index >= turns.size()) {
            return null;
        }
        return turns.get(index);
    }

    private static List<List<ChessBallAIMove>> parse(String raw) {
        List<List<ChessBallAIMove>> result = new ArrayList<List<ChessBallAIMove>>();
        if (raw == null || raw.isEmpty()) {
            return result;
        }
        String[] turnBlocks = raw.split("#", -1);
        for (String block : turnBlocks) {
            List<ChessBallAIMove> turn = new ArrayList<ChessBallAIMove>();
            if (block.isEmpty()) {
                result.add(turn);
                continue;
            }
            String[] segments = block.split(";", -1);
            for (String segment : segments) {
                if (segment.isEmpty()) continue;
                String[] v = segment.split(",");
                if (v.length < 5) continue;
                try {
                    turn.add(new ChessBallAIMove(
                            Integer.parseInt(v[4]),
                            Integer.parseInt(v[0]),
                            Integer.parseInt(v[1]),
                            Integer.parseInt(v[2]),
                            Integer.parseInt(v[3])));
                } catch (NumberFormatException ignored) {
                }
            }
            result.add(turn);
        }
        return result;
    }
}
