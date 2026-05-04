package com.apogames.chessball.game.puzzle;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.backend.Game;
import com.apogames.chessball.backend.io.IOOnlineLibgdx;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.solver.PuzzleSolver;
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

import java.util.List;

public class ChessBallPuzzle extends ChessBallModel {

    public static final String FUNCTION_MENU = "puzzle_menu";
    public static final String FUNCTION_RANDOM = "puzzle_random";
    public static final String FUNCTION_LEVEL = "puzzle_level_";

    // Start-dialog geometry (mirrors ChessBallGame.renderWinDialog style).
    private static final int DIALOG_X = 40;
    private static final int DIALOG_Y = 270;
    private static final int DIALOG_W = 400;
    private static final int DIALOG_H = 230;

    private boolean[] keys = new boolean[256];

    private ChessBallFigure choosenFigure = null;
    private GridPoint2 mousePosition = new GridPoint2(0,0);
    private GridPoint2 figurePosition = new GridPoint2(0,0);
    private GridPoint2 mouseDifPosition = new GridPoint2(0,0);

    private ChessBallWinState chessBallWinState = ChessBallWinState.GAME;
    private float time = 0;
    private int imageTextIndex = 0;

    private int level = 0;

    private boolean isMenu = true;
    /** Briefing shown after picking a level; one click anywhere dismisses it and play starts. */
    private boolean showStartDialog = false;
    /** True when the active level was loaded from a server demo (no level number, no solved tracking). */
    private boolean isRandomLevel = false;
    /** Snapshot of the current random level in 137-char format, captured after loadFromDemo
     *  builds the board. Lets restart() (after fail) reload the same random level instead of
     *  falling back to LEVELS[this.level]. */
    private String randomLevelString = null;
    /** Bound on rejected demos before we give up and drop to a local level — guards
     *  against the server returning a long run of unsolvable goal-segments. */
    private static final int RANDOM_RETRY_LIMIT = 20;
    private int randomRetryCount = 0;

    private volatile ChessBallDemo pendingDemo;
    private volatile String pendingError;

    public ChessBallPuzzle(MainPanel mainPanel) {
        super(mainPanel);
        // Lets inherited isLevelSolved(i) compare against this list — must be set before
        // setNeededButtonsVisible runs (which is called by MainPanel.changeModel BEFORE init).
        this.levels = ChessBallLevels.LEVELS;
    }

    @Override
    public void init() {
        this.getMainPanel().resetSize(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);

        this.level = 0;
        this.isMenu = true;
        this.showStartDialog = false;
        this.isRandomLevel = false;
        this.randomLevelString = null;
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
        getButtonByFunction(FUNCTION_MENU).setVisible(true);
        getButtonByFunction(FUNCTION_RANDOM).setVisible(true);
        // Level buttons live in MainPanel.getButtons() (not modelButtons), so the inherited
        // setMenuButtonVisible loop doesn't reach them — set visibility + solved state here.
        for (int i = 0; i < ChessBallLevels.LEVELS.length; i++) {
            ApoButton btn = getButtonByFunction(FUNCTION_LEVEL+i);
            btn.setVisible(true);
            btn.setSolved(isLevelSolved(i));
        }
    }

    @Override
    public void mouseButtonFunction(String function) {
        super.mouseButtonFunction(function);
        if (function.equals(FUNCTION_MENU)) {
            this.quit();
        } else if (function.equals(FUNCTION_RANDOM)) {
            requestRandomDemo();
        } else if (function.startsWith(FUNCTION_LEVEL)) {
            this.level = Integer.parseInt(function.substring(FUNCTION_LEVEL.length()));
            this.isRandomLevel = false;
            this.randomLevelString = null;
            startLevel();
            this.showStartDialog = true;
        }
    }

    private void startLevel() {
        this.isMenu = false;
        for (int i = 0; i < ChessBallLevels.LEVELS.length; i++) {
            getButtonByFunction(FUNCTION_LEVEL+i).setVisible(false);
        }
        this.restart();
    }

