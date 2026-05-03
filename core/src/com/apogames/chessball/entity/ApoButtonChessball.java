package com.apogames.chessball.entity;

import com.apogames.chessball.Constants;
import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * Diese Klasse handelt einen Button, die ein 3geteiltes Bild enth�lt
 * das erste wird angezeigt, wenn die Maus nicht dadr�ber ist
 * das zweite wird angezeigt, wenn die Maus �ber der Entity ist
 * das dritte wird angezeigt, wenn die Maus auf das Entity geklickt hat
 *
 * @author Dirk Aporius
 */
public class ApoButtonChessball extends ApoButton {

    /**
     * Instantiates a new Apo button chessball.
     *
     * @param x        the x
     * @param y        the y
     * @param width    the width
     * @param height   the height
     * @param function the function
     * @param text     the text
     */
    public ApoButtonChessball(int x, int y, int width, int height, String function, String text )	{
		super(x, y, width, height, function, text );
	}
	
	public void render(GameScreen screen, int changeX, int changeY, boolean bShowTextOnly ) {
		if ( this.isVisible() ) {
			if (!this.isOnlyText()) {
				int rem = 0;
				if (getStroke() > 1) {
					rem = getStroke()/2;
				}

				screen.getRenderer().begin(ShapeType.Line);
				if (getStroke() > 1) {
					Gdx.gl20.glLineWidth(getStroke());
				}
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
}
