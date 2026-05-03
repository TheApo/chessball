package com.apogames.chessball;

import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.Game;
import com.apogames.chessball.game.MainPanel;

public class ChessBall extends Game {

	private IClassLoader classLoader;
	private IAIUpdate aiUpdate;
	private MainPanel mainPanel;

	public ChessBall() {
		this.classLoader = new DefaultClassLoader();
		this.aiUpdate = new DefaultAIUpdate();
	}

	public ChessBall(IClassLoader classLoader, IAIUpdate aiUpdate) {
		this.classLoader = classLoader;
		this.aiUpdate = aiUpdate;
	}

	@Override
	public void create () {
		AssetLoader.load();
		this.mainPanel = new MainPanel(this.classLoader, this.aiUpdate);
		setScreen(this.mainPanel);
	}

	@Override
	public void dispose() {
		super.dispose();
		AssetLoader.dispose();
	}
}