    /** Async fetch of a random demo; result is drained on next think tick. */
    private void requestRandomDemo() {
        pendingError = null;
        pendingDemo = null;
        this.getMainPanel().getOnline().loadRandomDemo(new IOOnlineLibgdx.DemoCallback() {
            public void onDemo(int id, String solution) {
                pendingDemo = new ChessBallDemo(solution);
            }
            public void onError(String message) {
                pendingError = message;
            }
        });
    }

    /**
     * Build a one-move-to-goal puzzle from a demo. A demo is a full match with multiple
     * goals; after each goal the real game resets to the start formation. We pick one
     * random goal-segment, reset the board, and only replay the turns of THAT segment
     * (from the previous goal +1 up to the picked goal turn, exclusive). The picked
     * goal turn itself is what the player solves. Random levels show the briefing too
     * (with a different title), but are never tracked as solved.
     */
    private void loadFromDemo(ChessBallDemo demo) {
        List<Integer> goals = demo.getGoalTurnIndices();
        if (goals.isEmpty()) {
            rejectAndRetryDemo("demo has no goals");
            return;
        }
        int pickIndex = (int) (Math.random() * goals.size());
        int pickedTurn = goals.get(pickIndex);
        int segmentStart = (pickIndex == 0) ? 0 : (goals.get(pickIndex - 1) + 1);

        this.getBoard().reset();
        for (int t = segmentStart; t < pickedTurn; t++) {
            List<ChessBallAIMove> turn = demo.getTurn(t);
            if (turn == null) continue;
            for (ChessBallAIMove move : turn) {
                this.getBoard().applyMove(move);
            }
        }

        if (!this.getBoard().hasBall()) {
            rejectAndRetryDemo("ball missing after replay (demo malformed)");
            return;
        }

        // The picked turn is what the player will execute. y==0 -> black scored,
        // so black is to move; y==14 -> white.
        List<ChessBallAIMove> goalTurn = demo.getTurn(pickedTurn);
        int goalY = goalTurn.get(goalTurn.size() - 1).getDestinationY();
        this.getBoard().setCurrentColor(goalY == 0 ? ChessBallColor.BLACK : ChessBallColor.WHITE);

        // Validate solvability BEFORE committing UI changes. A demo is a full match —
        // the picked goal-turn can require more than 1+3 if the original game took
        // multiple turns to set up. Reject and ask for another (bounded retry).
        String snapshot = snapshotBoardAsLevelString();
        List<PuzzleSolver.Step> solution = PuzzleSolver.solveLevel(snapshot);
        if (solution == null) {
            rejectAndRetryDemo("not solvable in 1+3");
            return;
        }
        randomRetryCount = 0;
        Gdx.app.log("ChessBallPuzzle",
                "Random level solution:\n" + PuzzleSolver.formatPath(solution));

        this.isRandomLevel = true;
        this.showStartDialog = true;
        this.randomLevelString = snapshot;

        this.isMenu = false;
        for (int i = 0; i < ChessBallLevels.LEVELS.length; i++) {
            getButtonByFunction(FUNCTION_LEVEL + i).setVisible(false);
        }
        this.chessBallWinState = ChessBallWinState.GAME;
        this.choosenFigure = null;
        this.mouseDifPosition.x = -1;
        this.figurePosition.x = -1;
        this.getBoard().deleteCircle();
    }

    /** Common rejection path: log the reason, increment the retry counter, fetch another
     *  demo. After {@link #RANDOM_RETRY_LIMIT} consecutive rejections, drop to a local
     *  level so the user isn't stuck waiting on an endless network loop. */
    private void rejectAndRetryDemo(String reason) {
        randomRetryCount++;
        Gdx.app.log("ChessBallPuzzle",
                "demo rejected (" + reason + "), retry " + randomRetryCount);
        if (randomRetryCount >= RANDOM_RETRY_LIMIT) {
            randomRetryCount = 0;
            fallbackToLocalLevel("too many rejected demos: " + reason);
            return;
        }
        requestRandomDemo();
    }

