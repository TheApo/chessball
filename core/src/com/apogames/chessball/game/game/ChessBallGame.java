package com.apogames.chessball.game.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.ai.You;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.backend.Game;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.game.ChessBallModel;
import com.apogames.chessball.game.MainPanel;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.apogames.chessball.game.enums.ChessBallWinState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class ChessBallGame extends ChessBallModel {

    public static final String FUNCTION_MENU = "game_menu";
    public static final String FUNCTION_TURNEND = "game_TURNEND";
    public static final String FUNCTION_NEXT = "game_next";
    public static final String FUNCTION_BACK = "game_back";

    // End-of-game dialog geometry.
    private static final int DIALOG_X = 40;
    private static final int DIALOG_Y = 200;
    private static final int DIALOG_W = 400;
    private static final int DIALOG_H = 410;

    private boolean[] keys = new boolean[256];

    private ChessBallFigure choosenFigure = null;
    private GridPoint2 mousePosition = new GridPoint2(0,0);
    private GridPoint2 figurePosition = new GridPoint2(0,0);
    private GridPoint2 mouseDifPosition = new GridPoint2(0,0);

    private ChessBallWinState chessBallWinState = ChessBallWinState.GAME;
    private float time = 0;
    private float timeWaitUntilMove = 0;
    private int imageTextIndex = 0;

    private String currentString;

    private List<ChessBallStep> stepsToGo;

    /** Complete-match demo string built turn-by-turn for upload at game-over.
     *  Format: {@code figX,figY,toX,toY,figType;...;#...#}. */
    private final StringBuilder currentGameString = new StringBuilder();
    private boolean demoUploaded = false;

    public ChessBallGame(MainPanel mainPanel) {
        super(mainPanel);
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

        if (this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN) {
            this.restart();
            return;
        }

        if (!this.isHumanPlayerTurn()) {
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

        if (!this.isHumanPlayerTurn()) {
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

        if (!this.isHumanPlayerTurn()) {
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

        if (!this.isHumanPlayerTurn()) {
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
        keys = new boolean[256];
        choosenFigure = null;
        this.mouseDifPosition.x = -1;
        this.figurePosition.x = -1;
        this.getBoard().deleteCircle();
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
        boolean animating = this.timeWaitUntilMove > 0
                || (this.stepsToGo != null && !this.stepsToGo.isEmpty())
                || this.time > 0
                || (!this.isHumanPlayerTurn() && !this.isWon());
        if (animating) {
            Game.markDirty();
        }

        if (this.timeWaitUntilMove > 0) {
            this.timeWaitUntilMove -= delta;
        } else if (this.stepsToGo != null && !this.stepsToGo.isEmpty()) {
            this.checkAndSetStepAndGo();
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

    private boolean isWon() {
        return this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN;
    }

    private void checkAndSetStepAndGo() {
        ChessBallStep step = this.stepsToGo.get(0);

        float modulo = 35f;

        Vector2 vector = this.getBoard()
                .getMovement()[step.getFigureX()][step.getFigureY()];
        float addX = 0;
        if (step.getStepFigureX() - step.getFigureX() < 0) {
            addX = -1f / modulo;
        } else if (step.getStepFigureX() - step.getFigureX() > 0) {
            addX = 1f / modulo;
        }
        vector.x += addX;

        float addY = 0;
        if (step.getStepFigureY() - step.getFigureY() < 0) {
            addY = -1f / modulo;
        } else if (step.getStepFigureY() - step.getFigureY() > 0) {
            addY = 1f / modulo;
        }
        vector.y += addY;

        if ((step.getStepFigureX() - step.getFigureX() < 0 && step.getStepFigureX() - step.getFigureX() >= vector.x) ||
            (step.getStepFigureX() - step.getFigureX() > 0 && step.getStepFigureX() - step.getFigureX() <= vector.x)) {
            vector.x = step.getStepFigureX() - step.getFigureX();
        }
        if ((step.getStepFigureY() - step.getFigureY() < 0 && step.getStepFigureY() - step.getFigureY() >= vector.y) ||
                (step.getStepFigureY() - step.getFigureY() > 0 && step.getStepFigureY() - step.getFigureY() <= vector.y)) {
            vector.y = step.getStepFigureY() - step.getFigureY();
        }

        if (vector.x == step.getStepFigureX() - step.getFigureX() && vector.y == step.getStepFigureY() - step.getFigureY()) {
            ChessBallFigure chessBallFigure = this.getBoard()
                    .getBoard()[step.getFigureX()][step.getFigureY()];
            ChessBallFigure capturedAtTarget = this.getBoard()
                    .getBoard()[step.getStepFigureX()][step.getStepFigureY()];
            this.getBoard().recordAction(chessBallFigure, capturedAtTarget);
            recordStep(step.getFigureX(), step.getFigureY(),
                       step.getStepFigureX(), step.getStepFigureY(), chessBallFigure);
            this.getBoard()
                    .getBoard()[step.getFigureX()][step.getFigureY()] = ChessBallFigure.EMPTY;
            this.getBoard()
                    .getBoard()[step.getStepFigureX()][step.getStepFigureY()] = chessBallFigure;
            // Reset movement offsets at BOTH endpoints. Without this, the source's
            // stale offset (set during this animation) would mis-render any later
            // piece that lands on this square, and a later step with dx=0 (or dy=0)
            // could never terminate because the termination check is "vector ==
            // delta" — a stale vector.x=1 vs delta=0 deadlocks the animation.
            this.getBoard().getMovement()[step.getFigureX()][step.getFigureY()].set(0f, 0f);
            this.getBoard().getMovement()[step.getStepFigureX()][step.getStepFigureY()].set(0f, 0f);
            this.stepsToGo.remove(step);
            this.getBoard().deleteCircle();

            if (this.stepsToGo.isEmpty()) {
                // Auto-detect win after AI/animated turn finishes — mirrors the human path
                // in mouseButtonReleased so the goal text + reset trigger without a click.
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
            } else {
                this.getBoard().setPossibleStepsForPosition(this.stepsToGo.get(0).getFigureX(), this.stepsToGo.get(0).getFigureY());
                timeWaitUntilMove = Constants.WAIT_UNTIL_MOVE_IN_MILLISECONDS;
            }
        }
    }

    private void aiThink() {
        if (this.getMainPanel().getAiUpdate().getUpdate() != null) {
            List<ChessBallStep> update = this.getMainPanel().getAiUpdate().getUpdate();
            if (update.isEmpty()) {
                this.nextPlayer();
            } else {
                this.stepsToGo = update;
                this.getBoard().setPossibleStepsForPosition(this.stepsToGo.get(0).getFigureX(), this.stepsToGo.get(0).getFigureY());
                timeWaitUntilMove = Constants.WAIT_UNTIL_MOVE_IN_MILLISECONDS;
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
        this.getMainPanel().spriteBatch.begin();

        this.getMainPanel().spriteBatch.draw(AssetLoader.background, 0, 0);
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
     * End-of-game dialog: semi-transparent green panel with dark-green border, title
     * (Glückwunsch / Schade depending on whether You won), winner line, per-side
     * statistics (Pässe/Züge/Geschlagen/Verloren), then Next + Back buttons (rendered
     * by {@code MainPanel.getButtons()} so they stay clickable).
     */
    private void renderWinDialog() {
        MainPanel mp = this.getMainPanel();

        // --- Panel + border (ShapeRenderer with alpha blending) ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        mp.getRenderer().begin(ShapeType.Filled);
        mp.getRenderer().setColor(0.05f, 0.40f, 0.05f, 0.88f);
        mp.getRenderer().roundedRect(DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H, 12);
        mp.getRenderer().end();

        Gdx.gl20.glLineWidth(3f);
        mp.getRenderer().begin(ShapeType.Line);
        mp.getRenderer().setColor(0.02f, 0.20f, 0.02f, 1f);
        mp.getRenderer().roundedRectLine(DIALOG_X, DIALOG_Y, DIALOG_W, DIALOG_H, 12);
        mp.getRenderer().end();
        Gdx.gl20.glLineWidth(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // --- Text contents (SpriteBatch) ---
        boolean youWon = computeYouWon();
        boolean whiteWon = this.chessBallWinState == ChessBallWinState.WHITE_WIN;
        com.badlogic.gdx.utils.I18NBundle i18n = Localization.getInstance().getCommon();
        String title = i18n.get(youWon ? "dialog.congrats" : "dialog.too_bad");
        String white = i18n.get("dialog.white");
        String black = i18n.get("dialog.black");
        String winnerSide = whiteWon ? white : black;
        String winnerName = whiteWon ? mp.getPlayerWhite().getName() : mp.getPlayerBlack().getName();

        int centerX = DIALOG_X + DIALOG_W / 2;

        mp.spriteBatch.begin();
        mp.drawString(title, centerX, DIALOG_Y + 30,
                Constants.COLOR_WHITE, AssetLoader.font30, DrawString.MIDDLE);
        mp.drawString(i18n.get("dialog.winner") + ": " + winnerSide + " (" + winnerName + ")",
                centerX, DIALOG_Y + 70,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);

        // Stats table — column headers, then 4 rows.
        int colWhiteX = DIALOG_X + 230;
        int colBlackX = DIALOG_X + 350;
        int rowY = DIALOG_Y + 120;

        mp.drawString(white, colWhiteX, rowY,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);
        mp.drawString(black, colBlackX, rowY,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);

        rowY += 40;
        drawStatRow(i18n.get("dialog.passes"),
                this.getBoard().getPassesWhite(), this.getBoard().getPassesBlack(),
                colWhiteX, colBlackX, rowY);
        rowY += 35;
        drawStatRow(i18n.get("dialog.moves"),
                this.getBoard().getMovesWhite(), this.getBoard().getMovesBlack(),
                colWhiteX, colBlackX, rowY);
        rowY += 35;
        drawStatRow(i18n.get("dialog.captured"),
                this.getBoard().getCapturedByWhite(), this.getBoard().getCapturedByBlack(),
                colWhiteX, colBlackX, rowY);
        rowY += 35;
        // "Lost" = pieces of mine captured by the opponent.
        drawStatRow(i18n.get("dialog.lost"),
                this.getBoard().getCapturedByBlack(), this.getBoard().getCapturedByWhite(),
                colWhiteX, colBlackX, rowY);
        mp.spriteBatch.end();
    }

    private void drawStatRow(String label, int whiteVal, int blackVal,
                             int colWhiteX, int colBlackX, int y) {
        MainPanel mp = this.getMainPanel();
        mp.drawString(label, DIALOG_X + 20, y,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.BEGIN);
        mp.drawString(String.valueOf(whiteVal), colWhiteX, y,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);
        mp.drawString(String.valueOf(blackVal), colBlackX, y,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);
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