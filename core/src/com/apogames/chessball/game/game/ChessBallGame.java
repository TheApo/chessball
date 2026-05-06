package com.apogames.chessball.game.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.ai.You;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.backend.Game;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.entity.Dialog;
import com.apogames.chessball.game.ChessBallModel;
import com.apogames.chessball.game.MainPanel;
import com.apogames.chessball.game.MoveAnimator;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.apogames.chessball.game.enums.ChessBallWinState;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.I18NBundle;

import java.util.List;

public class ChessBallGame extends ChessBallModel {

    public static final String FUNCTION_MENU = "game_menu";
    public static final String FUNCTION_TURNEND = "game_TURNEND";
    public static final String FUNCTION_NEXT = "game_next";
    public static final String FUNCTION_BACK = "game_back";

    // End-of-game dialog geometry.
    // Y shifted by Constants.TOP_BAR_HEIGHT so the dialog sits below the in-app title bar.
    private static final int DIALOG_X = 40;
    private static final int DIALOG_Y = 200 + Constants.TOP_BAR_HEIGHT;
    private static final int DIALOG_W = 400;
    private static final int DIALOG_H = 410;

    private boolean[] keys = new boolean[256];

    private ChessBallFigure choosenFigure = null;
    private GridPoint2 mousePosition = new GridPoint2(0,0);
    private GridPoint2 figurePosition = new GridPoint2(0,0);
    private GridPoint2 mouseDifPosition = new GridPoint2(0,0);

    private ChessBallWinState chessBallWinState = ChessBallWinState.GAME;
    private float time = 0;
    private int imageTextIndex = 0;

    private String currentString;

    private final MoveAnimator animator = new MoveAnimator(getBoard());

    /** Complete-match demo string built turn-by-turn for upload at game-over.
     *  Format: {@code figX,figY,toX,toY,figType;...;#...#}. */
    private final StringBuilder currentGameString = new StringBuilder();
    private boolean demoUploaded = false;

    public ChessBallGame(MainPanel mainPanel) {
        super(mainPanel);
    }

    @Override
    protected String getTopBarTitle() {
        return Localization.getInstance().getCommon().format("topbar.game.vs",
                this.getMainPanel().getPlayerWhite().getName(),
                this.getMainPanel().getPlayerBlack().getName());
    }

    @Override
    public void init() {
        this.getMainPanel().resetSize(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);

        restart();

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
        if (Constants.IS_HTML) {
            getMainPanel().getButtonByFunction(FUNCTION_EXIT).setVisible(false);
        }
        applyDialogVisibility(false);
    }

    /** Toggle bottom-bar play buttons vs end-of-game dialog buttons. */
    private void applyDialogVisibility(boolean dialogShown) {
        getButtonByFunction(FUNCTION_MENU).setVisible(!dialogShown);
        getButtonByFunction(FUNCTION_TURNEND).setVisible(!dialogShown);
        getButtonByFunction(FUNCTION_NEXT).setVisible(dialogShown);
        getButtonByFunction(FUNCTION_BACK).setVisible(dialogShown);
    }

    @Override
    public void mouseButtonFunction(String function) {
        super.mouseButtonFunction(function);
        if (function.equals(FUNCTION_MENU)) {
            quit();
        } else if (function.equals(FUNCTION_TURNEND)) {
            this.nextPlayer();
        } else if (function.equals(FUNCTION_NEXT)) {
            this.restart();
            applyDialogVisibility(false);
        } else if (function.equals(FUNCTION_BACK)) {
            applyDialogVisibility(false);
            this.quit();
        }
    }

    private void recordStep(int fx, int fy, int tx, int ty, ChessBallFigure figure) {
        if (figure == null || figure == ChessBallFigure.EMPTY) return;
        currentGameString
            .append(fx).append(',')
            .append(fy).append(',')
            .append(tx).append(',')
            .append(ty).append(',')
            .append(figure.getFieldValue()).append(';');
    }

    private void recordTurnEnd() {
        // Avoid double-# if no step happened in this turn.
        int len = currentGameString.length();
        if (len == 0 || currentGameString.charAt(len - 1) == '#') return;
        currentGameString.append('#');
    }

