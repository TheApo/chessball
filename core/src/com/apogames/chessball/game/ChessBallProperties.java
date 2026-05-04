package com.apogames.chessball.game;

import com.apogames.chessball.backend.GameProperties;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import java.util.ArrayList;

public class ChessBallProperties extends GameProperties {

	private static final String KEY_PUZZLE_SIZE = "puzzle_solved_size";
	private static final String KEY_PUZZLE_PREFIX = "puzzle_solved_";

	private final MainPanel mainPanel;

	public ChessBallProperties(MainPanel mainPanel) {
		super();
		this.mainPanel = mainPanel;
	}

	@Override
	public Preferences getPreferences() {
		return Gdx.app.getPreferences("ChessBallProperties");
	}

	@Override
	public void writeLevel() {
		ArrayList<String> solved = mainPanel.getPuzzle().getSolvedLevels();
		getPref().putInteger(KEY_PUZZLE_SIZE, solved.size());
		for (int i = 0; i < solved.size(); i++) {
			getPref().putString(KEY_PUZZLE_PREFIX + i, solved.get(i));
		}
		getPref().flush();
	}

	@Override
	public void readLevel() {
		ArrayList<String> solved = mainPanel.getPuzzle().getSolvedLevels();
		solved.clear();
		int size = getPref().getInteger(KEY_PUZZLE_SIZE, 0);
		for (int i = 0; i < size; i++) {
			String entry = getPref().getString(KEY_PUZZLE_PREFIX + i, null);
			if (entry != null) {
				solved.add(entry);
			}
		}
	}
}
