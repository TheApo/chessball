package com.apogames.chessball.ai.algo;

import com.apogames.chessball.ai.ChessBallStep;
import com.badlogic.gdx.files.FileHandle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transposition table — caches negamax search results so repeated positions
 * skip re-search. In-memory during a search; can be persisted to disk so the
 * shipped Hard AI starts already knowing common positions (built up by
 * self-play, see {@code SelfPlayMain} and {@code assets/ai/tt.txt}).
 *
 * <p>Plain-text serialisation, one entry per line:
 * <pre>{@code
 * <hashHex>;<depth>;<score>;<flag>;<fromX,fromY,toX,toY|...>;<visits>
 * }</pre>
 */
public final class TranspositionTable {

    public enum Flag { EXACT, LOWER_BOUND, UPPER_BOUND }

    public static final class Entry {
        public final long hash;
        public final int depth;
        public final int score;
        public final Flag flag;
        public final List<ChessBallStep> bestMove;
        public int visits;

        public Entry(long hash, int depth, int score, Flag flag,
                     List<ChessBallStep> bestMove, int visits) {
            this.hash = hash;
            this.depth = depth;
            this.score = score;
            this.flag = flag;
            this.bestMove = bestMove;
            this.visits = visits;
        }
    }

    /** Cap when serialising — keep top-N by visits×depth. ~3 MB plain text. */
    public static final int DEFAULT_MAX_ENTRIES = 50_000;

    private final Map<Long, Entry> map = new HashMap<Long, Entry>();

    public int size() { return map.size(); }

    public Entry get(long hash) { return map.get(hash); }

    /** Insert or update. On collision, prefer higher-depth entry (more accurate);
     *  on equal/lower depth, keep existing but bump visits so popular shallow
     *  entries get retained when capping. */
    public void put(long hash, int depth, int score, Flag flag,
                    List<ChessBallStep> bestMove) {
        Entry existing = map.get(hash);
        if (existing == null) {
            map.put(hash, new Entry(hash, depth, score, flag, bestMove, 1));
            return;
        }
        existing.visits++;
        if (depth > existing.depth) {
            map.put(hash, new Entry(hash, depth, score, flag, bestMove,
                    existing.visits));
        }
    }

    /** Fold another TT into this one — used to merge per-game caches into the
     *  global self-play table without thread-safe contention during the search. */
    public void merge(TranspositionTable other) {
        for (Entry e : other.map.values()) {
            Entry mine = map.get(e.hash);
            if (mine == null) {
                map.put(e.hash, new Entry(e.hash, e.depth, e.score, e.flag,
                        e.bestMove, e.visits));
            } else {
                mine.visits += e.visits;
                if (e.depth > mine.depth) {
                    map.put(e.hash, new Entry(e.hash, e.depth, e.score, e.flag,
                            e.bestMove, mine.visits));
                }
            }
        }
    }

    /** Top-N entries by (visits × depth) descending. Used as the eviction order
     *  before serialising — high-depth + frequently-visited beats cheap noise. */
    public List<Entry> topEntries(int n) {
        List<Entry> all = new ArrayList<Entry>(map.values());
        Collections.sort(all, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) {
                long sa = (long) a.visits * Math.max(1, a.depth);
                long sb = (long) b.visits * Math.max(1, b.depth);
                return Long.compare(sb, sa);
            }
        });
        if (all.size() > n) return new ArrayList<Entry>(all.subList(0, n));
        return all;
    }

    /** Append all entries (top {@code maxEntries}) as plain text to the file.
     *  Overwrites if the handle exists. */
    public void saveTo(FileHandle file, int maxEntries) {
        StringBuilder sb = new StringBuilder(map.size() * 80);
        sb.append("# ChessBall TT v1 — fields: hashHex;depth;score;flag;move;visits\n");
        for (Entry e : topEntries(maxEntries)) {
            appendEntry(sb, e);
            sb.append('\n');
        }
        file.writeString(sb.toString(), false);
    }

    /** Load from a file handle. Tolerant: skips blank/comment lines and any
     *  malformed entry without aborting. */
    public void loadFrom(FileHandle file) {
        if (file == null || !file.exists()) return;
        loadFromString(file.readString());
    }

    public void loadFromString(String content) {
        BufferedReader reader = new BufferedReader(new StringReader(content));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                Entry e = parseEntry(line);
                if (e == null) continue;
                map.put(e.hash, e);
            }
        } catch (IOException ignored) {
            // StringReader doesn't actually throw — defensive only.
        }
    }

    private static void appendEntry(StringBuilder sb, Entry e) {
        sb.append(Long.toHexString(e.hash)).append(';');
        sb.append(e.depth).append(';');
        sb.append(e.score).append(';');
        sb.append(e.flag.name()).append(';');
        if (e.bestMove != null) {
            for (int i = 0; i < e.bestMove.size(); i++) {
                ChessBallStep s = e.bestMove.get(i);
                if (i > 0) sb.append('|');
                sb.append(s.getFigureX()).append(',')
                  .append(s.getFigureY()).append(',')
                  .append(s.getStepFigureX()).append(',')
                  .append(s.getStepFigureY());
            }
        }
        sb.append(';').append(e.visits);
    }

    private static Entry parseEntry(String line) {
        try {
            String[] parts = line.split(";", -1);
            if (parts.length < 6) return null;
            long hash = Long.parseUnsignedLong(parts[0], 16);
            int depth = Integer.parseInt(parts[1]);
            int score = Integer.parseInt(parts[2]);
            Flag flag = Flag.valueOf(parts[3]);
            List<ChessBallStep> move = parseMove(parts[4]);
            int visits = Integer.parseInt(parts[5]);
            return new Entry(hash, depth, score, flag, move, visits);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static List<ChessBallStep> parseMove(String s) {
        if (s == null || s.isEmpty()) return Collections.emptyList();
        String[] steps = s.split("\\|");
        List<ChessBallStep> result = new ArrayList<ChessBallStep>(steps.length);
        for (String stepStr : steps) {
            String[] coords = stepStr.split(",");
            if (coords.length != 4) return null;
            int fx = Integer.parseInt(coords[0]);
            int fy = Integer.parseInt(coords[1]);
            int tx = Integer.parseInt(coords[2]);
            int ty = Integer.parseInt(coords[3]);
            result.add(new ChessBallStep(fx, fy, tx, ty));
        }
        return result;
    }

    /** Convenience for writers that don't need a FileHandle (e.g. CLI tools). */
    public void saveTo(java.io.File file, int maxEntries) {
        try {
            try (PrintWriter pw = new PrintWriter(file, "UTF-8")) {
                pw.println("# ChessBall TT v1 — fields: hashHex;depth;score;flag;move;visits");
                StringBuilder sb = new StringBuilder(120);
                for (Entry e : topEntries(maxEntries)) {
                    sb.setLength(0);
                    appendEntry(sb, e);
                    pw.println(sb);
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write TT to " + file, ex);
        }
    }
}