    /** Serialize the current board to a level-string in the same 137-char format
     *  as {@link ChessBallLevels#LEVELS} (135 cells then turn digit). */
    private String snapshotBoardAsLevelString() {
        ChessBallFigure[][] cells = this.getBoard().getBoard();
        int width = cells.length;
        int height = cells[0].length;
        char[] chars = new char[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                String code = cells[x][y].getPuzzleLevel();
                chars[y * width + x] = code.isEmpty() ? '0' : code.charAt(0);
            }
        }
        char turn = this.getBoard().getCurrentColor() == ChessBallColor.WHITE ? '0' : '1';
        return new String(chars) + turn;
    }

    private void fallbackToLocalLevel(String reason) {
        com.badlogic.gdx.Gdx.app.log("ChessBallPuzzle", "demo fallback: " + reason);
        this.level = (int) (Math.random() * ChessBallLevels.LEVELS.length);
        this.isRandomLevel = false;
        this.randomLevelString = null;
        startLevel();
        this.showStartDialog = true;
    }

    @Override
    protected void quit() {
        if (!this.isMenu) {
            this.isMenu = true;
            this.showStartDialog = false;
            setNeededButtonsVisible();
        } else {
            this.getMainPanel().changeToMenu();
        }
    }

    @Override
    public void mouseButtonReleased(int x, int y, boolean isRightButton) {
        super.mouseButtonReleased(x, y, isRightButton);

        if (this.isMenu) {
            return;
        }

        if (this.showStartDialog) {
            this.showStartDialog = false;
            return;
        }

        if (this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN) {
            this.restart();
            return;
        }

        if (this.choosenFigure != null) {
            this.getBoard().checkToSetFigure(x, y, this.figurePosition);
        }

        ChessBallWinState winState = this.getBoard().winCheck();
        if (winState == ChessBallWinState.WHITE_GOAL || winState == ChessBallWinState.BLACK_GOAL) {
            this.time = Constants.TEXT_TIME_IN_MILLISECONDS;
            this.chessBallWinState = winState;
            this.checkImageText(winState);
        } else if (!this.getBoard().isOneStepPossible()) {
            this.restart();
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
    }

    @Override
    public void mouseDragged(int x, int y, boolean isRightButton) {
        super.mouseDragged(x, y, isRightButton);

        if (this.isMenu || this.showStartDialog) {
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

        if (this.isMenu || this.showStartDialog) {
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

        if (this.isMenu || this.showStartDialog) {
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

        this.chessBallWinState = ChessBallWinState.GAME;

        this.getBoard().setCurrentColor(ChessBallColor.WHITE);

        this.getMainPanel().getAiUpdate().reset();
        keys = new boolean[256];
        choosenFigure = null;
        this.mouseDifPosition.x = -1;
        this.figurePosition.x = -1;
        this.getBoard().deleteCircle();

        this.setPositionForStep();
    }

    private void nextLevel() {
        // After solving a random level, fetch a fresh random demo instead of advancing
        // the local level index (this.level was never set for random and would point
        // at a stale or unrelated stored level).
        if (this.isRandomLevel) {
            requestRandomDemo();
            return;
        }
        this.level += 1;
        this.restart();
        // Auto-advanced level still gets a briefing — same UX as picking from the grid.
        this.showStartDialog = true;
    }

    private void setPositionForStep() {
        // Random levels were built by demo replay — replay isn't deterministic across the
        // network, so we use the snapshot taken right after loadFromDemo finished.
        if (this.isRandomLevel && this.randomLevelString != null) {
            applyLevelString(this.randomLevelString);
            return;
        }
        if (this.level == ChessBallLevels.LEVELS.length) {
            this.level = 0;
        } else if (this.level < 0) {
            this.level = ChessBallLevels.LEVELS.length - 1;
        }
        applyLevelString(ChessBallLevels.LEVELS[this.level]);
    }

    private void applyLevelString(String levelString) {
        ChessBallFigure[][] cells = this.getBoard().getBoard();
        int width = cells.length;
        int height = cells[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = ChessBallFigure.getFigure(levelString.substring(y * width + x, y * width + x + 1));
            }
        }
        // 137-char format: trailing turn + status digits
        if (levelString.length() >= width * height + 1) {
            int turn = levelString.charAt(width * height) - '0';
            this.getBoard().setCurrentColor(turn == 0 ? ChessBallColor.WHITE : ChessBallColor.BLACK);
        }
    }

    @Override
    protected void doThink(float delta) {
        if (state == 0) {
            readAndCreateNewLevel();
        }
        ChessBallDemo demo = pendingDemo;
        if (demo != null) {
            pendingDemo = null;
            loadFromDemo(demo);
        }
        if (pendingError != null) {
            String err = pendingError;
            pendingError = null;
            fallbackToLocalLevel("fetch failed: " + err);
        }
        if (keys[Input.Keys.R]) {
            this.restart();
            return;
        }
        // Win-text fade animates without user input — HTML backend skips render()
        // unless markDirty() is called, so we explicitly request redraws while the
        // fade is running. Desktop/Android always render and don't need this.
        if (this.time > 0) {
            Game.markDirty();
            this.time -= delta;
            if (this.time <= 0 && this.imageTextIndex <= 17) {
                this.winCheck(this.chessBallWinState);
                if (this.isWon()) {
                    persistSolvedIfTrackable();
                    this.nextLevel();
                }
            }
        }
    }

    private boolean isWon() {
        return this.getBoard().getScoreWhite() > 0 || this.getBoard().getScoreBlack() > 0;
    }

    private void persistSolvedIfTrackable() {
        if (this.isRandomLevel) return;
        if (this.level < 0 || this.level >= ChessBallLevels.LEVELS.length) return;
        addSolvedLevel(ChessBallLevels.LEVELS[this.level]);
    }

    @Override
    public void render() {
        renderMenu();

        if (this.showStartDialog) {
            renderStartDialog();
        }

        for (ApoButton button : this.getMainPanel().getButtons()) {
            button.render(this.getMainPanel(), 0, 0);
        }
    }

    /**
     * Briefing shown after the player picked a level: which level, which color is
     * to move, the one-move solve rule, and a "click anywhere to start" hint.
     * Visual style matches {@code ChessBallGame.renderWinDialog} (green panel + dark border).
     */
    private void renderStartDialog() {
        MainPanel mp = this.getMainPanel();

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

        com.badlogic.gdx.utils.I18NBundle i18n = Localization.getInstance().getCommon();
        boolean white = this.getBoard().getCurrentColor() == ChessBallColor.WHITE;
        String title = this.isRandomLevel
                ? i18n.get("puzzle.start.title.random")
                : i18n.format("puzzle.start.title", this.level + 1);
        String color = i18n.get(white ? "puzzle.start.color.white" : "puzzle.start.color.black");
        String rule = i18n.get("puzzle.start.rule");
        String press = i18n.get("puzzle.start.press");

        int centerX = DIALOG_X + DIALOG_W / 2;

        mp.spriteBatch.begin();
        mp.drawString(title, centerX, DIALOG_Y + 30,
                Constants.COLOR_WHITE, AssetLoader.font30, DrawString.MIDDLE);
        mp.drawString(color, centerX, DIALOG_Y + 90,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);
        mp.drawString(rule, centerX, DIALOG_Y + 130,
                Constants.COLOR_WHITE, AssetLoader.font15, DrawString.MIDDLE);
        mp.drawString(press, centerX, DIALOG_Y + DIALOG_H - 40,
                Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);
        mp.spriteBatch.end();
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

        if (this.isMenu) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.textPuzzle[0], 15, 85);
            this.getMainPanel().spriteBatch.draw(AssetLoader.puzzleBackground, 15, 170);
            this.getMainPanel().spriteBatch.draw(AssetLoader.textPuzzle[1], 15, 185);
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

}