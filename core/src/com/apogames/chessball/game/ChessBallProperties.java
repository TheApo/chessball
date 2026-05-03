package com.apogames.chessball.game;

import com.apogames.chessball.backend.GameProperties;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class ChessBallProperties extends GameProperties {

	public ChessBallProperties(MainPanel mainPanel) {
		super();
	}

	@Override
	public Preferences getPreferences() {
		return Gdx.app.getPreferences("ChessBallProperties");
	}

}
