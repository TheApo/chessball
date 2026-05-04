package com.apogames.chessball.entity;

import com.apogames.chessball.Constants;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

public class ApoButtonImagePuzzle extends ApoButton {

	private int level;

	public ApoButtonImagePuzzle(int x, int y, int width, int height, String function, String text, int level) {
		super(x, y, width, height, function, text);

		this.level = level;
	}

	public int getLevel() {
		return level;
	}

	private static final int DIGIT_WIDTH = 20;

	public void render(GameScreen screen, int changeX, int changeY, boolean bShowTextOnly ) {
		if ( this.isVisible() ) {
			screen.spriteBatch.begin();
			screen.spriteBatch.enableBlending();
			renderImage(screen, changeX, changeY);

			// Solved-state star sits between bg and number so the level number stays
			// readable on top of it. Drawn at the button's full extent so it scales
			// with whatever size the button actually is, not the texture's native 60x40.
			if (this.isSolved()) {
				screen.spriteBatch.draw(AssetLoader.puzzle[2],
						this.getX() + changeX, this.getY() + changeY,
						this.getWidth(), this.getHeight());
			}
			renderLevelNumber(screen, changeX, changeY);
			screen.spriteBatch.end();

			renderOutline(screen, changeX, changeY);
		}
	}

	private void renderLevelNumber(GameScreen screen, int changeX, int changeY) {
		String value = String.valueOf(this.level);
		float baseX = this.getX() + changeX + this.getWidth()/2f - (value.length() * DIGIT_WIDTH) / 2f;
		float y = this.getY() + changeY + 10;
		for (int k = 0; k < value.length(); k++) {
			int digit = value.charAt(k) - '0';
			screen.spriteBatch.draw(AssetLoader.numbers[digit], baseX + k * DIGIT_WIDTH, y);
		}
	}

	public void renderOutline(GameScreen screen, int changeX, int changeY) {
		if ( this.isVisible() ) {
			if (isBPressed() || isBOver()) {
				int rem = 0;
				if (getStroke() > 1) {
					rem = getStroke()/2;
				}

				if (getStroke() > 1) {
					Gdx.gl20.glLineWidth(getStroke());
				}
				screen.getRenderer().begin(ShapeType.Line);
				if (( this.isBPressed() ) || (this.isSelect())) {
					screen.getRenderer().setColor(Constants.COLOR_RED[0], Constants.COLOR_RED[1], Constants.COLOR_RED[2], 1f);
					screen.getRenderer().roundedRectLine(this.getX() + rem + changeX, this.getY() + rem + changeY, (this.getWidth() - 1 - rem*2), (this.getHeight() - 1 - rem*2), getRounded());
				} else if ( this.isBOver() ) {
					screen.getRenderer().setColor(Constants.COLOR_YELLOW[0], Constants.COLOR_YELLOW[1], Constants.COLOR_YELLOW[2], 1f);
					screen.getRenderer().roundedRectLine(this.getX() + rem + changeX, this.getY() + rem + changeY, (this.getWidth() - 1 - rem*2), (this.getHeight() - 1 - rem*2), getRounded());
				}
				screen.getRenderer().end();

				Gdx.gl20.glLineWidth(1f);
			}
		}
	}

	public void renderImage(GameScreen screen, int changeX, int changeY) {
		screen.spriteBatch.draw(AssetLoader.puzzle[0], this.getX() + changeX, this.getY() + changeY, getWidth(), getHeight());
	}
}
