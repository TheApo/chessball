package com.apogames.chessball.desktop;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.ChessBallAIInformations;
import com.apogames.chessball.ai.ChessBallPlayerAI;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.ai.Easy;
import com.apogames.chessball.ai.Hard;
import com.apogames.chessball.ai.Medium;
import com.apogames.chessball.ai.algo.AlphaBetaAI;
import com.apogames.chessball.ai.algo.TranspositionTable;
import com.apogames.chessball.ai.algo.TurnGenerator;
import com.apogames.chessball.game.ChessBallBoard;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.apogames.chessball.game.enums.ChessBallWinState;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless self-play harness — runs many AI-vs-AI games to populate a
 * transposition table that ships with the game so {@link Hard} plays known
 * positions instantly. Each game uses a fresh in-memory TT (no contention);
 * after the game its entries are merged into the global table under a single
 * lock. The global table is written to disk at the end as plain text.
 *
 * <p>Run via {@code ./gradlew desktop:runSelfPlay --args="..."}.
 */
public final class SelfPlayMain {

    /** Hard cap per game — pathological loops can't lock a worker forever. */
    private static final int MAX_TURNS_PER_GAME = 400;
    /** Status line cadence in completed games. */
    private static final int PROGRESS_EVERY = 10;

    public static void main(String[] cliArgs) throws Exception {
        Args args = parseArgs(cliArgs);
        System.out.println("Self-play config: " + args);
        initHeadless();

        TranspositionTable globalTT = new TranspositionTable();
        if (args.loadFile != null) {
            globalTT.loadFrom(Gdx.files.absolute(args.loadFile.getAbsolutePath()));
            System.out.println("Pre-loaded " + globalTT.size() + " entries from " + args.loadFile);
        }

        long start = System.currentTimeMillis();
        AtomicInteger done = new AtomicInteger();
        AtomicInteger whiteWins = new AtomicInteger();
        AtomicInteger blackWins = new AtomicInteger();
        AtomicInteger draws = new AtomicInteger();

        ExecutorService exec = Executors.newFixedThreadPool(args.threads);
        for (int i = 0; i < args.games; i++) {
            final int gameId = i;
            exec.submit(() -> runOneGame(gameId, args, globalTT, done, whiteWins, blackWins, draws, start));
        }
        exec.shutdown();
        exec.awaitTermination(7, TimeUnit.DAYS);

        long elapsed = System.currentTimeMillis() - start;
        System.out.printf("Done: %d games in %.1fs (%.1f games/min)%n",
                args.games, elapsed / 1000.0,
                args.games / Math.max(0.001, elapsed / 60_000.0));
        System.out.printf("Results: white wins %d, black wins %d, draws %d%n",
                whiteWins.get(), blackWins.get(), draws.get());

        File outFile = args.outFile.getAbsoluteFile();
        File parent = outFile.getParentFile();
        if (parent != null) parent.mkdirs();
        globalTT.saveTo(outFile, args.maxEntries);
        System.out.println("Wrote " + Math.min(globalTT.size(), args.maxEntries)
                + " of " + globalTT.size() + " entries to " + outFile);

        // HeadlessApplication keeps a daemon thread alive; exit explicitly so the
        // gradle task finishes promptly.
        System.exit(0);
    }

    private static void runOneGame(int gameId, Args args, TranspositionTable globalTT,
                                   AtomicInteger done, AtomicInteger whiteWins,
                                   AtomicInteger blackWins, AtomicInteger draws,
                                   long start) {
        try {
            TranspositionTable localTT = new TranspositionTable();
            ChessBallPlayerAI white = makeAI(args.white);
            ChessBallPlayerAI black = makeAI(args.black);
            attachTT(white, localTT);
            attachTT(black, localTT);

            ChessBallWinState result = playOneGame(white, black);
            synchronized (globalTT) { globalTT.merge(localTT); }
            if (result == ChessBallWinState.WHITE_WIN) whiteWins.incrementAndGet();
            else if (result == ChessBallWinState.BLACK_WIN) blackWins.incrementAndGet();
            else draws.incrementAndGet();

            int n = done.incrementAndGet();
            if (n % PROGRESS_EVERY == 0) {
                long elapsed = System.currentTimeMillis() - start;
                int ttSize;
                synchronized (globalTT) { ttSize = globalTT.size(); }
                System.out.printf("  %d/%d games  TT=%d  W:%d B:%d D:%d  %.1fs%n",
                        n, args.games, ttSize,
                        whiteWins.get(), blackWins.get(), draws.get(),
                        elapsed / 1000.0);
            }
        } catch (Throwable t) {
            System.err.println("game " + gameId + " failed: " + t);
            t.printStackTrace();
        }
    }

    private static void attachTT(ChessBallPlayerAI ai, TranspositionTable tt) {
        if (ai instanceof AlphaBetaAI) ((AlphaBetaAI) ai).setTranspositionTable(tt);
    }