    private void uploadDemoIfDone() {
        if (demoUploaded) return;
        if (!this.isWon()) return;
        recordTurnEnd(); // close the final scoring turn
        demoUploaded = true;
        this.getMainPanel().getOnline().saveDemo(currentGameString.toString());
    }

    private void nextPlayer() {
        recordTurnEnd();
        this.getBoard().nextPlayer();
        this.getMainPanel().getAiUpdate().reset();

        // Defensive: clear any stale selection state from the just-ended turn so a
        // stale figurePosition can't be used by the next player's first click.
        this.choosenFigure = null;
        this.figurePosition.x = -1;
        this.figurePosition.y = -1;
        this.mouseDifPosition.x = -1;
        this.mouseDifPosition.y = -1;
        this.getBoard().deleteCircle();

        this.turnShow();
    }

    @Override
    protected void quit() {
        this.getMainPanel().changeToMenu();
    }

    @Override
    public void mouseButtonReleased(int x, int y, boolean isRightButton) {
        super.mouseButtonReleased(x, y, isRightButton);

        // Game-over: only the dialog buttons (Next / Back) restart the match. A
        // click anywhere else used to silently restart and leave the dialog buttons
        // visible — the user lost their stats screen and saw stale buttons.
        if (this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN) {
            return;
        }

        if (!this.isHumanPlayerTurn() || isAiThinking()) {
            return;
        }

        if (this.choosenFigure != null) {
            this.getBoard().checkToSetFigure(x, y, this.figurePosition);
            ChessBallStep applied = this.getBoard().getLastStep();
            if (applied != null) {
                recordStep(applied.getFigureX(), applied.getFigureY(),
                           applied.getStepFigureX(), applied.getStepFigureY(),
                           this.getBoard().getLastStepFigure());
            }
        }

        ChessBallWinState winState = this.getBoard().winCheck();
        if (winState != ChessBallWinState.GAME) {
            this.time = Constants.TEXT_TIME_IN_MILLISECONDS;
            this.chessBallWinState = winState;
            this.checkImageText(winState);
            recordTurnEnd();
        } else if (!this.getBoard().isOneStepPossible()) {
            this.nextPlayer();
        }

        choosenFigure = null;
        this.mouseDifPosition.x = -1;
        this.figurePosition.x = -1;
        this.getBoard().deleteCircle();
    }

    private void checkImageText(ChessBallWinState chessBallWinState) {
        switch (chessBallWinState) {
            case BLACK_GOAL:
            case WHITE_NO_KING:
                this.imageTextIndex = 16;
                break;
            case WHITE_GOAL:
            case BLACK_NO_KING:
                this.imageTextIndex = 17;
                break;
        }
    }

    private void winCheck(ChessBallWinState chessBallWinState) {
        switch (chessBallWinState) {
            case BLACK_GOAL:
            case WHITE_NO_KING:
                this.getBoard().addGoalBlack();
                break;
            case WHITE_GOAL:
            case BLACK_NO_KING:
                this.getBoard().addGoalWhite();
                break;
        }
        // Goal flips the side-to-move via addGoalX → board.nextPlayer(), but the
        // turn-finish branches (animated turn / mouseButtonReleased) skipped our
        // own nextPlayer() to keep chessBallWinState=GOAL through the text fade.
        // That left getAiUpdate() holding the SCORING side's stale steps. Without
        // this reset, the next AI's aiThink() picks up that stale list and the
        // turn appears to be skipped. Critical for AI-vs-AI matches.
        this.getMainPanel().getAiUpdate().reset();

        ChessBallWinState overWinState = this.getBoard().isGameOver();
        if (overWinState != ChessBallWinState.GAME) {
            this.chessBallWinState = overWinState;
            applyDialogVisibility(true);
            uploadDemoIfDone();
        }
    }

