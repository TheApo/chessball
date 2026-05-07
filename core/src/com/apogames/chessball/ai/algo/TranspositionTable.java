package com.apogames.chessball.ai.algo;

import com.apogames.chessball.ai.ChessBallStep;
import com.badlogic.gdx.files.FileHandle;

import java.io.BufferedReader;
import java.io.IOException;
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
 * <p>Compact binary serialisation (magic {@code TTBN1\n}), per entry:
 * <pre>
 *   6 B  hash low 48 bits, big-endian
 *   1 B  step count
 *   N×2B steps, each = (fx&lt;&lt;12)|(fy&lt;&lt;8)|(tx&lt;&lt;4)|ty
 * </pre>
 * Typical 3-step entry = 13 bytes (~6× smaller than the legacy plain-text
 * format, which is still readable on load for backwards compatibility). Only
 * the bestMove survives the round-trip — depth/score/flag are not persisted
 * because the runtime root cache only consults bestMove. See {@link Entry#fromDisk}
 * for the in-search safety story.
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
        /** True for entries reconstructed from disk — depth/score/flag are placeholders.
         *  Safe at the root cache shortcut (which only consults bestMove), but in-search
         *  negamax MUST skip these or the bogus score=0 EXACT poisons cutoffs. */
        public final boolean fromDisk;

        public Entry(long hash, int depth, int score, Flag flag,
                     List<ChessBallStep> bestMove, int visits) {
            this(hash, depth, score, flag, bestMove, visits, false);
        }

        public Entry(long hash, int depth, int score, Flag flag,
                     List<ChessBallStep> bestMove, int visits, boolean fromDisk) {
            this.hash = hash;
            this.depth = depth;
            this.score = score;
            this.flag = flag;
            this.bestMove = bestMove;
            this.visits = visits;
            this.fromDisk = fromDisk;
        }
    }

    /** Cap when serialising — keep top-N by visits×depth. */
    public static final int DEFAULT_MAX_ENTRIES = 50_000;

    /** Hash truncation for the binary format — low 48 bits. Trades collision
     *  resistance for a 25 % smaller hash field. Birthday-collision odds are
     *  ~0.4 % at 50 K entries, ~7 % at 200 K. {@link #AlphaBetaAI} guards by
     *  validating the from-square of a hit's bestMove before using it. */
    public static final long HASH_MASK = 0xFFFFFFFFFFFFL;

    /** Marker for entries materialised from disk. Higher than any plausible
     *  search depth so the root-shortcut always considers them deep enough. */
    private static final int LOADED_DEPTH = 99;

    private static final byte[] BINARY_MAGIC = {'T', 'T', 'B', 'N', '1', '\n'};

    private final Map<Long, Entry> map = new HashMap<Long, Entry>();

    public int size() { return map.size(); }

    public Entry get(long hash) { return map.get(hash & HASH_MASK); }

    /** Insert or update. On collision, prefer higher-depth entry (more accurate);
     *  on equal/lower depth, keep existing but bump visits so popular shallow
     *  entries get retained when capping. */
    public void put(long hash, int depth, int score, Flag flag,
                    List<ChessBallStep> bestMove) {
        long key = hash & HASH_MASK;
        Entry existing = map.get(key);
        if (existing == null) {
            map.put(key, new Entry(key, depth, score, flag, bestMove, 1));
            return;
        }
        existing.visits++;
        if (depth > existing.depth) {
            map.put(key, new Entry(key, depth, score, flag, bestMove,
                    existing.visits));
        }
    }

    /** Fold another TT into this one — used to merge per-game caches into the
     *  global self-play table without thread-safe contention during the search. */
    public void merge(TranspositionTable other) {
        for (Entry e : other.map.values()) {
            long key = e.hash & HASH_MASK;
            Entry mine = map.get(key);
            if (mine == null) {
                map.put(key, new Entry(key, e.depth, e.score, e.flag,
                        e.bestMove, e.visits, e.fromDisk));
            } else {
                mine.visits += e.visits;
                if (e.depth > mine.depth) {
                    map.put(key, new Entry(key, e.depth, e.score, e.flag,
                            e.bestMove, mine.visits, e.fromDisk));
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

    /** Serialise top-N entries as compact binary (format {@code TTBN1}). Per entry:
     *  6 B hash (low 48 bits, big-endian), 1 B step count, then N×2 B packed steps
     *  (each = {@code (fx<<12)|(fy<<8)|(tx<<4)|ty}). On load, depth/score/flag are
     *  reconstituted as {@code LOADED_DEPTH}/0/EXACT — see {@link Entry#fromDisk}.
     *  Only entries with non-empty bestMove are written (others can't drive a
     *  root cache hit, so they'd be dead weight on disk). */
    public void saveTo(FileHandle file, int maxEntries) {
        file.writeBytes(encodeBinary(maxEntries), false);
    }

    /** Convenience for writers that don't need a FileHandle (e.g. CLI tools). */
    public void saveTo(java.io.File file, int maxEntries) {
        try {
            try (java.io.OutputStream out = new java.io.BufferedOutputStream(
                    new java.io.FileOutputStream(file))) {
                out.write(encodeBinary(maxEntries));
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to write TT to " + file, ex);
        }
    }

    private byte[] encodeBinary(int maxEntries) {
        List<Entry> top = topEntries(maxEntries);
        // Worst-case sizing: 6 + 1 + (15 steps × 2) per entry. Real avg ~13 B.
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(
                BINARY_MAGIC.length + top.size() * 16);
        buf.write(BINARY_MAGIC, 0, BINARY_MAGIC.length);
        for (Entry e : top) {
            if (e.bestMove == null || e.bestMove.isEmpty()) continue;
            int stepCount = Math.min(255, e.bestMove.size());
            long h = e.hash & HASH_MASK;
            buf.write((int) (h >>> 40) & 0xFF);
            buf.write((int) (h >>> 32) & 0xFF);
            buf.write((int) (h >>> 24) & 0xFF);
            buf.write((int) (h >>> 16) & 0xFF);
            buf.write((int) (h >>> 8)  & 0xFF);
            buf.write((int) h          & 0xFF);
            buf.write(stepCount);
            for (int i = 0; i < stepCount; i++) {
                ChessBallStep s = e.bestMove.get(i);
                int packed = ((s.getFigureX()     & 0xF) << 12)
                           | ((s.getFigureY()     & 0xF) << 8)
                           | ((s.getStepFigureX() & 0xF) << 4)
                           |  (s.getStepFigureY() & 0xF);
                buf.write((packed >>> 8) & 0xFF);
                buf.write(packed         & 0xFF);
            }
        }
        return buf.toByteArray();
    }

    /** Load from a file handle. Auto-detects format: binary {@code TTBN1} when
     *  the magic header matches, otherwise falls back to legacy V1 plain text
     *  (so previously committed {@code tt.txt} files still work). */
    public void loadFrom(FileHandle file) {
        if (file == null || !file.exists()) return;
        byte[] data = file.readBytes();
        if (looksBinary(data)) loadBinary(data);
        else loadFromString(new String(data));
    }

    public void loadFromString(String content) {
        BufferedReader reader = new BufferedReader(new StringReader(content));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') continue;
                Entry e = parseEntry(line);
                if (e == null) continue;
                map.put(e.hash & HASH_MASK, new Entry(e.hash & HASH_MASK,
                        e.depth, e.score, e.flag, e.bestMove, e.visits));
            }
        } catch (IOException ignored) {
            // StringReader doesn't actually throw — defensive only.
        }
    }

    private static boolean looksBinary(byte[] data) {
        if (data == null || data.length < BINARY_MAGIC.length) return false;
        for (int i = 0; i < BINARY_MAGIC.length; i++) {
            if (data[i] != BINARY_MAGIC[i]) return false;
        }
        return true;
    }

    private void loadBinary(byte[] data) {
        int pos = BINARY_MAGIC.length;
        while (pos + 7 <= data.length) {
            long h = 0L;
            for (int i = 0; i < 6; i++) {
                h = (h << 8) | (data[pos + i] & 0xFFL);
            }
            pos += 6;
            int stepCount = data[pos++] & 0xFF;
            if (pos + stepCount * 2 > data.length) break;
            ArrayList<ChessBallStep> steps = new ArrayList<ChessBallStep>(stepCount);
            for (int i = 0; i < stepCount; i++) {
                int packed = ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
                pos += 2;
                steps.add(new ChessBallStep(
                        (packed >>> 12) & 0xF,
                        (packed >>> 8)  & 0xF,
                        (packed >>> 4)  & 0xF,
                         packed         & 0xF));
            }
            map.put(h, new Entry(h, LOADED_DEPTH, 0, Flag.EXACT, steps, 1, true));
        }
    }

    private static Entry parseEntry(String line) {
        try {
            String[] parts = line.split(";", -1);
            if (parts.length < 6) return null;
            long hash = parseHexUnsigned(parts[0]);
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

    /** Hand-rolled hex-to-long because {@code Long.parseUnsignedLong(String, int)}
     *  isn't emulated by TeaVM/gdx-teavm — the HTML build links the method to a
     *  throwing stub. Pure char arithmetic, no JDK helper required. */
    private static long parseHexUnsigned(String s) {
        int n = s.length();
        if (n == 0 || n > 16) throw new NumberFormatException(s);
        long result = 0L;
        for (int i = 0; i < n; i++) {
            char c = s.charAt(i);
            int digit;
            if      (c >= '0' && c <= '9') digit = c - '0';
            else if (c >= 'a' && c <= 'f') digit = c - 'a' + 10;
            else if (c >= 'A' && c <= 'F') digit = c - 'A' + 10;
            else throw new NumberFormatException(s);
            result = (result << 4) | digit;
        }
        return result;
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
}
