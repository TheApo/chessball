package com.apogames.chessball.backend;

import com.badlogic.gdx.Preferences;

/**
 * The type Game properties.
 */
public abstract class GameProperties {

	private final Preferences pref;

	public GameProperties() {
		this.pref = getPreferences();
	}

	public Preferences getPref() {
		return pref;
	}

	public abstract Preferences getPreferences();

	public void writeLevel() {
		// Optional hook for subclasses
	}

	public void readLevel() {
		// Optional hook for subclasses
	}
}
