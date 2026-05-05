package com.apogames.chessball.game.menu;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.DrawString;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.ChessBallModel;
import com.apogames.chessball.game.MainPanel;
import com.badlogic.gdx.Gdx;

public class ChessBallMenu extends ChessBallModel {

    public static final String FUNCTION_START = "menu_start";
    public static final String FUNCTION_TUTORIAL = "menu_tutorial";
    public static final String FUNCTION_PUZZLE = "menu_puzzle";
    public static final String FUNCTION_LEFT_WHITE = "menu_left_white";
    public static final String FUNCTION_RIGHT_WHITE = "menu_right_white";
    public static final String FUNCTION_LEFT_BLACK = "menu_left_black";
    public static final String FUNCTION_RIGHT_BLACK = "menu_right_black";

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
    public void mouseButtonReleased(int x, int y, boolean isRightButton) {
        super.mouseButtonReleased(x, y, isRightButton);
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

        this.getMainPanel().spriteBatch.draw(AssetLoader.menu, 15, 75 + dy);

        this.getMainPanel().drawString(this.getMainPanel().getPlayerWhite().getName(), 121, 191 + dy, Constants.COLOR_BLACK, AssetLoader.font20, DrawString.MIDDLE);
        this.getMainPanel().drawString(this.getMainPanel().getPlayerWhite().getName(), 120, 190 + dy, Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);

        this.getMainPanel().drawString(this.getMainPanel().getPlayerBlack().getName(), 368, 191 + dy, Constants.COLOR_BLACK, AssetLoader.font20, DrawString.MIDDLE);
        this.getMainPanel().drawString(this.getMainPanel().getPlayerBlack().getName(), 367, 190 + dy, Constants.COLOR_WHITE, AssetLoader.font20, DrawString.MIDDLE);

        this.getMainPanel().spriteBatch.end();
    }

}
