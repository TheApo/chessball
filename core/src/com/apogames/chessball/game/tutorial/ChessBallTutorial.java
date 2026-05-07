package com.apogames.chessball.game.tutorial;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.Game;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.entity.TextSegment;
import com.apogames.chessball.game.ChessBallModel;
import com.apogames.chessball.game.MainPanel;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.apogames.chessball.game.enums.ChessBallWinState;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.GridPoint2;

import java.util.List;

public class ChessBallTutorial extends ChessBallModel {

    public static final String FUNCTION_MENU = "tutorial_menu";
    public static final String FUNCTION_STEP = "tutorial_nextStep";

    private boolean[] keys = new boolean[256];

    private ChessBallFigure choosenFigure = null;
    private GridPoint2 mousePosition = new GridPoint2(0,0);
    private GridPoint2 figurePosition = new GridPoint2(0,0);
    private GridPoint2 mouseDifPosition = new GridPoint2(0,0);

    private ChessBallWinState chessBallWinState = ChessBallWinState.GAME;
    private float time = 0;
    private int imageTextIndex = 0;

    private int step = 0;

    public ChessBallTutorial(MainPanel mainPanel) {
        super(mainPanel);
    }

    @Override
    protected List<TextSegment> getTopBarSegments() {
        return withSideSuffix(Localization.getInstance().getCommon().get("topbar.tutorial"));
    }

    @Override
    public void init() {
        this.getMainPanel().resetSize(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);

        this.step = 0;
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
        getButtonByFunction(FUNCTION_STEP).setVisible(true);
    }

    @Override
    public void mouseButtonFunction(String function) {
        super.mouseButtonFunction(function);
        if (function.equals(FUNCTION_MENU)) {
            this.quit();
        } else if (function.equals(FUNCTION_STEP)) {
            this.step += 1;
            if (this.step == 4) {
                this.quit();
            } else {
                this.restart();
            }
        }
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

        if (this.choosenFigure != null) {
            this.getBoard().checkToSetFigure(x, y, this.figurePosition);
        }

        ChessBallWinState winState = this.getBoard().winCheck();
        if (winState == ChessBallWinState.WHITE_GOAL) {
            this.time = Constants.TEXT_TIME_IN_MILLISECONDS;
            this.chessBallWinState = winState;
            this.checkImageText(winState);
        } else if (!this.getBoard().isOneStepPossible()) {
            if (this.step == 2) {
                this.winCheck(this.chessBallWinState);
            }
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

        if (this.step == 2 && this.getBoard().getPassesLeft() <= Constants.MAX_PASSES - 1 && this.getBoard().getPlayersMove() == 0) {
            this.step += 1;
            this.restart();
        } else if (this.step == 3 && chessBallWinState != ChessBallWinState.GAME) {
            this.step = 0;
            this.getMainPanel().changeToMenu();
        }
    }

    @Override
    public void mouseDragged(int x, int y, boolean isRightButton) {
        super.mouseDragged(x, y, isRightButton);

        if (this.step < 2) {
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

        if (this.step < 2) {
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

        if (this.step < 2) {
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

        this.turnShow();

        this.getMainPanel().getAiUpdate().reset();
        keys = new boolean[256];
        choosenFigure = null;
        this.mouseDifPosition.x = -1;
        this.figurePosition.x = -1;
        this.getBoard().deleteCircle();

        this.setPositionForStep();
        // HTML backend renders only on markDirty(); restart can be triggered from
        // doThink (auto-advance after step) where input markDirty has been consumed.
        Game.markDirty();
    }

    private void setPositionForStep() {
        for (int x = 0; x < this.getBoard().getBoard().length; x++) {
            for (int y = 0; y < this.getBoard().getBoard()[0].length; y++) {
                this.getBoard().getBoard()[x][y] = ChessBallFigure.EMPTY;
            }
        }
        if (this.step >= 2) {
            if (this.step == 2) {
                this.getBoard().getBoard()[3][2] = ChessBallFigure.WHITE_KNIGHT;
                this.getBoard().getBoard()[4][5] = ChessBallFigure.BALL;
            }
            if (this.step == 3) {
                this.getBoard().getBoard()[4][0] = ChessBallFigure.WHITE_KING;
                this.getBoard().getBoard()[5][1] = ChessBallFigure.WHITE_ROOK;
                this.getBoard().getBoard()[8][2] = ChessBallFigure.BALL;
                this.getBoard().getBoard()[8][5] = ChessBallFigure.WHITE_BISHOP;
                this.getBoard().getBoard()[2][8] = ChessBallFigure.WHITE_QUEEN;
                this.getBoard().getBoard()[4][10] = ChessBallFigure.BLACK_KING;
            }
        }
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
        // Win-text fade animates without user input — see ChessBallGame.doThink.
        if (this.time > 0) {
            Game.markDirty();
            this.time -= delta;
            if (this.time <= 0 && this.imageTextIndex <= 17) {
                this.winCheck(this.chessBallWinState);
                if (!this.isWon()) {
                    this.turnShow();
                }
            }
        }
    }

    private boolean isWon() {
        return this.chessBallWinState == ChessBallWinState.BLACK_WIN || this.chessBallWinState == ChessBallWinState.WHITE_WIN;
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
        this.getBoard().renderInformations(this.getMainPanel());

        if (this.time > 0) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[this.imageTextIndex], 15, Constants.GAME_HEIGHT/2f - 20);
        }
        if (this.step == 0) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[0], 15, 120 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[1], 15, 165 + dy);

            this.getMainPanel().spriteBatch.draw(AssetLoader.text[2], 15, 225 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[3], 15, 265 + dy);

            this.getMainPanel().spriteBatch.draw(AssetLoader.text[4], 15, 365 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[5], 15, 405 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[6], 15, 445 + dy);
        } else if (this.step == 1) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[0], 15, 120 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[1], 15, 165 + dy);

            this.getMainPanel().spriteBatch.draw(AssetLoader.text[7], 15, 225 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[8], 15, 265 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[9], 15, 305 + dy);

            this.getMainPanel().spriteBatch.draw(AssetLoader.text[10], 15, 365 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[11], 15, 405 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[12], 15, 445 + dy);
        } else if (this.step == 2) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[13], 15, 405 + dy);
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[14], 15, 445 + dy);
        } else if (this.step == 3) {
            this.getMainPanel().spriteBatch.draw(AssetLoader.text[15], 15, 225 + dy);
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