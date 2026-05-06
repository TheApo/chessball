package com.apogames.chessball.game.menu;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.backend.Game;
import com.apogames.chessball.backend.io.IOOnlineLibgdx;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.game.ChessBallModel;
import com.apogames.chessball.game.MainPanel;
import com.apogames.chessball.game.MoveAnimator;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.enums.ChessBallWinState;
import com.apogames.chessball.game.puzzle.ChessBallAIMove;
import com.apogames.chessball.game.puzzle.ChessBallDemo;
import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

public class ChessBallMenu extends ChessBallModel {

    public static final String FUNCTION_START = "menu_start";
    public static final String FUNCTION_TUTORIAL = "menu_tutorial";
    public static final String FUNCTION_PUZZLE = "menu_puzzle";
    public static final String FUNCTION_LEFT_WHITE = "menu_left_white";
    public static final String FUNCTION_RIGHT_WHITE = "menu_right_white";
    public static final String FUNCTION_LEFT_BLACK = "menu_left_black";
    public static final String FUNCTION_RIGHT_BLACK = "menu_right_black";

    /** Idle threshold (ms) before a background demo starts playing. Reset on every click. */
    private static final float DEMO_IDLE_THRESHOLD = 5000f;
    /** Stop trying after this many consecutive fetch failures. Counter resets only on
     *  re-entering the menu (in {@link #init()}). */
    private static final int DEMO_MAX_FAILURES = 3;

    private enum DemoPhase {
        /** No demo activity; idle timer counts up toward {@link #DEMO_IDLE_THRESHOLD}. */
        IDLE,
        /** Async fetch in progress, waiting for {@link #pendingDemo} or {@link #pendingError}. */
        LOADING,
        /** Walking through the loaded demo turn by turn via {@link #animator}. */
        ANIMATING,
        /** Goal-text overlay visible; transitions to next turn when {@link #goalTextTime} runs out. */
        GOAL_TEXT
    }

    private final MoveAnimator animator = new MoveAnimator(getBoard());
    private float idleTime = 0;
    private DemoPhase demoPhase = DemoPhase.IDLE;
    private ChessBallDemo demo;
    private int demoTurnIndex = 0;
    private int demoFailures = 0;
    private boolean demoDisabled = false;
    private float goalTextTime = 0;
    private int goalImageTextIndex = 0;
    private ChessBallWinState pendingGoalState;

    private volatile ChessBallDemo pendingDemo;
    private volatile String pendingError;

    public ChessBallMenu(MainPanel mainPanel) {
        super(mainPanel);
    }

    @Override
    protected String getTopBarTitle() {
        return Localization.getInstance().getCommon().get("topbar.menu");
    }

    @Override
    public void init() {
        this.getMainPanel().resetSize(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);

        resetDemoState();
        demoFailures = 0;
        demoDisabled = false;
        this.getBoard().reset();

        setMyMenu();
    }

    @Override
    protected int getHeight() {
        return Constants.GAME_HEIGHT;
    }

    @Override
    protected float getScale() {
        return 1f;
    }

    @Override
    public void setNeededButtonsVisible() {
        getButtonByFunction(FUNCTION_START).setVisible(true);
        getButtonByFunction(FUNCTION_TUTORIAL).setVisible(true);
        getButtonByFunction(FUNCTION_PUZZLE).setVisible(true);
        getButtonByFunction(FUNCTION_LEFT_WHITE).setVisible(true);
        getButtonByFunction(FUNCTION_RIGHT_WHITE).setVisible(true);
        getButtonByFunction(FUNCTION_LEFT_BLACK).setVisible(true);
        getButtonByFunction(FUNCTION_RIGHT_BLACK).setVisible(true);
        getButtonByFunction(FUNCTION_EXIT).setVisible(true);
        if (Constants.IS_HTML) {
            getMainPanel().getButtonByFunction(FUNCTION_EXIT).setVisible(false);
        }
    }

    @Override
    public void mouseButtonFunction(String function) {
        super.mouseButtonFunction(function);
        if (function.equals(FUNCTION_START)) {
            this.getMainPanel().changeToGame();
        } else if (function.equals(FUNCTION_PUZZLE)) {
            this.getMainPanel().changeToPuzzle();
        } else if (function.equals(FUNCTION_TUTORIAL)) {
            this.getMainPanel().changeToTutorial();
        } else if (function.equals(FUNCTION_EXIT)) {
            this.quit();
        } else if (function.equals(FUNCTION_LEFT_WHITE)) {
            this.getMainPanel().changeAI(ChessBallColor.WHITE, -1);
        } else if (function.equals(FUNCTION_RIGHT_WHITE)) {
            this.getMainPanel().changeAI(ChessBallColor.WHITE, 1);
        } else if (function.equals(FUNCTION_LEFT_BLACK)) {
            this.getMainPanel().changeAI(ChessBallColor.BLACK, -1);
        } else if (function.equals(FUNCTION_RIGHT_BLACK)) {
            this.getMainPanel().changeAI(ChessBallColor.BLACK, 1);
        }
    }

