/*
 * Copyright (c) 2005-2017 Dirk Aporius <dirk.aporius@gmail.com>
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
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.apogames.chessball.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class AssetLoader {

    private static Texture backgroundTexture;
    public static TextureRegion background;
	private static Texture menuTexture;
	public static TextureRegion menu;
	private static Texture figureTexture;
	public static TextureRegion[] figures;
	private static Texture buttonTexture;
	public static TextureRegion[] buttons;
	private static Texture textTexture;
	public static TextureRegion[] text;
	private static Texture colorTexture;
	public static TextureRegion[] color;
	private static Texture numbersTexture;
	public static TextureRegion[] numbers;
	private static Texture winnerTexture;
	public static TextureRegion winner;
	private static Texture puzzleTexture;
	public static TextureRegion[] puzzle;
	private static Texture puzzleBackgroundTexture;
	public static TextureRegion puzzleBackground;
	private static Texture textPuzzleTexture;
	public static TextureRegion[] textPuzzle;


	public static BitmapFont font40;
	public static BitmapFont font20;
	public static BitmapFont font15;
	public static BitmapFont font25;
	public static BitmapFont font30;

    public static void load() {
		final FileHandle internal = Gdx.files.internal("chessball_background.png");
		backgroundTexture = new Texture(internal);
    	backgroundTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

    	background = new TextureRegion(backgroundTexture, 0, 0, 480, 800);
		background.flip(false, true);

		final FileHandle menuInternal = Gdx.files.internal("chessball_menu_puzzle.png");
		menuTexture = new Texture(menuInternal);
		menuTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		menu = new TextureRegion(menuTexture, 0, 0, 451, 653);
		menu.flip(false, true);

		final FileHandle figureInternal = Gdx.files.internal("chessball_figures.png");
		figureTexture = new Texture(figureInternal);
		figureTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		figures = new TextureRegion[15];
		for (int i = 0; i < figures.length; i++) {
			figures[i] = new TextureRegion(figureTexture, i * 50, 0, 50, 50);
			figures[i].flip(false, true);
		}

		final FileHandle buttonInternal = Gdx.files.internal("chessball_buttons.png");
		buttonTexture = new Texture(buttonInternal);
		buttonTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		buttons = new TextureRegion[13];
		for (int i = 0; i < buttons.length; i++) {
			buttons[i] = new TextureRegion(buttonTexture, i * 120, 0, 120, 40);
			buttons[i].flip(false, true);
		}

		final FileHandle textInternal = Gdx.files.internal("chessball_text.png");
		textTexture = new Texture(textInternal);
		textTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		text = new TextureRegion[25];
		for (int i = 0; i < text.length; i++) {
			text[i] = new TextureRegion(textTexture, 0, i * 40, 450, 40);
			text[i].flip(false, true);
		}

		final FileHandle colorInternal = Gdx.files.internal("chessball_color.png");
		colorTexture = new Texture(colorInternal);
		colorTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		color = new TextureRegion[2];
		for (int i = 0; i < color.length; i++) {
			color[i] = new TextureRegion(colorTexture, i * 60, 0, 60, 20);
			color[i].flip(false, true);
		}

		final FileHandle numbersInternal = Gdx.files.internal("chessball_numbers.png");
		numbersTexture = new Texture(numbersInternal);
		numbersTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		numbers = new TextureRegion[10];
		for (int i = 0; i < numbers.length; i++) {
			numbers[i] = new TextureRegion(numbersTexture, i * 20, 0, 20, 20);
			numbers[i].flip(false, true);
		}

		final FileHandle winnerInternal = Gdx.files.internal("chessball_winner.png");
		winnerTexture = new Texture(winnerInternal);
		winnerTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		winner = new TextureRegion(winnerTexture, 0, 0, 380, 220);
		winner.flip(false, true);


		final FileHandle puzzleInternal = Gdx.files.internal("chessball_buttons_puzzle.png");
		puzzleTexture = new Texture(puzzleInternal);
		puzzleTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		puzzle = new TextureRegion[3];
		for (int i = 0; i < puzzle.length; i++) {
			puzzle[i] = new TextureRegion(puzzleTexture, i * 60, 0, 60, 40);
			puzzle[i].flip(false, true);
		}

		final FileHandle puzzleBackgroundInternal = Gdx.files.internal("chessball_puzzle_background.png");
		puzzleBackgroundTexture = new Texture(puzzleBackgroundInternal);
		puzzleBackgroundTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		puzzleBackground = new TextureRegion(puzzleBackgroundTexture, 0, 0, 450, 400);
		puzzleBackground.flip(false, true);

		final FileHandle textPuzzleInternal = Gdx.files.internal("chessball_text_puzzle.png");
		textPuzzleTexture = new Texture(textPuzzleInternal);
		textPuzzleTexture.setFilter(TextureFilter.Linear, TextureFilter.Nearest);

		textPuzzle = new TextureRegion[25];
		for (int i = 0; i < textPuzzle.length; i++) {
			textPuzzle[i] = new TextureRegion(textPuzzleTexture, 0, i * 40, 450, 40);
			textPuzzle[i].flip(false, true);
		}

		font15 = new BitmapFont(Gdx.files.internal("fonts/frutiger15.fnt"), Gdx.files.internal("fonts/frutiger15.png"), true);
		font20 = new BitmapFont(Gdx.files.internal("fonts/frutiger20.fnt"), Gdx.files.internal("fonts/frutiger20.png"), true);
		font25 = new BitmapFont(Gdx.files.internal("fonts/frutiger25.fnt"), Gdx.files.internal("fonts/frutiger25.png"), true);
		font30 = new BitmapFont(Gdx.files.internal("fonts/frutiger30.fnt"), Gdx.files.internal("fonts/frutiger30.png"), true);
		font40 = new BitmapFont(Gdx.files.internal("fonts/frutiger40.fnt"), Gdx.files.internal("fonts/frutiger40.png"), true);
    }

    public static void dispose() {
    	backgroundTexture.dispose();
		menuTexture.dispose();
		figureTexture.dispose();
		buttonTexture.dispose();
		textTexture.dispose();
		colorTexture.dispose();
		numbersTexture.dispose();
		winnerTexture.dispose();
		puzzleTexture.dispose();
		puzzleBackgroundTexture.dispose();
		textPuzzleTexture.dispose();

    	font15.dispose();
    	font20.dispose();
    	font25.dispose();
    	font30.dispose();
    	font40.dispose();
    }

}