    @Override
    public void mouseDragged(int x, int y, boolean isRightButton) {
        super.mouseDragged(x, y, isRightButton);

        if (!this.isHumanPlayerTurn() || isAiThinking()) {
            return;
        }

        if (this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN) {
            return;
        }

        this.mousePosition.x = x;
        this.mousePosition.y = y;
    }

    @Override
    public void mousePressed(int x, int y, boolean isRightButton) {
        super.mousePressed(x, y, isRightButton);

        if (!this.isHumanPlayerTurn() || isAiThinking()) {
            return;
        }

        if (this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN) {
            return;
        }

        this.choosenFigure = null;
        this.mouseMoved(x, y);

        if ((choosenFigure != null) && (this.choosenFigure != ChessBallFigure.EMPTY)) {
            this.mouseDifPosition = this.getBoard().getMouseChange(x, y);
            this.getBoard().setPossibleStepsForPosition(this.figurePosition.x, this.figurePosition.y);
        }
    }

    @Override
    public void mouseMoved(int x, int y) {
        super.mouseMoved(x, y);

        if (!this.isHumanPlayerTurn() || isAiThinking()) {
            return;
        }

        if (this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN) {
            return;
        }

        this.mousePosition.x = x;
        this.mousePosition.y = y;
        this.getBoard().getMouseOver().x = -1;
        GridPoint2 point = this.getBoard().getPointForPosition(x, y);
        if (point != null) {
            ChessBallFigure figure = this.getBoard().getBoard()[point.x][point.y];
            if (figure == ChessBallFigure.EMPTY) {
                return;
            }
            if (figure == ChessBallFigure.BALL) {
                if (this.getBoard().hasNeighborsForBall(point)) {
                    this.setChoosenFigureToMove(point, figure);
                }
            } else if ((this.getBoard().getCurrentColor() == ChessBallColor.WHITE && figure.isWhite())
                    || (this.getBoard().getCurrentColor() == ChessBallColor.BLACK && !figure.isWhite())) {
                this.setChoosenFigureToMove(point, figure);
            }

        }
    }

    private boolean isHumanPlayerTurn() {
        if (this.getBoard().getCurrentColor() == ChessBallColor.WHITE
                && this.getMainPanel().getPlayerWhite().getName() != null
                && this.getMainPanel().getPlayerWhite().getName().equals("You")) {
            return true;
        }
        if (this.getBoard().getCurrentColor() == ChessBallColor.BLACK
                && this.getMainPanel().getPlayerBlack().getName() != null
                && this.getMainPanel().getPlayerBlack().getName().equals("You")) {
            return true;
        }
        return false;
    }

    /** True while the AI's worker is computing its turn — between "AI's turn started"
     *  and "first animation step queued". Used to suppress click input and to render
     *  the centered "X is thinking…" overlay. */
    private boolean isAiThinking() {
        if (this.isHumanPlayerTurn() || this.isWon()) return false;
        if (animator.isActive()) return false;
        if (this.time > 0) return false;
        return true;
    }

    @Override
    public void keyPressed(int keyCode, char keyCharacter) {
        super.keyPressed(keyCode, keyCharacter);
        keys[keyCode] = true;
    }

    @Override
    public void keyButtonReleased(int keyCode, char character) {
        super.keyButtonReleased(keyCode, character);
        keys[keyCode] = false;
    }

    private void setChoosenFigureToMove(GridPoint2 point, ChessBallFigure figure) {
        this.getBoard().getMouseOver().x = point.x;
        this.getBoard().getMouseOver().y = point.y;
        choosenFigure = figure;
        this.figurePosition = point;
    }

    @Override
    public void dispose() {

    }

    public void readAndCreateNewLevel() {
        readAndCreateNewLevel(false);
    }

    private void restart() {
        this.getBoard().reset();

        // Fresh demo string for the new match.
        currentGameString.setLength(0);
        demoUploaded = false;

        if (this.chessBallWinState == ChessBallWinState.WHITE_WIN) {
            this.getBoard().setCurrentColor(ChessBallColor.BLACK);
        }
        this.chessBallWinState = ChessBallWinState.GAME;

        this.turnShow();

        this.getMainPanel().getAiUpdate().reset();
        animator.clear();
        keys = new boolean[256];
        choosenFigure = null;
        this.mouseDifPosition.x = -1;
        this.figurePosition.x = -1;
        this.getBoard().deleteCircle();
        // HTML backend renders only on markDirty(); restart can fire from doThink
        // (auto-restart after a draw / no-step-possible) without an input event.
        Game.markDirty();
    }

