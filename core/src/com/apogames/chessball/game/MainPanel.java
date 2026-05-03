package com.apogames.chessball.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.IAIUpdate;
import com.apogames.chessball.IClassLoader;
import com.apogames.chessball.ai.ChessBallPlayerAI;
import com.apogames.chessball.ai.Easy;
import com.apogames.chessball.ai.Hard;
import com.apogames.chessball.ai.Medium;
import com.apogames.chessball.ai.You;
import com.apogames.chessball.backend.GameProperties;
import com.apogames.chessball.backend.GameScreen;
import com.apogames.chessball.backend.ScreenModel;
import com.apogames.chessball.backend.io.IOOnlineLibgdx;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.game.ChessBallGame;
import com.apogames.chessball.game.menu.ChessBallMenu;
import com.apogames.chessball.game.puzzle.ChessBallPuzzle;
import com.apogames.chessball.game.tutorial.ChessBallTutorial;
import com.badlogic.gdx.Gdx;

import java.util.ArrayList;
import java.util.List;

public class MainPanel extends GameScreen {

    private IOOnlineLibgdx online;

    private ChessBallGame game;
    private ChessBallMenu menu;
    private ChessBallTutorial tutorial;
    private ChessBallPuzzle puzzle;

    private GameProperties gameProperties;

    private IClassLoader classLoader;

    private AIHelp playerWhite;
    private AIHelp playerBlack;

    private List<AIHelp> ais;

    private final IAIUpdate aiUpdate;

    public MainPanel(final IClassLoader classLoader, final IAIUpdate aiUpdate) {
        super();
        if (this.online == null) {
            this.online = new IOOnlineLibgdx();
        }

        // Constants.setLanguage is already called from its static block via
        // Locale.getDefault().getLanguage() — OS / browser / device locale wins.
        this.gameProperties = new ChessBallProperties(this);
        this.loadProperties();

        if (this.game == null)     this.game = new ChessBallGame(this);
        if (this.menu == null)     this.menu = new ChessBallMenu(this);
        if (this.tutorial == null) this.tutorial = new ChessBallTutorial(this);
        if (this.puzzle == null)   this.puzzle = new ChessBallPuzzle(this);

        if ((this.getButtons() == null) || (this.getButtons().isEmpty())) {
            new ButtonProvider(this).init();
        }

        if (this.ais == null) {
            this.ais = new ArrayList<>();
            this.resetAIs();
            this.playerWhite = this.ais.get(0);
            this.playerBlack = this.ais.get(1);
        }

        this.classLoader = classLoader;
        this.loadPlayers();

        this.aiUpdate = aiUpdate;

        this.changeToMenu();
    }

    private void loadPlayers() {
        List<ChessBallPlayerAI> aisLoading = this.classLoader.loadPlayers();
        if ((aisLoading != null) && (!aisLoading.isEmpty())) {
            this.resetAIs();
            for (ChessBallPlayerAI ai: aisLoading) {
                this.ais.add(new AIHelp(ai, this.ais.size()));
            }
        }
    }

    public IAIUpdate getAiUpdate() {
        return aiUpdate;
    }

    private void resetAIs() {
        this.ais.clear();
        this.ais.add(new AIHelp(new You(), 0));
        this.ais.add(new AIHelp(new Easy(), 1));
        this.ais.add(new AIHelp(new Medium(), 2));
        this.ais.add(new AIHelp(new Hard(), 3));
    }

    public void changeAI(ChessBallColor player, int add) {
        if (player == ChessBallColor.WHITE) {
            int newIndex = this.getIndexForPlayer(this.playerWhite.getIndex(), add);
            this.playerWhite = this.ais.get(newIndex);
        } else {
            int newIndex = this.getIndexForPlayer(this.playerBlack.getIndex(), add);
            this.playerBlack = this.ais.get(newIndex);
        }
    }

    private int getIndexForPlayer(int index, int add) {
        int newIndex = index + add;
        if (newIndex < 0) {
            newIndex = this.ais.size() - 1;
        }
        if (newIndex >= this.ais.size()) {
            newIndex = 0;
        }
        return newIndex;
    }

    public ChessBallPlayerAI getPlayerWhite() {
        return playerWhite.getAi();
    }

    public ChessBallPlayerAI getPlayerBlack() {
        return playerBlack.getAi();
    }

    public IOOnlineLibgdx getOnline() {
        return online;
    }

    public void loadProperties() {
    	gameProperties.readLevel();
        updateLevelChooser();
    }

    public GameProperties getGameProperties() {
		return gameProperties;
	}

    public void updateLevelChooser() {
        this.gameProperties.writeLevel();
    }

    public void setLanguage(String language) {
    	Constants.REGION = language;
    	Constants.setLanguage(language);
    }

    public final void changeToGame() {
        changeModel(game);
    }

    public final void changeToTutorial() {
        changeModel(tutorial);
    }

    public final void changeToPuzzle() {
        changeModel(puzzle);
    }
    
    public final void changeToMenu() {
        changeModel(menu);
    }

    public final void quitGame() {
        Gdx.app.exit();
    }

    private void changeModel(final ScreenModel model) {
        if (this.model != null) {
            this.model.dispose();
        }

        this.model = model;

        this.setButtonsInvisible();
        this.model.setNeededButtonsVisible();
        this.model.init();
    }
    
    public final void setButtonsInvisible() {
    	for (int i = 0; i < this.getButtons().size(); i++) {
            this.getButtons().get(i).setVisible(false);
        }
    }

    @Override
    public void think(final float delta) {
        super.think(delta);
        if (model != null) {
        	model.think(delta);
        }
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        if (model != null) {
            model.render();
            model.drawOverlay();
        }
    }

}