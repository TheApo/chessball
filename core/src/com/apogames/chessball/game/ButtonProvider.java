/*
 * Copyright (c) 2005-2013 Dirk Aporius <dirk.aporius@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 */

package com.apogames.chessball.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.common.Localization;
import com.apogames.chessball.entity.ApoButton;
import com.apogames.chessball.entity.ApoButtonChessball;
import com.apogames.chessball.entity.ApoButtonImage;
import com.apogames.chessball.entity.ApoButtonImagePuzzle;
import com.apogames.chessball.game.game.ChessBallGame;
import com.apogames.chessball.game.menu.ChessBallMenu;
import com.apogames.chessball.game.puzzle.ChessBallLevels;
import com.apogames.chessball.game.puzzle.ChessBallPuzzle;
import com.apogames.chessball.game.tutorial.ChessBallTutorial;

/**
 * Static UI button registry for the main panel.
 *
 * Click areas and positions match the ChessballAndroid 480x800 layout:
 * <ul>
 *   <li>Menu image at (15,75), 451x653.</li>
 *   <li>Start button: 175x70 at (155,285).</li>
 *   <li>Tutorial button: 320x60 at (80,460).</li>
 *   <li>Puzzle button: 320x60 at (80,600).</li>
 *   <li>AI choosers row: y=185, height=35, 25-wide arrows at x=55/160 (white) and 300/410 (black).</li>
 *   <li>Bottom-bar buttons: 120x40 at y=740, left-cluster x=30, right-cluster x=330.</li>
 *   <li>Puzzle level buttons: 60x40, grid starting (30,240), stride dx=90 / dy=70, wrap at x>400.</li>
 * </ul>
 */
public class ButtonProvider {

	private static final int BAR_Y = 740 + Constants.TOP_BAR_HEIGHT;
	private static final int BAR_LEFT_X = 30;
	private static final int BAR_RIGHT_X = 330;
	private static final int BAR_W = 120;
	private static final int BAR_H = 40;

	private final MainPanel game;

	public ButtonProvider(MainPanel game) {
		this.game = game;
	}

	public void init() {
		this.game.getButtons().clear();

		// All Y values shifted by Constants.TOP_BAR_HEIGHT to make room for the in-app title bar.
		final int dy = Constants.TOP_BAR_HEIGHT;

		// --- Main menu: mode buttons (transparent overlays on chessball_menu_puzzle.png) ---
		addChessballButton(ChessBallMenu.FUNCTION_START,    155, 285 + dy, 175, 70);
		addChessballButton(ChessBallMenu.FUNCTION_TUTORIAL,  80, 460 + dy, 320, 60);
		addChessballButton(ChessBallMenu.FUNCTION_PUZZLE,    80, 600 + dy, 320, 60);

		// --- Main menu: AI chooser arrows ---
		addChessballButton(ChessBallMenu.FUNCTION_LEFT_WHITE,   55, 185 + dy, 25, 35);
		addChessballButton(ChessBallMenu.FUNCTION_RIGHT_WHITE, 160, 185 + dy, 25, 35);
		addChessballButton(ChessBallMenu.FUNCTION_LEFT_BLACK,  300, 185 + dy, 25, 35);
		addChessballButton(ChessBallMenu.FUNCTION_RIGHT_BLACK, 410, 185 + dy, 25, 35);

		// --- Bottom bar: in-game / tutorial / puzzle navigation (button-strip indices match
		//     chessball_buttons.png frames). ---
		addBarImageButton(ChessBallGame.FUNCTION_MENU,     BAR_RIGHT_X, AssetLoader.buttons[2]);
		addBarImageButton(ChessBallGame.FUNCTION_TURNEND,  BAR_LEFT_X,  AssetLoader.buttons[1]);

		// End-of-game dialog buttons — hidden by default, toggled in ChessBallGame.applyDialogVisibility().
		// Use puzzle level button background (chessball_buttons_puzzle.png frame 0), scaled to fit text.
		addDialogImageButton(ChessBallGame.FUNCTION_NEXT,  70, 540 + dy, 140, 50,
				AssetLoader.puzzle[0], Localization.getInstance().getCommon().get("dialog.next"));
		addDialogImageButton(ChessBallGame.FUNCTION_BACK, 270, 540 + dy, 140, 50,
				AssetLoader.puzzle[0], Localization.getInstance().getCommon().get("dialog.back"));

		addBarImageButton(ChessBallMenu.FUNCTION_EXIT,     BAR_RIGHT_X, AssetLoader.buttons[3]);
		addBarImageButton(ChessBallTutorial.FUNCTION_MENU, BAR_RIGHT_X, AssetLoader.buttons[2]);
		addBarImageButton(ChessBallTutorial.FUNCTION_STEP, BAR_LEFT_X,  AssetLoader.buttons[5]);
		addBarImageButton(ChessBallPuzzle.FUNCTION_MENU,   BAR_RIGHT_X, AssetLoader.buttons[2]);
		addBarImageButton(ChessBallPuzzle.FUNCTION_RANDOM, BAR_LEFT_X,  AssetLoader.buttons[6]);

		// --- Puzzle level grid: 5 columns x N rows, starting at (30,240) ---
		final int gridStartX = 30;
		final int gridStartY = 240 + dy;
		final int gridStrideX = 90;
		final int gridStrideY = 70;
		final int btnW = 60;
		final int btnH = 40;
		int curX = gridStartX;
		int curY = gridStartY;
		for (int i = 0; i < ChessBallLevels.LEVELS.length; i++) {
			ApoButton btn = new ApoButtonImagePuzzle(curX, curY, btnW, btnH,
					ChessBallPuzzle.FUNCTION_LEVEL + i, "" + (i + 1), (i + 1));
			btn.setStroke(2);
			this.game.getButtons().add(btn);

			curX += gridStrideX;
			if (curX > 400) {
				curX = gridStartX;
				curY += gridStrideY;
			}
		}

		for (int i = 0; i < this.game.getButtons().size(); i++) {
			this.game.getButtons().get(i).setBOpaque(false);
		}
	}

	private void addChessballButton(String function, int x, int y, int w, int h) {
		ApoButton btn = new ApoButtonChessball(x, y, w, h, function, "");
		btn.setStroke(3);
		this.game.getButtons().add(btn);
	}

	private void addBarImageButton(String function, int x,
	                               com.badlogic.gdx.graphics.g2d.TextureRegion image) {
		ApoButton btn = new ApoButtonImage(x, BAR_Y, BAR_W, BAR_H, function, "", image);
		btn.setStroke(2);
		this.game.getButtons().add(btn);
	}

	/** Text-only button used inside the end-of-game dialog. Hidden by default. */
	private void addDialogButton(String function, int x, int y, int w, int h, String text) {
		ApoButton btn = new ApoButton(x, y, w, h, function, text);
		btn.setFont(AssetLoader.font20);
		btn.setStroke(2);
		btn.setVisible(false);
		this.game.getButtons().add(btn);
	}

	/** Image-backed dialog button (uses chessball_buttons_puzzle scaled to {@code w x h}). */
	private void addDialogImageButton(String function, int x, int y, int w, int h,
	                                  com.badlogic.gdx.graphics.g2d.TextureRegion image, String text) {
		ApoButton btn = new ApoButtonImage(x, y, w, h, function, text, image);
		btn.setFont(AssetLoader.font20);
		btn.setStroke(2);
		btn.setVisible(false);
		this.game.getButtons().add(btn);
	}
}