    @Override
    public void mousePressed(int x, int y, boolean isRightButton) {
        onUserClick();
        super.mousePressed(x, y, isRightButton);
    }

    @Override
    public void mouseButtonReleased(int x, int y, boolean isRightButton) {
        onUserClick();
        super.mouseButtonReleased(x, y, isRightButton);
    }

    /** Any click resets the idle timer and aborts a running demo (board → start position). */
    private void onUserClick() {
        idleTime = 0;
        if (demoPhase != DemoPhase.IDLE) {
            resetDemoState();
            this.getBoard().reset();
            Game.markDirty();
        }
    }

    private void resetDemoState() {
        animator.clear();
        demoPhase = DemoPhase.IDLE;
        demo = null;
        demoTurnIndex = 0;
        goalTextTime = 0;
        pendingGoalState = null;
        pendingDemo = null;
        pendingError = null;
    }

    /** Back from the main menu quits the app — there's nothing higher to go to. */
    @Override
    protected void quit() {
        Gdx.app.exit();
    }

    @Override
    public void dispose() {

    }

    public void readAndCreateNewLevel() {
        readAndCreateNewLevel(false);
    }

    @Override
    protected void doThink(float delta) {
        if (state == 0) {
            readAndCreateNewLevel();
        }

        // Drain async demo result. Only act if we're still LOADING — a delayed callback
        // arriving after the user clicked or left the menu is just dropped.
        if (demoPhase == DemoPhase.LOADING) {
            if (pendingDemo != null) {
                ChessBallDemo arrived = pendingDemo;
                pendingDemo = null;
                pendingError = null;
                startDemoPlayback(arrived);
            } else if (pendingError != null) {
                String err = pendingError;
                pendingError = null;
                onDemoFetchFailed(err);
            }
        } else {
            // Stale callback — discard.
            pendingDemo = null;
            pendingError = null;
        }

        switch (demoPhase) {
            case IDLE:
                if (demoDisabled) return;
                idleTime += delta;
                if (idleTime >= DEMO_IDLE_THRESHOLD) {
                    requestRandomDemo();
                }
                break;
            case LOADING:
                // Just waiting — markDirty so HTML keeps rendering the (still) start position.
                Game.markDirty();
                break;
            case ANIMATING:
                Game.markDirty();
                MoveAnimator.Phase phase = animator.tick(delta);
                if (phase == MoveAnimator.Phase.STEP_DONE && animator.isLastStepFinal()) {
                    onDemoTurnFinished();
                }
                break;
            case GOAL_TEXT:
                Game.markDirty();
                goalTextTime -= delta;
                if (goalTextTime <= 0) {
                    applyPendingGoal();
                }
                break;
        }
    }

    /** Async fetch — same pattern as {@code ChessBallPuzzle.requestRandomDemo}. */
    private void requestRandomDemo() {
        idleTime = 0;
        demoPhase = DemoPhase.LOADING;
        pendingDemo = null;
        pendingError = null;
        this.getMainPanel().getOnline().loadRandomDemo(new IOOnlineLibgdx.DemoCallback() {
            public void onDemo(int id, String solution) {
                pendingDemo = new ChessBallDemo(solution);
            }
            public void onError(String message) {
                pendingError = message;
            }
        });
    }

    private void onDemoFetchFailed(String reason) {
        demoFailures++;
        Gdx.app.log("ChessBallMenu", "demo fetch failed (" + reason + "), failures=" + demoFailures);
        if (demoFailures >= DEMO_MAX_FAILURES) {
            demoDisabled = true;
            demoPhase = DemoPhase.IDLE;
            return;
        }
        requestRandomDemo();
    }

    private void startDemoPlayback(ChessBallDemo arrived) {
        if (arrived == null || arrived.getTurnCount() == 0) {
            onDemoFetchFailed("empty demo");
            return;
        }
        this.demo = arrived;
        this.demoTurnIndex = 0;
        this.getBoard().reset();
        // reset() sets currentColor=WHITE, scores 0 — exactly the starting state we want.
        beginNextDemoTurn();
    }

