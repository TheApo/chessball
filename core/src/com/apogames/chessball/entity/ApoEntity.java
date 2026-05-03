package com.apogames.chessball.entity;

import com.apogames.chessball.backend.GameScreen;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;

/**
 * Klasse von der Button und Player erben und einige grundlegene Sachen zur
 * Verf�gung stellt
 *
 * @author Dirk Aporius
 */
public class ApoEntity {
    private float x, y, startX, startY, vecX, vecY, lastX;

    private float width, height;

    private boolean bSelect, visible, bClose, bUse, bOpaque, bMoveable, bLastOnGround, bInFunction;

    /**
     * Instantiates a new Apo entity.
     *
     * @param x      the x
     * @param y      the y
     * @param width  the width
     * @param height the height
     */
    public ApoEntity(float x, float y, float width, float height) {
        this.startX = x;
        this.startY = y;
        this.width = width;
        this.height = height;
        this.bOpaque = true;
        this.init();
    }

    /**
     * setzt die Werte auf ihre urspr�nglichen Values
     */
    public void init() {
        this.x = this.startX;
        this.lastX = this.x;
        this.y = this.startY;
        this.bSelect = false;
        this.visible = true;
        this.bLastOnGround = false;
        this.bInFunction = false;
        this.vecX = 0.0F;
        this.vecY = 0.0F;
        this.setBUse(false);
    }

    /**
     * Isb in function boolean.
     *
     * @return the boolean
     */
    public boolean isbInFunction() {
        return bInFunction;
    }

    /**
     * Sets in function.
     *
     * @param bInFunction the b in function
     */
    public void setbInFunction(boolean bInFunction) {
        this.bInFunction = bInFunction;
    }

    /**
     * Isb last on ground boolean.
     *
     * @return the boolean
     */
    public boolean isbLastOnGround() {
        return bLastOnGround;
    }

    /**
     * Sets last on ground.
     *
     * @param bLastOnGround the b last on ground
     */
    public void setbLastOnGround(boolean bLastOnGround) {
        this.bLastOnGround = bLastOnGround;
    }

    /**
     * Is moveable boolean.
     *
     * @return the boolean
     */
    public boolean isMoveable() {
        return bMoveable;
    }

    /**
     * Sets moveable.
     *
     * @param bMoveable the b moveable
     */
    public void setMoveable(boolean bMoveable) {
        this.bMoveable = bMoveable;
    }

    /**
     * gibt den Start X-Wert der Entity zur�ck, der immer gesetzt wird
     * wenn init aufgerufen wird
     *
     * @return gibt den Start X-Wert der Entity zur�ck, der immer gesetzt wird wenn init aufgerufen wird
     */
    public float getStartX() {
        return this.startX;
    }

    /**
     * setzt den Start X-Wert auf den �bergebenen
     *
     * @param startX : neuer X-Startwert
     */
    public void setStartX(float startX) {
        this.startX = startX;
    }

    /**
     * gibt den Start Y-Wert der Entity zur�ck, der immer gesetzt wird
     * wenn init aufgerufen wird
     *
     * @return gibt den Start Y-Wert der Entity zur�ck, der immer gesetzt wird wenn init aufgerufen wird
     */
    public float getStartY() {
        return this.startY;
    }

    /**
     * setzt den Start Y-Wert auf den �bergebenen
     *
     * @param startY : neuer Y-Startwert
     */
    public void setStartY(float startY) {
        this.startY = startY;
    }

    /**
     * �berpr�fung, ob Pixelgenau gepr�ft werden soll
     *
     * @return TRUE, pixelgenau, FALSE nicht
     */
    public boolean isBOpaque() {
        return this.bOpaque;
    }

    /**
     * setzt den boolean Wert, ob bei der �berpr�fung von 2 Entitys durchsichtige Sachen betrachtet werden, auf true
     * oder false
     *
     * @param bOpaque the b opaque
     */
    public void setBOpaque(boolean bOpaque) {
        this.bOpaque = bOpaque;
    }

    /**
     * gibt zur�ck, ob die Entity angezeigt werden soll oder nicht
     *
     * @return gibt zur�ck, ob die Entity angezeigt werden soll oder nicht
     */
    public boolean isVisible() {
        return this.visible;
    }