    @Override
    protected void doThink(float delta) {
        if (state == 0) {
            readAndCreateNewLevel();
        }
        if (keys[Input.Keys.R]) {
            this.restart();
            return;
        }
        if (keys[Input.Keys.F2]) {
            Constants.SHOW_COORDS = !Constants.SHOW_COORDS;
            keys[Input.Keys.F2] = false; // edge-trigger
        }
        // Any time-driven state (move animation, win-text fade, AI delay, AI thinking
        // status) changes visuals without user input. The HTML backend skips render()
        // by default unless markDirty() is called, so animations only show when we
        // explicitly request a redraw. Desktop/Android always render and don't need this.
        boolean animating = animator.isActive()
                || this.time > 0
                || (!this.isHumanPlayerTurn() && !this.isWon());
        if (animating) {
            Game.markDirty();
        }

        if (animator.isActive()) {
            MoveAnimator.Phase phase = animator.tick(delta);
            if (phase == MoveAnimator.Phase.STEP_DONE) {
                ChessBallStep done = animator.getLastFinishedStep();
                recordStep(done.getFigureX(), done.getFigureY(),
                           done.getStepFigureX(), done.getStepFigureY(),
                           animator.getLastFinishedFigure());
                if (animator.isLastStepFinal()) {
                    onAnimatedTurnFinished();
                }
            }
        } else if (this.time > 0) {
            this.time -= delta;
            if (this.time <= 0 && this.imageTextIndex <= 17) {
                this.winCheck(this.chessBallWinState);
                if (!this.isWon()) {
                    this.turnShow();
                }
            }
        } else if (!this.isHumanPlayerTurn() && !this.isWon() ) {
            this.aiThink();
        }
    }

    /** Auto-detect win after AI/animated turn finishes — mirrors the human path
     *  in {@link #mouseButtonReleased(int, int, boolean)} so the goal text + reset
     *  trigger without a click. */
    private void onAnimatedTurnFinished() {
        ChessBallWinState ws = this.getBoard().winCheck();
        if (ws == ChessBallWinState.WHITE_GOAL || ws == ChessBallWinState.BLACK_GOAL
            || ws == ChessBallWinState.WHITE_NO_KING || ws == ChessBallWinState.BLACK_NO_KING) {
            this.time = Constants.TEXT_TIME_IN_MILLISECONDS;
            this.chessBallWinState = ws;
            this.checkImageText(ws);
            recordTurnEnd();
        } else {
            this.nextPlayer();
        }
    }

    private boolean isWon() {
        return this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN;
    }

    private void aiThink() {
        if (this.getMainPanel().getAiUpdate().getUpdate() != null) {
            List<ChessBallStep> update = this.getMainPanel().getAiUpdate().getUpdate();
            if (update.isEmpty()) {
                this.nextPlayer();
            } else {
                animator.start(update);
            }
        } else if (this.getMainPanel().getAiUpdate().isRunning()) {
            if (this.getBoard().getCurrentColor() == ChessBallColor.BLACK && this.getMainPanel().getPlayerBlack().getCurrentString() != null) {
                this.currentString = this.getMainPanel().getPlayerBlack().getCurrentString();
            } else if (this.getBoard().getCurrentColor() == ChessBallColor.WHITE && this.getMainPanel().getPlayerWhite().getCurrentString() != null) {
                this.currentString = this.getMainPanel().getPlayerWhite().getCurrentString();
            }
        } else {
            try {
                if (this.getBoard().getCurrentColor() == ChessBallColor.BLACK) {
                    this.getMainPanel().getAiUpdate().update(this.getBoard(), this.getMainPanel().getPlayerBlack(), true);
                } else {
                    this.getMainPanel().getAiUpdate().update(this.getBoard(), this.getMainPanel().getPlayerWhite(), false);
                }
            } catch (Exception ex) {
                if (this.getBoard().getCurrentColor() == ChessBallColor.BLACK) {
                    this.chessBallWinState = ChessBallWinState.WHITE_WIN;
                } else {
                    this.chessBallWinState = ChessBallWinState.BLACK_WIN;
                }
                this.winCheck(this.chessBallWinState);
            }
        }
    }