    /** Advance to {@link #demoTurnIndex} and load that turn's moves into the animator.
     *  If we've run out of turns, fetch a fresh demo and play that one next. */
    private void beginNextDemoTurn() {
        if (demo == null) {
            requestRandomDemo();
            return;
        }
        // Skip empty turns (defensive — shouldn't happen in well-formed demos).
        while (demoTurnIndex < demo.getTurnCount()) {
            List<ChessBallAIMove> moves = demo.getTurn(demoTurnIndex);
            if (moves != null && !moves.isEmpty()) {
                List<ChessBallStep> steps = new ArrayList<>(moves.size());
                for (ChessBallAIMove m : moves) {
                    steps.add(new ChessBallStep(m.getStartX(), m.getStartY(),
                                                m.getDestinationX(), m.getDestinationY()));
                }
                animator.start(steps);
                demoPhase = DemoPhase.ANIMATING;
                return;
            }
            demoTurnIndex++;
        }
        // Demo exhausted — fetch the next one immediately.
        demo = null;
        requestRandomDemo();
    }

    /** Called after the animator finishes the last step of the current demo turn.
     *  Detects goals, schedules the goal-text overlay, or flips the side-to-move and
     *  proceeds to the next turn. */
    private void onDemoTurnFinished() {
        ChessBallWinState ws = this.getBoard().winCheck();
        if (ws == ChessBallWinState.WHITE_GOAL || ws == ChessBallWinState.BLACK_GOAL
                || ws == ChessBallWinState.WHITE_NO_KING || ws == ChessBallWinState.BLACK_NO_KING) {
            pendingGoalState = ws;
            goalTextTime = Constants.TEXT_TIME_IN_MILLISECONDS;
            // text indices match ChessBallGame.checkImageText
            goalImageTextIndex = (ws == ChessBallWinState.BLACK_GOAL || ws == ChessBallWinState.WHITE_NO_KING) ? 16 : 17;
            demoPhase = DemoPhase.GOAL_TEXT;
            return;
        }
        this.getBoard().nextPlayer();
        demoTurnIndex++;
        beginNextDemoTurn();
    }

    /** End of the goal-text overlay — score the goal (which resets the formation and
     *  flips the side-to-move internally) and proceed to the next demo turn or, if the
     *  match has ended, immediately fetch a fresh demo. */
    private void applyPendingGoal() {
        ChessBallWinState ws = pendingGoalState;
        pendingGoalState = null;
        goalTextTime = 0;
        if (ws == ChessBallWinState.BLACK_GOAL || ws == ChessBallWinState.WHITE_NO_KING) {
            this.getBoard().addGoalBlack();
        } else if (ws == ChessBallWinState.WHITE_GOAL || ws == ChessBallWinState.BLACK_NO_KING) {
            this.getBoard().addGoalWhite();
        }
        demoTurnIndex++;
        if (this.getBoard().isGameOver() != ChessBallWinState.GAME) {
            // Match decided — start a fresh demo from a clean board.
            demo = null;
            this.getBoard().reset();
            requestRandomDemo();
            return;
        }
        beginNextDemoTurn();
    }

    @Override
    public void render() {
        if (state == STATE_MENU) {
            renderMenu();
        }

        for (ApoButton button : this.getMainPanel().getButtons()) {
            button.render(this.getMainPanel(), 0, 0);
        }
    }

    @Override
    public void renderMenu() {
        renderBackground();

        for (ApoButton button : this.getModelButtons()) {
            button.render(this.getMainPanel(), 0, 0);
        }
    }

    private void renderBackground() {
        final int dy = Constants.TOP_BAR_HEIGHT;
        renderTopBar();

        this.getMainPanel().spriteBatch.begin();

        this.getMainPanel().spriteBatch.draw(AssetLoader.background, 0, dy);

        this.getBoard().renderBoard(this.getMainPanel());
        // Score / current-player indicator on the top strip — visible above the menu image.
        this.getBoard().renderInformations(this.getMainPanel());

        if (goalTextTime > 0) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[goalImageTextIndex],
                    15, Constants.GAME_HEIGHT / 2f - 20);
        }

        this.getMainPanel().spriteBatch.draw(AssetLoader.menu, 15, 75 + dy);

        this.getMainPanel().drawString(this.getMainPanel().getPlayerWhite().getName(), 121, 191 + dy, Constants.COLOR_BLACK, AssetLoader.font20, DrawString.MIDDLE);
        this.getMainPanel().drawString(this.getMainPanel().getPlayerWhite().getName(), 120, 190 + dy, Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);

        this.getMainPanel().drawString(this.getMainPanel().getPlayerBlack().getName(), 368, 191 + dy, Constants.COLOR_BLACK, AssetLoader.font20, DrawString.MIDDLE);
        this.getMainPanel().drawString(this.getMainPanel().getPlayerBlack().getName(), 367, 190 + dy, Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);

        this.getMainPanel().spriteBatch.end();
    }

}