    /**
     * setzt die Sichtbarkeit der Entity auf den �bergebenen Wert
     *
     * @param visible the b visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * gibt an ob die Entity ausgew�hlt wurde oder nicht
     *
     * @return TRUE falls ausgew�hlt sonst FALSE
     */
    public boolean isSelect() {
        return this.bSelect;
    }

    /**
     * setzt den boolean Wert ob ausgew�hlt oder nicht auf den �bergebenen
     *
     * @param bSelect the b select
     */
    public void setSelect(boolean bSelect) {
        this.bSelect = bSelect;
    }

    /**
     * gibt zur�ck, ob die JumpEntity fest ist oder vom Spieler gesetzt wurde
     *
     * @return gibt zur�ck, ob die JumpEntity fest ist oder vom Spieler gesetzt wurde
     */
    public boolean isBClose() {
        return this.bClose;
    }

    /**
     * setzt die JumpEntity ob sie fest ist oder nicht auf den �bergebenen Wert
     *
     * @param bClose the b close
     */
    public void setBClose(boolean bClose) {
        this.bClose = bClose;
    }

    /**
     * gibt an, ob eine Entity schon benutzt wurde oder nicht
     *
     * @return gibt an, ob eine Entity schon benutzt wurde oder nicht
     */
    public boolean isBUse() {
        return this.bUse;
    }

    /**
     * setzt den Wert f�r die Entity, ob sie benutzt wurde oder nicht auf den
     * �bergebenen Wert
     *
     * @param bUse the b use
     */
    public void setBUse(boolean bUse) {
        this.bUse = bUse;
    }

    /**
     * gibt die Geschwindigkeit in y-Richtung zur�ck
     *
     * @return gibt die Geschwindigkeit in y-Richtung zur�ck
     */
    public float getVecY() {
        return this.vecY;
    }

    /**
     * setzt die Geschwindkeit in y-Richtung zur�ck
     *
     * @param vecY the vec y
     */
    public void setVecY(float vecY) {
        this.vecY = vecY;
    }

    /**
     * gibt die Geschwindigkeit in x-Richtung zur�ck
     *
     * @return gibt die Geschwindigkeit in x-Richtung zur�ck
     */
    public float getVecX() {
        return this.vecX;
    }

    /**
     * setzt die Geschwindkeit in x-Richtung zur�ck
     *
     * @param vecX the vec x
     */
    public void setVecX(float vecX) {
        this.vecX = vecX;
    }

    /**
     * gibt die Weite des Objektes zur�ck
     *
     * @return Weite des Objektes
     */
    public float getWidth() {
        return this.width;
    }

    /**
     * setzt die Weite des Objektes auf den �bergebenen Wert
     *
     * @param width the width
     */
    public void setWidth(float width) {
        this.width = width;
    }

    /**
     * gibt die H�he des Objektes zur�ck
     *
     * @return H �he des Objektes
     */
    public float getHeight() {
        return this.height;
    }

    /**
     * setzt die H�he des Objektes auf den �bergebenen Wert
     *
     * @param height the height
     */
    public void setHeight(float height) {
        this.height = height;
    }

    /**
     * gibt den x-Wert des Objektes zur�ck (also den linken Rand des Bildes
     *
     * @return x -Wert des Objektes
     */
    public float getX() {
        return this.x;
    }

    /**
     * gibt den mittigen x-Wert des Objektes (also die Kopfmitte sozusagen)
     *
     * @return x -Wert des Objektes
     */
    public float getXMiddle() {
        return this.x + this.width / 2;
    }

    /**
     * setzt den X-Wert auf den �bergebenen Wert
     *
     * @param x the x
     */
    public void setX(float x) {
        this.lastX = this.x;
        this.x = x;
    }

    /**
     * Gets last x.
     *
     * @return the last x
     */
    public float getLastX() {
        return this.lastX;
    }

    /**
     * gibt den y-Wert des Objektes zur�ck (also den h�chsten Punkt am Kopf)
     *
     * @return y -Wert des Objektes
     */
    public float getY() {
        return this.y;
    }

    /**
     * setzt den y-Wert des Objektes auf den �bergebenen
     *
     * @param y the y
     */
    public void setY(float y) {
        this.y = y;
    }

