package com.apogames.chessball.entity;

import com.apogames.chessball.Constants;
import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

/**
 * Diese Klasse handelt einen Button, die ein 3geteiltes Bild enth�lt
 * das erste wird angezeigt, wenn die Maus nicht dadr�ber ist
 * das zweite wird angezeigt, wenn die Maus �ber der Entity ist
 * das dritte wird angezeigt, wenn die Maus auf das Entity geklickt hat
 *
 * @author Dirk Aporius
 */
public class ApoButton extends ApoEntity {
	
	private int				WAIT_DELAY = 70;
	private int				wait, maxWait;
	private boolean			bWait, bFirstWait;
	private String function;
	private String text;
	private String mouseOver;
	private boolean 		bOver, bPressed, bSolved;
	private int				stroke, rounded;
	private BitmapFont font;
	private TextureRegion image;
	private TextureRegion solvedImage;
	private boolean			onlyText;

	private String id;

    /**
     * Instantiates a new Apo button.
     *
     * @param x        the x
     * @param y        the y
     * @param width    the width
     * @param height   the height
     * @param function the function
     * @param text     the text
     */
    public ApoButton(int x, int y, int width, int height, String function, String text )	{
		super(x, y, width, height );
		
		this.function	= function;
		this.text		= text;
		this.mouseOver	= null;
		this.bOver		= false;
		this.bPressed	= false;
		this.bSolved	= false;
		
		super.setBOpaque(false);
		this.wait		= 0;
		this.maxWait 	= 0;
		this.bWait		= false;
		this.bFirstWait	= true;
		this.stroke 	= 1;
		this.rounded	= 5;
		this.onlyText 	= false;
	}

	public String getMouseOver() {
		return mouseOver;
	}

	public void setMouseOver(String mouseOver) {
		this.mouseOver = mouseOver;
	}

	/**
     * Gets solved image.
     *
     * @return the solved image
     */
    public TextureRegion getSolvedImage() {
		return solvedImage;
	}

