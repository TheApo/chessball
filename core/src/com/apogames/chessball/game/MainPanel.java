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
import com.apogames.chessball.backend.io.DemoCache;
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
    private DemoCache demoCache;
    /** True while a {@link #fetchDemosAsync} request is in flight. Set on the
     *  render thread before the request, cleared on either thread when the
     *  callback fires (success path bounces to render thread first). */
    private volatile boolean fetchInFlight;

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

        // Wire i18n bundle to OS / browser / device locale captured by Constants.REGION.
        Constants.setLanguage(Constants.REGION);

        if (this.game == null)     this.game = new ChessBallGame(this);
        if (this.menu == null)     this.menu = new ChessBallMenu(this);
        if (this.tutorial == null) this.tutorial = new ChessBallTutorial(this);
        if (this.puzzle == null)   this.puzzle = new ChessBallPuzzle(this);

        // Properties read solved levels into puzzle's list — needs puzzle constructed first.
        this.gameProperties = new ChessBallProperties(this);
        this.puzzle.setGameProperties(this.gameProperties);
        this.loadProperties();

        // Demo pool shares the same Preferences file. Load whatever was cached locally,
        // then fire-and-forget a delta fetch — anything new lands in the cache before
        // the user enters menu-idle or clicks puzzle-random.
        this.demoCache = new DemoCache(this.gameProperties.getPref());
        this.demoCache.load();
        this.fetchDemosAsync();

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

    public DemoCache getDemoCache() {
        return demoCache;
    }

    public ChessBallPuzzle getPuzzle() {
        return puzzle;
    }

    /** Async delta fetch: pulls everything with id > cache.maxId (server caps at 1500),
     *  merges into the cache, persists. Errors are logged and silently ignored — the
     *  game falls back to the existing local cache (or local levels if empty).
     *  No-op when a fetch is already in flight. */
    private void fetchDemosAsync() {
        if (fetchInFlight) {
            return;
        }
        fetchInFlight = true;
        final DemoCache cache = this.demoCache;
        this.online.loadDemosSince(cache.getMaxId(), new IOOnlineLibgdx.DemosCallback() {
            @Override
            public void onDemos(final java.util.List<DemoCache.Entry> demos, final int maxId) {
                // Bounce to render thread: cache mutation + Preferences flush should
                // not race with pick() calls from doThink.
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        if (!demos.isEmpty() || maxId > cache.getMaxId()) {
                            cache.merge(demos, maxId);
                            cache.persistAll();
                            Gdx.app.log("DemoCache", "merged " + demos.size()
                                    + " new demos, pool=" + cache.size() + ", maxId=" + cache.getMaxId());
                        }
                        fetchInFlight = false;
                    }
                });
            }
            @Override
            public void onError(String message) {
                fetchInFlight = false;
                Gdx.app.log("DemoCache", "fetch failed: " + message);
            }
        });
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
        // Retry the demo fetch every time we re-enter the menu as long as we
        // never got any demos yet (first run with no network at startup).
        // Once we successfully cached at least one batch (maxId > 0), we stop
        // re-fetching here — the next refresh happens on the next app start.
        if (demoCache.getMaxId() == 0 && !fetchInFlight) {
            fetchDemosAsync();
        }
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