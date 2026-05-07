package com.apogames.chessball.backend.io;

import com.badlogic.gdx.Preferences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Persisted pool of all demos fetched from the server. The pool is filled
 * incrementally via {@link IOOnlineLibgdx#loadDemosSince}: on first launch the
 * server returns the latest 1500 demos, on subsequent launches only those with
 * id larger than {@link #getMaxId()}.
 *
 * Each side ({@link Side#MENU} for the menu showcase, {@link Side#PUZZLE} for
 * the random-puzzle picker) tracks separately which demo ids it has already
 * dispensed. {@link #pick} picks a random unseen demo and marks it seen; once
 * everything is seen for that side the seen-set is cleared and selection
 * restarts. The other side and the demo pool itself are unaffected.
 */
public class DemoCache {

    public enum Side { MENU, PUZZLE }

    public static final class Entry {
        public final int id;
        public final String solution;
        public Entry(int id, String solution) {
            this.id = id;
            this.solution = solution;
        }
    }

    private static final String KEY_SIZE = "demo_size";
    private static final String KEY_ENTRY_ID = "demo_id_";
    private static final String KEY_ENTRY_SOLUTION = "demo_sol_";
    private static final String KEY_MAX_ID = "demo_max_id";
    private static final String KEY_SEEN_MENU = "demo_seen_menu";
    private static final String KEY_SEEN_PUZZLE = "demo_seen_puzzle";

    private final Preferences prefs;
    private final TreeMap<Integer, Entry> byId = new TreeMap<Integer, Entry>();
    private final Set<Integer> seenMenu = new HashSet<Integer>();
    private final Set<Integer> seenPuzzle = new HashSet<Integer>();
    private int maxId = 0;
    private final Random rng = new Random();

    public DemoCache(Preferences prefs) {
        this.prefs = prefs;
    }

    public void load() {
        byId.clear();
        seenMenu.clear();
        seenPuzzle.clear();
        int size = prefs.getInteger(KEY_SIZE, 0);
        for (int i = 0; i < size; i++) {
            int id = prefs.getInteger(KEY_ENTRY_ID + i, -1);
            String sol = prefs.getString(KEY_ENTRY_SOLUTION + i, null);
            if (id > 0 && sol != null && !sol.isEmpty()) {
                byId.put(id, new Entry(id, sol));
            }
        }
        maxId = prefs.getInteger(KEY_MAX_ID, 0);
        parseIds(prefs.getString(KEY_SEEN_MENU, ""), seenMenu);
        parseIds(prefs.getString(KEY_SEEN_PUZZLE, ""), seenPuzzle);
        seenMenu.retainAll(byId.keySet());
        seenPuzzle.retainAll(byId.keySet());
    }

    public int getMaxId() {
        return maxId;
    }

    public boolean isEmpty() {
        return byId.isEmpty();
    }

    public int size() {
        return byId.size();
    }

    /** Add fresh demos (existing ids are kept as-is) and bump {@link #maxId}. */
    public void merge(Collection<Entry> incoming, int serverMaxId) {
        for (Entry e : incoming) {
            if (!byId.containsKey(e.id)) {
                byId.put(e.id, e);
            }
        }
        if (serverMaxId > maxId) {
            maxId = serverMaxId;
        }
    }

    /**
     * Pick a random unseen demo for {@code side} and mark it seen. Returns
     * {@code null} only when the pool itself is empty. When all entries are
     * already seen, the seen-set for that side is cleared first so the next
     * cycle starts.
     */
    public Entry pick(Side side) {
        if (byId.isEmpty()) {
            return null;
        }
        Set<Integer> seen = (side == Side.MENU) ? seenMenu : seenPuzzle;
        if (seen.size() >= byId.size()) {
            seen.clear();
        }
        List<Integer> unseen = new ArrayList<Integer>(byId.size() - seen.size());
        for (Integer id : byId.keySet()) {
            if (!seen.contains(id)) {
                unseen.add(id);
            }
        }
        if (unseen.isEmpty()) {
            return null;
        }
        int picked = unseen.get(rng.nextInt(unseen.size()));
        seen.add(picked);
        return byId.get(picked);
    }

    /** Persist only the seen-sets — call after each {@link #pick}. */
    public void persistSeen() {
        prefs.putString(KEY_SEEN_MENU, encodeIds(seenMenu));
        prefs.putString(KEY_SEEN_PUZZLE, encodeIds(seenPuzzle));
        prefs.flush();
    }

    /** Persist the full pool + seen-sets — call after merging fresh entries. */
    public void persistAll() {
        int prevSize = prefs.getInteger(KEY_SIZE, 0);
        int newSize = byId.size();
        int i = 0;
        for (Entry e : byId.values()) {
            prefs.putInteger(KEY_ENTRY_ID + i, e.id);
            prefs.putString(KEY_ENTRY_SOLUTION + i, e.solution);
            i++;
        }
        for (int j = newSize; j < prevSize; j++) {
            prefs.remove(KEY_ENTRY_ID + j);
            prefs.remove(KEY_ENTRY_SOLUTION + j);
        }
        prefs.putInteger(KEY_SIZE, newSize);
        prefs.putInteger(KEY_MAX_ID, maxId);
        prefs.putString(KEY_SEEN_MENU, encodeIds(seenMenu));
        prefs.putString(KEY_SEEN_PUZZLE, encodeIds(seenPuzzle));
        prefs.flush();
    }

    private static void parseIds(String s, Set<Integer> out) {
        if (s == null || s.isEmpty()) {
            return;
        }
        String[] parts = s.split(",");
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                out.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private static String encodeIds(Set<Integer> ids) {
        if (ids.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer id : ids) {
            if (!first) {
                sb.append(',');
            }
            sb.append(id);
            first = false;
        }
        return sb.toString();
    }
}