    /**
     * Sets solved image.
     *
     * @param solvedImage the solved image
     */
    public void setSolvedImage(TextureRegion solvedImage) {
		this.solvedImage = solvedImage;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

    /**
     * Is only text boolean.
     *
     * @return the boolean
     */
    public boolean isOnlyText() {
		return onlyText;
	}

    /**
     * Sets only text.
     *
     * @param onlyText the only text
     */
    public void setOnlyText(boolean onlyText) {
		this.onlyText = onlyText;
	}

    /**
     * Gets image.
     *
     * @return the image
     */
    public TextureRegion getImage() {
		return image;
	}

    /**
     * Sets image.
     *
     * @param image the image
     */
    public void setImage(TextureRegion image) {
		this.image = image;
	}

    /**
     * Gets font.
     *
     * @return the font
     */
    public BitmapFont getFont() {
		return this.font;
	}

    /**
     * Sets font.
     *
     * @param font the font
     */
    public void setFont(BitmapFont font) {
		this.font = font;
	}

    /**
     * Gets stroke.
     *
     * @return the stroke
     */
    public int getStroke() {
		return this.stroke;
	}

    /**
     * Sets stroke.
     *
     * @param stroke the stroke
     */
    public void setStroke(int stroke) {
		this.stroke = stroke;
	}

    /**
     * Gets rounded.
     *
     * @return the rounded
     */
    public int getRounded() {
		return rounded;
	}

    /**
     * Sets rounded.
     *
     * @param rounded the rounded
     */
    public void setRounded(int rounded) {
		this.rounded = rounded;
	}

    /**
     * Is solved boolean.
     *
     * @return the boolean
     */
    public boolean isSolved() {
		return bSolved;
	}

    /**
     * Sets solved.
     *
     * @param bSolved the b solved
     */
    public void setSolved(boolean bSolved) {
		this.bSolved = bSolved;
	}

    /**
     * Gets text.
     *
     * @return the text
     */
    public String getText() {
		return text;
	}

    /**
     * Sets text.
     *
     * @param text the text
     */
    public void setText(String text) {
		this.text = text;
	}

    /**
     * gibt zur�ck, ob wenn eine Maustaste gehalten wird, auch alle paar Millisekunden
     * gecheckt werden soll, ob sich was ver�ndern soll
     *
     * @return gibt zur�ck, ob wenn eine Maustaste gehalten wird, auch alle paar Millisekunden gecheckt werden soll, ob sich was ver�ndern soll
     */
    public boolean isBWait() {
		return this.bWait;
	}

    /**
     * setzt den boolean Wert ob wenn die Maustaste gehalten wird, alle paar Millisekunden
     * gecheckt werden soll, ob sich was ver�nder soll, auf den �bergebenen Wert
     *
     * @param bWait the b wait
     */
    public void setBWait(boolean bWait) {
		this.bWait = bWait;
	}

    /**
     * gibt die Wartezeit zwischen 2 Funktionsaufrufen, wenn die Maus
     * gedr�ckt gehalten wird, zur�ck
     *
     * @return gibt die Wartezeit zwischen 2 Funktionsaufrufen, wenn die Maus gedr�ckt gehalten wird, zur�ck
     */
    public int getWAIT_DELAY() {
		return this.WAIT_DELAY;
	}

    /**
     * setzt die Wartezeit zwischen 2 Funktionsaufrufen auf den
     * �bergebenen Wert
     *
     * @param wait_delay = neue Wartezeit in Millisekunden
     */
    public void setWAIT_DELAY(int wait_delay) {
		this.WAIT_DELAY = wait_delay;
	}

    /**
     * gibt zur�ck, ob die Maus �ber dem Button ist oder nicht
     *
     * @return TRUE, falls Maus dr�ber, sonst FALSE
     */
    public boolean isBOver() {
		return this.bOver;
	}

    /**
     * setzt den boolean-Wert f�r bOver auf den �bergebenen Wert
     *
     * @param bOver the b over
     */
    public void setBOver(boolean bOver) {
		this.bOver = bOver;
	}

    /**
     * gibt zur�ck, ob eine Maustaste �ber dem Button gedr�ckt ist oder nicht
     *
     * @return TRUE, falls Maustaste gedr�ckt, sonst FALSE
     */
    public boolean isBPressed()	{
		return this.bPressed;
	}

    /**
     * setzt den boolean-Wert f�r bPressed auf den �bergebenen Wert
     *
     * @param bPressed the b pressed
     */
    public void setBPressed(boolean bPressed) {
		this.bPressed = bPressed;
	}

    /**
     * gibt die Funktion des Buttons zur�ck
     *
     * @return function function
     */
    public String getFunction()	{
		return this.function;
	}

    /**
     * sezt die Funktion des Buttons auf den �bergebenen Wert
     *
     * @param function the function
     */
    public void setFunction(String function) {
		this.function = function;
	}

    /**
     * was passiert, wenn die Maus im Spielfeld bewegt wurde
     *
     * @param x : x-Koordinate der Maus
     * @param y : y-Koordinate der Maus
     * @return TRUE, falls Maus dr�ber, sonst FALSE
     */
    public boolean getMove( int x, int y ) {
		if ((!this.isBOver()) && (this.intersects(x, y)) && (this.isVisible())) {
			this.setBOver( true );
			return true;
		} else if ((this.isBOver()) && (!this.intersects(x, y))) {
			this.notOver();
			return true;
		}
		return false;
	}
	
	public void setVisible(boolean bVisible) {
		super.setVisible(bVisible);
		if (!bVisible) {
			this.notOver();
		}
	}
	
	private void notOver() {
		this.bOver		= false;
		this.bPressed	= false;
		this.wait 		= 0;
		this.maxWait	= 0;
		this.bFirstWait	= true;
	}

    /**
     * was passiert, wenn eine Maustaste im Spielfeld gedr�ckt wurde wurde
     *
     * @param x : x-Koordinate der Maus
     * @param y : y-Koordinate der Maus
     * @return TRUE, falls �ber Button Maus gedr�ckt, sonst FALSE
     */
    public boolean getPressed( int x, int y ) {
		if ( ( this.isBOver() || !this.isBOver() ) && ( this.intersects( x, y ) ) && ( this.isVisible() ) ) {
			this.setBPressed( true );
			return true;
		}
		return false;
	}

    /**
     * was passiert, wenn eine Maustaste im Spielfeld losgelassen wurde
     *
     * @param x : x-Koordinate der Maus
     * @param y : y-Koordinate der Maus
     * @return TRUE, wenn die Maustaste losgelassen wurde und der Spieler auch diesen Button gedr�ckt hatte, sonst FALSE
     */
    public boolean getReleased( int x, int y ) {
		if ((this.isBPressed()) && (this.intersects(x, y)) && (this.isVisible())) {
			this.setBPressed(false);
			this.setBOver(false);
			this.wait 		= 0;
			this.maxWait	= 0;
			this.bFirstWait	= true;
			return true;
		}
		return false;
	}

	public void releaseButton() {
		this.setBPressed(false);
		this.setBOver(false);
		this.wait 		= 0;
		this.maxWait	= 0;
		this.bFirstWait	= true;
	}

    /**
     * Gets wait.
     *
     * @return the wait
     */
    public int getWait() {
		return this.wait;
	}

	/**
	 * was passiert, wenn eine Maustaste gedr�ckt wurde und gehalten wird
	 * @param delay
	 */
	public void think( int delay ) {
		if ( !this.isBWait() ) {
			return;
		}
		if ( this.isBPressed() ) {
			this.wait += delay;
			this.maxWait += delay;
			if ( this.bFirstWait ) {
				if ( this.wait > 400) {
					this.wait -= 400;
					this.bFirstWait = false;
					return;
				}
			} else {
				if (this.maxWait > 2500) {
					if (this.wait > this.WAIT_DELAY/5) {
						this.wait -= this.WAIT_DELAY/5;
					}
				} else {
					if (this.wait > this.WAIT_DELAY) {
						this.wait -= this.WAIT_DELAY;
					}
				}
			}
		}
	}
	
	/**
	 * malt den Button an die Stelle getX() + changeX und getY() + changeY hin
	 * @param changeX: Verschiebung in x-Richtung
	 * @param changeY: Verschiebung in y-Richtung
	 */
	public void render(GameScreen screen, int changeX, int changeY ) {
		render(screen, changeX, changeY, false);
	}

    /**
     * Render.
     *
     * @param screen        the screen
     * @param changeX       the change x
     * @param changeY       the change y
     * @param bShowTextOnly the b show text only
     */
    public void render(GameScreen screen, int changeX, int changeY, boolean bShowTextOnly ) {
		if ( this.isVisible() ) {
			if (!this.isOnlyText()) {
				int rem = 0;
				if (stroke > 1) {
					rem = stroke/2;
				}
				
				if (this.image != null) {
					screen.getRenderer().begin(ShapeType.Filled);
					screen.getRenderer().setColor(Constants.COLOR_BUTTONS[0], Constants.COLOR_BUTTONS[1], Constants.COLOR_BUTTONS[2], 1f);
					screen.getRenderer().circle(this.getX() + rem + changeX + getWidth()/2, this.getY() + rem + changeY + getWidth()/2, getWidth()/2);
					screen.getRenderer().end();
					
					screen.getRenderer().begin(ShapeType.Line);
					screen.getRenderer().setColor(Constants.COLOR_WHITE[0], Constants.COLOR_WHITE[1], Constants.COLOR_WHITE[2], 1f);
					if (( this.isBPressed() )) {
						screen.getRenderer().setColor(255f/ 255.0f, 0f / 255.0f, 0f / 255.0f, 1f);
					} else if ( this.isBOver() ) {
						screen.getRenderer().setColor(255f/ 255.0f, 255f / 255.0f, 0f / 255.0f, 1f);
					}
					if (stroke > 1) {
						Gdx.gl20.glLineWidth(stroke);
					}
					screen.getRenderer().circle(this.getX() + rem + changeX + getWidth()/2, this.getY() + rem + changeY + getWidth()/2, getWidth()/2);
					screen.getRenderer().end();
					Gdx.gl20.glLineWidth(1f);
					
					screen.spriteBatch.begin();
					screen.spriteBatch.enableBlending();
					renderImage(screen, changeX, changeY);
					screen.spriteBatch.end();
				} else {
					screen.getRenderer().begin(ShapeType.Filled);
					screen.getRenderer().setColor(Constants.COLOR_BUTTONS[0], Constants.COLOR_BUTTONS[1], Constants.COLOR_BUTTONS[2], 1f);
					screen.getRenderer().roundedRect(this.getX() + rem + changeX, this.getY() + rem + changeY, (this.getWidth() - 1 - rem*2), (this.getHeight() - 1 - rem*2), rounded);
					screen.getRenderer().end();
					
					renderOutline(screen, changeX, changeY);
				}
			}
			
		}
		if (isVisible() || ((bShowTextOnly) && (isOnlyText()))) {
			if (this.text != null && this.text.length() > 0) {
				drawString(screen, changeX, changeY, Constants.COLOR_WHITE);
			}
		}
	}

    /**
     * Render outline.
     *
     * @param screen  the screen
     * @param changeX the change x
     * @param changeY the change y
     */
    public void renderOutline(GameScreen screen, int changeX, int changeY) {
		renderOutline(screen, changeX, changeY, true);
	}

    /**
     * Render outline.
     *
     * @param screen           the screen
     * @param changeX          the change x
     * @param changeY          the change y
     * @param bWithNewRenderer the b with new renderer
     */
    public void renderOutline(GameScreen screen, int changeX, int changeY, boolean bWithNewRenderer) {
		if (isVisible()) {
			int rem = 0;
			if (stroke > 1) {
				rem = stroke/2;
			}
			
			if (bWithNewRenderer) {
				screen.getRenderer().begin(ShapeType.Line);
			}
			screen.getRenderer().setColor(Constants.COLOR_WHITE[0], Constants.COLOR_WHITE[1], Constants.COLOR_WHITE[2], 1f);
			if (( this.isBPressed() ) || (this.isSelect())) {
				screen.getRenderer().setColor(Constants.COLOR_RED[0], Constants.COLOR_RED[1], Constants.COLOR_RED[2], 1f);
			} else if ( this.isBOver() ) {
				screen.getRenderer().setColor(Constants.COLOR_YELLOW[0], Constants.COLOR_YELLOW[1], Constants.COLOR_YELLOW[2], 1f);
			}
			if (stroke > 1) {
				Gdx.gl20.glLineWidth(stroke);
			}
			screen.getRenderer().roundedRectLine(this.getX() + rem + changeX, this.getY() + rem + changeY, (this.getWidth() - 1 - rem*2), (this.getHeight() - 1 - rem*2), rounded);
			if (bWithNewRenderer) {
				screen.getRenderer().end();
			}
			Gdx.gl20.glLineWidth(1f);
		}
	}

    /**
     * Render before.
     *
     * @param screen  the screen
     * @param changeX the change x
     * @param changeY the change y
     */
    public void renderBefore(GameScreen screen, int changeX, int changeY) {
		if (isVisible()) {
			
		}
	}

    /**
     * Draw string.
     *
     * @param screen  the screen
     * @param changeX the change x
     * @param changeY the change y
     * @param color   the color
     */
    public void drawString(GameScreen screen, int changeX, int changeY, float[] color) {
		Constants.glyphLayout.setText(font, text);
		float h = Constants.glyphLayout.height;
		if (( this.isBPressed() )) {
			screen.drawString(this.text, this.getX() + changeX + this.getWidth()/2, this.getY() + changeY + this.getHeight()/2 - h/2, Constants.COLOR_RED, font, true);
		} else if ( this.isBOver() ) {
			screen.drawString(this.text, this.getX() + changeX + this.getWidth()/2, this.getY() + changeY + this.getHeight()/2 - h/2, Constants.COLOR_YELLOW, font, true);
		} else {
			screen.drawString(this.text, this.getX() + changeX + this.getWidth()/2, this.getY() + changeY + this.getHeight()/2 - h/2, color, font, true);	
		}
	}

    /**
     * Render image.
     *
     * @param screen  the screen
     * @param changeX the change x
     * @param changeY the change y
     */
    public void renderImage(GameScreen screen, int changeX, int changeY) {
		renderImage(screen, this.image, changeX, changeY);
	}

    /**
     * Render image.
     *
     * @param screen  the screen
     * @param image   the image
     * @param changeX the change x
     * @param changeY the change y
     */
    public void renderImage(GameScreen screen, TextureRegion image, int changeX, int changeY) {
		screen.spriteBatch.draw(image, this.getX() + changeX + this.getWidth()/2 - image.getRegionWidth()/2, this.getY() + changeY + this.getHeight()/2 - image.getRegionHeight()/2);
	}
}
