package com.apogames.chessball.entity;

import com.apogames.chessball.Constants;
import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * The type Apo button image.
 */
public class ApoButtonImage extends ApoButton {

    /**
     * Instantiates a new Apo button image.
     *
     * @param x        the x
     * @param y        the y
     * @param width    the width
     * @param height   the height
     * @param function the function
     * @param text     the text
     * @param image    the image
     */
    public ApoButtonImage(int x, int y, int width, int height, String function, String text, TextureRegion image) {
		super(x, y, width, height, function, text);
		
		super.setImage(image);
	}
	
	public void render(GameScreen screen, int changeX, int changeY, boolean bShowTextOnly ) {
		if ( this.isVisible() ) {
			screen.spriteBatch.begin();
			screen.spriteBatch.enableBlending();
			renderImage(screen, changeX, changeY);
			screen.spriteBatch.end();
			
			renderOutline(screen, changeX, changeY);

			if ((this.getText() != null) && (!this.getText().isEmpty())) {
				int addY = 0;
				if (isSolved()) {
					addY = -10;
				}
				drawString(screen,changeX + 1, changeY + addY + 1, Constants.COLOR_WHITE);
				drawString(screen,changeX, changeY + addY, Constants.COLOR_BLACK);
			}
		}
	}

	public void renderOutline(GameScreen screen, int changeX, int changeY) {
		if ( this.isVisible() ) {
			if (isBPressed() || isBOver() || this.isSelect()) {
				screen.getRenderer().begin(ShapeType.Line);
				screen.getRenderer().setColor(0f, 44f / 255f, 99f / 255f, 1f);
				if (this.isBPressed() || this.isSelect()) {
					screen.getRenderer().setColor(255f / 255.0f, 0f / 255.0f, 0f / 255.0f, 1f);
				} else if (this.isBOver()) {
					screen.getRenderer().setColor(255f / 255.0f, 255f / 255.0f, 0f / 255.0f, 1f);
				}
				if (getStroke() > 1) {
					Gdx.gl20.glLineWidth(getStroke());
				}
				screen.getRenderer().roundedRectLine(this.getX() + changeX + 1, this.getY() + changeY, getWidth() - 3, getHeight() - 1, 5);
				screen.getRenderer().end();

				Gdx.gl20.glLineWidth(1f);
			}
		}
	}

	public void renderImage(GameScreen screen, int changeX, int changeY) {
		screen.spriteBatch.draw(this.getImage(), this.getX() + changeX, this.getY() + changeY, getWidth(), getHeight());
	}
}