    private void turnShow() {
        this.time = Constants.TEXT_TIME_IN_MILLISECONDS;
        this.imageTextIndex = this.getBoard().getCurrentColor() == ChessBallColor.BLACK ? 19 : 18;
    }

    @Override
    public void render() {
        if (state == STATE_MENU) {
            renderMenu();
        }

        if (isWon()) {
            renderWinDialog();
        } else if (isAiThinking()) {
            renderThinkingDialog();
        }

        for (ApoButton button : this.getMainPanel().getButtons()) {
            button.render(this.getMainPanel(), 0, 0);
        }

        renderTurnBorder();
    }

    /** Centered "X is thinking..." overlay shown while the AI worker computes; clicks
     *  are blocked elsewhere. Border color reflects which side is to move so the
     *  player can see whose turn the wait is for. ASCII dots — the bitmap font
     *  doesn't carry the Unicode ellipsis glyph. */
    private void renderThinkingDialog() {
        I18NBundle i18n = Localization.getInstance().getCommon();
        boolean white = this.getBoard().getCurrentColor() == ChessBallColor.WHITE;
        String side = i18n.get(white ? "dialog.white" : "dialog.black");
        String thinking = i18n.get("dialog.thinking");
        int w = 400, h = 120;
        int x = (Constants.GAME_WIDTH - w) / 2;
        int y = (Constants.GAME_HEIGHT - h) / 2;
        float[] panel = new float[]{Constants.COLOR_CLEAR[0], Constants.COLOR_CLEAR[1],
                Constants.COLOR_CLEAR[2], 0.88f};
        // font.draw renders DOWNWARD from textY, so for vertical-center with
        // ~30px font height: textY = h/2 - fontHeight/2 ≈ h/2 - 15.
        new Dialog(x, y, w, h)
                .setPanelColor(panel)
                .setBorderColor(white ? Constants.COLOR_WHITE : Constants.COLOR_BLACK)
                .addCenteredLine(side + " " + thinking + "...", AssetLoader.font30, h / 2 - 10,
                        Constants.COLOR_WHITE)
                .render(this.getMainPanel(), 0, 0);
    }

    @Override
    public void renderMenu() {
        renderBackground();

        for (ApoButton button : this.getModelButtons()) {
            button.render(this.getMainPanel(), 0, 0);
        }
    }