    /**
     * Drive one game from initial position to a winner (or {@link ChessBallWinState#GAME}
     * if the turn cap is hit, treated as a draw). Mirrors the structure of
     * {@code ChessBallGame.mouseButtonReleased}+{@code winCheck} but without rendering.
     */
    private static ChessBallWinState playOneGame(ChessBallPlayerAI whiteAi,
                                                 ChessBallPlayerAI blackAi) {
        ChessBallBoard board = new ChessBallBoard();
        for (int turnIdx = 0; turnIdx < MAX_TURNS_PER_GAME; turnIdx++) {
            ChessBallWinState over = board.isGameOver();
            if (over != ChessBallWinState.GAME) return over;

            boolean isBlack = board.getCurrentColor() == ChessBallColor.BLACK;
            ChessBallPlayerAI ai = isBlack ? blackAi : whiteAi;

            List<ChessBallStep> turn = ai.update(new ChessBallAIInformations(board, isBlack));
            if (turn == null || turn.isEmpty()) {
                board.nextPlayer();
                continue;
            }

            // AI returns moves in white-internal POV; un-mirror when it played black.
            if (isBlack) turn = unmirror(turn);

            ChessBallFigure[][] after = TurnGenerator.applyTurn(board.getBoard(), turn);
            board.setBoard(after);

            ChessBallWinState ws = board.winCheck();
            switch (ws) {
                case BLACK_GOAL:
                case WHITE_NO_KING:
                    board.addGoalBlack();
                    break;
                case WHITE_GOAL:
                case BLACK_NO_KING:
                    board.addGoalWhite();
                    break;
                default:
                    board.nextPlayer();
            }
        }
        return ChessBallWinState.GAME;
    }

    private static List<ChessBallStep> unmirror(List<ChessBallStep> turn) {
        int w = Constants.BOARD_COLS;
        int h = Constants.BOARD_ROWS;
        ArrayList<ChessBallStep> out = new ArrayList<>(turn.size());
        for (ChessBallStep s : turn) {
            out.add(new ChessBallStep(
                    w - 1 - s.getFigureX(),     h - 1 - s.getFigureY(),
                    w - 1 - s.getStepFigureX(), h - 1 - s.getStepFigureY()));
        }
        return out;
    }

    private static ChessBallPlayerAI makeAI(String name) {
        switch (name.toLowerCase()) {
            case "hard":   return new Hard();
            case "medium": return new Medium();
            case "easy":   return new Easy();
            default:
                throw new IllegalArgumentException("unknown AI '" + name
                        + "' — use Easy / Medium / Hard");
        }
    }

    private static void initHeadless() {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new ApplicationAdapter() {}, cfg);
        // Mute the per-search debug logs from AlphaBetaAI so the progress lines
        // are readable. Flip to LOG_INFO if you want to see what Hard is thinking.
        Gdx.app.setLogLevel(Application.LOG_NONE);
    }

    private static final class Args {
        int games = 100;
        String white = "Hard";
        String black = "Hard";
        File outFile = new File("core/assets/ai/tt.txt");
        File loadFile = null;
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        int maxEntries = TranspositionTable.DEFAULT_MAX_ENTRIES;

        @Override public String toString() {
            return "games=" + games + " white=" + white + " black=" + black
                    + " threads=" + threads + " maxEntries=" + maxEntries
                    + " load=" + loadFile + " out=" + outFile.getAbsolutePath();
        }
    }

    private static Args parseArgs(String[] cli) {
        Args a = new Args();
        for (int i = 0; i < cli.length; i++) {
            String key = cli[i];
            String val = (i + 1 < cli.length) ? cli[i + 1] : null;
            switch (key) {
                case "--games":       a.games = Integer.parseInt(val); i++; break;
                case "--white":       a.white = val; i++; break;
                case "--black":       a.black = val; i++; break;
                case "--out":         a.outFile = new File(val); i++; break;
                case "--load":        a.loadFile = new File(val); i++; break;
                case "--threads":     a.threads = Math.max(1, Integer.parseInt(val)); i++; break;
                case "--max-entries": a.maxEntries = Integer.parseInt(val); i++; break;
                case "--help":
                case "-h":
                    printUsage();
                    System.exit(0);
                    break;
                default:
                    System.err.println("unknown arg: " + key);
                    printUsage();
                    System.exit(1);
            }
        }
        return a;
    }

    private static void printUsage() {
        System.out.println("Usage: SelfPlayMain [options]");
        System.out.println("  --games N            number of games to play (default 100)");
        System.out.println("  --white  AI          Easy | Medium | Hard (default Hard)");
        System.out.println("  --black  AI          Easy | Medium | Hard (default Hard)");
        System.out.println("  --out    PATH        output TT file (default core/assets/ai/tt.txt)");
        System.out.println("  --load   PATH        pre-load existing TT and add to it");
        System.out.println("  --threads N          parallel workers (default cores-1)");
        System.out.println("  --max-entries N      cap before serialising (default 50000)");
    }
}