    /**
     * �berpr�ft, ob die �bergebenen Werte in der Entity liegen
     *
     * @param x : x-Koordinate der Maus
     * @param y : y-Koordinate der Maus
     * @return the boolean
     * @return: TRUE, falls drin, sonst FALSE
     */
    public boolean intersects(float x, float y) {
        return this.intersects(x, y, 1, 1);
    }

    /**
     * �berpr�ft, ob die �bergebenen Werte (die ein Rechteck ergeben) die Entity
     * schneiden
     *
     * @param x      :      X-Wert (links oben vom Rechteck)
     * @param y      :      Y-Wert (links oben vom Rechteck)
     * @param width  :  Breiten-Wert (wie breit ist das Rechteck)
     * @param height : H�hen-Wert (wie hoch ist das Rechteck)
     * @return TRUE, falls drin, sonst FALSE
     */
    public boolean intersects(float x, float y, float width, float height) {
        if (this.getRec().overlaps(new Rectangle(x, y, width, height))) {
            return true;
        }
        return false;
    }

    /**
     * �berpr�ft, ob die �bergebene Entity die Entity schneidet
     *
     * @param entity : zu �berpr�fende Entity
     * @return TRUE, falls drin, sonst FALSE
     */
    public boolean intersects(ApoEntity entity) {
        if (this.getRec().overlaps(entity.getRec())) {
            return true;
        }
        return false;
    }

    /**
     * �berpr�ft, ob die �bergebenen Werte (die ein Reckteck ergeben) komplett
     * in der Entity liegen
     *
     * @param x      :      X-Wert (links oben vom Rechteck)
     * @param y      :      Y-Wert (links oben vom Rechteck)
     * @param width  :  Breiten-Wert (wie breit ist das Rechteck)
     * @param height : H�hen-Wert (wie hoch ist das Rechteck)
     * @return TRUE, falls drin, sonst FALSE
     */
    public boolean contains(float x, float y, float width, float height) {
        return this.getRec().contains(new Rectangle(x, y, width, height));
    }

    /**
     * �berpr�ft, ob die �bergebene Entity komplett in der Entity liegen
     *
     * @param entity : zu �berpr�fende Entity
     * @return TRUE, falls drin, sonst FALSE
     */
    public boolean contains(ApoEntity entity) {
        return this.getRec().contains(entity.getRec());
    }

    /**
     * gibt das aktuelle Rechteck der Entity zur�ck
     *
     * @return gibt das aktuelle Rechteck der Entity zur�ck
     */
    public Rectangle getRec() {
        return new Rectangle(this.getX(), this.getY(), this.getWidth(), this.getHeight());
    }

    /**
     * Gets sub rec.
     *
     * @param source the source
     * @param part   the part
     * @return the sub rec
     */
    public Rectangle getSubRec(Rectangle source, Rectangle part) {

        // Rechtecke erzeugen
        Rectangle sub = new Rectangle();

        // get X - compared to the Rectangle
        if (source.x > part.x) {
            sub.x = 0;
        } else {
            sub.x = part.x - source.x;
        }

        if (source.y > part.y) {
            sub.y = 0;
        } else {
            sub.y = part.y - source.y;
        }

        sub.width = part.width;
        sub.height = part.height;

        return sub;
    }

    /**
     * Methode, die immer waehrend der update Methode aufgerufen wird
     *
     * @param delta : Zeit, die seit dem letzten Aufruf vergangen ist
     */
    public void think(int delta) {
    }

    /**
     * malt das Objekt
     *
     * @param screen the screen
     * @param x      the x
     * @param y      the y
     */
    public void render(GameScreen screen, int x, int y) {
        if (this.isVisible()) {
            if ((this.isSelect()) || (this.bInFunction)) {
                screen.getRenderer().begin(ShapeType.Filled);
                screen.getRenderer().setColor(255f / 255.0f, 0f / 255.0f, 0f / 255.0f, 1f);
                screen.getRenderer().rect(this.getX() + x, this.getY() + y, this.getWidth() - 1, this.getHeight() - 1);
                screen.getRenderer().end();
            }
        }
    }

    /**
     * malt das Objekt
     *
     * @param screen = Graphics2D Objekt
     */
    public void render(GameScreen screen) {
        this.render(screen, 0, 0);
    }

}