    private void renderBackground() {
        renderTopBar();

        this.getMainPanel().spriteBatch.begin();

        this.getMainPanel().spriteBatch.draw(AssetLoader.background, 0, Constants.TOP_BAR_HEIGHT);
        this.getBoard().renderBoard(this.getMainPanel());
        this.getBoard().renderInformations(this.getMainPanel());

        if (this.time > 0) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[this.imageTextIndex], 15, Constants.GAME_HEIGHT/2f - 20);
        }
        // win-state visuals are drawn separately in renderWinDialog (see render()).

        if (this.currentString != null) {
            this.getMainPanel().drawString(this.currentString, Constants.GAME_WIDTH/2f, Constants.GAME_HEIGHT/2f + 20, Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);
        }

        this.getMainPanel().spriteBatch.end();

        if ((this.choosenFigure != null) && (this.mouseDifPosition.x > 0)) {
            this.getMainPanel().spriteBatch.begin();
            this.getMainPanel().spriteBatch.enableBlending();
            this.getMainPanel().spriteBatch.setColor(1, 1, 1, 0.5f);
            this.getMainPanel().spriteBatch.draw(AssetLoader.figures[this.choosenFigure.getImageValue()], mousePosition.x - mouseDifPosition.x, mousePosition.y - mouseDifPosition.y);
            this.getMainPanel().spriteBatch.setColor(1, 1, 1, 1f);
            this.getMainPanel().spriteBatch.end();
        }
    }

    /**
     * End-of-game dialog: title (Glückwunsch / Schade depending on whether You won),
     * winner line, per-side statistics (Pässe/Züge/Geschlagen/Verloren). The Next +
     * Back buttons sit underneath and are rendered by {@code MainPanel.getButtons()}
     * so they stay clickable.
     */
    private void renderWinDialog() {
        boolean youWon = computeYouWon();
        boolean whiteWon = this.chessBallWinState == ChessBallWinState.WHITE_WIN;
        I18NBundle i18n = Localization.getInstance().getCommon();
        String title = i18n.get(youWon ? "dialog.congrats" : "dialog.too_bad");
        String white = i18n.get("dialog.white");
        String black = i18n.get("dialog.black");
        String winnerSide = whiteWon ? white : black;
        String winnerName = whiteWon ? this.getMainPanel().getPlayerWhite().getName()
                                     : this.getMainPanel().getPlayerBlack().getName();

        int colWhiteX = 230;
        int colBlackX = 350;

        Dialog dialog = new Dialog(DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H)
                .addCenteredLine(title, AssetLoader.font30, 30, Constants.COLOR_WHITE)
                .addCenteredLine(i18n.get("dialog.winner") + ": " + winnerSide + " (" + winnerName + ")",
                        AssetLoader.font20, 70, Constants.COLOR_WHITE)
                .addLine(white, AssetLoader.font20, colWhiteX, 120, DrawString.MIDDLE, Constants.COLOR_WHITE)
                .addLine(black, AssetLoader.font20, colBlackX, 120, DrawString.MIDDLE, Constants.COLOR_WHITE);

        addStatRow(dialog, i18n.get("dialog.passes"),
                this.getBoard().getPassesWhite(), this.getBoard().getPassesBlack(),
                colWhiteX, colBlackX, 160);
        addStatRow(dialog, i18n.get("dialog.moves"),
                this.getBoard().getMovesWhite(), this.getBoard().getMovesBlack(),
                colWhiteX, colBlackX, 195);
        addStatRow(dialog, i18n.get("dialog.captured"),
                this.getBoard().getCapturedByWhite(), this.getBoard().getCapturedByBlack(),
                colWhiteX, colBlackX, 230);
        // "Lost" = pieces of mine captured by the opponent.
        addStatRow(dialog, i18n.get("dialog.lost"),
                this.getBoard().getCapturedByBlack(), this.getBoard().getCapturedByWhite(),
                colWhiteX, colBlackX, 265);

        dialog.render(this.getMainPanel(), 0, 0);
    }

    private void addStatRow(Dialog dialog, String label, int whiteVal, int blackVal,
                            int colWhiteX, int colBlackX, int relativeY) {
        dialog.addLine(label, AssetLoader.font20, 20, relativeY, DrawString.BEGIN, Constants.COLOR_WHITE);
        dialog.addLine(String.valueOf(whiteVal), AssetLoader.font20, colWhiteX, relativeY,
                DrawString.MIDDLE, Constants.COLOR_WHITE);
        dialog.addLine(String.valueOf(blackVal), AssetLoader.font20, colBlackX, relativeY,
                DrawString.MIDDLE, Constants.COLOR_WHITE);
    }

    /**
     * "You won" if any human player is on the winning side. If both seats are You,
     * always congratulations (one of them won).
     */
    private boolean computeYouWon() {
        MainPanel mp = this.getMainPanel();
        boolean whiteIsYou = mp.getPlayerWhite() instanceof You;
        boolean blackIsYou = mp.getPlayerBlack() instanceof You;
        boolean whiteWon = this.chessBallWinState == ChessBallWinState.WHITE_WIN;
        if (whiteIsYou && blackIsYou) return true;
        if (whiteIsYou && whiteWon) return true;
        if (blackIsYou && !whiteWon) return true;
        return false;
    }

}