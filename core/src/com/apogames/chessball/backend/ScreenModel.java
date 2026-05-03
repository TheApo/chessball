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

package com.apogames.chessball.backend;

import com.apogames.chessball.common.KeyCodes;
import com.apogames.chessball.game.MainPanel;
import com.badlogic.gdx.utils.Disposable;

/**
 * The type Screen model.
 */
public abstract class ScreenModel implements Disposable {

    private MainPanel mainPanel;

    /**
     * Instantiates a new Screen model.
     *
     * @param mainPanel the main panel
     */
    public ScreenModel(MainPanel mainPanel) {
        this.mainPanel = mainPanel;
    }

    /**
     * Init.
     */
    public abstract void init();

    /**
     * Think.
     *
     * @param delta the delta
     */
    public abstract void think(float delta);

    /**
     * Render.
     */
    public abstract void render();

    /**
     * Quits the screen. Override this if quitting your screen requires different behaviour.
     */
    protected void quit() {
        getMainPanel().changeToMenu();
    }

    /**
     * Sets needed buttons visible.
     */
    public void setNeededButtonsVisible() {
    	// Optional hook
    }

    /**
     * Key pressed.
     *
     * @param keyCode      the key code
     * @param keyCharacter the key character
     */
    public void keyPressed(int keyCode, char keyCharacter) {
        // Optional hook
    }

    /**
     * Key button released.
     *
     * @param button    the button
     * @param character the character
     */
    public void keyButtonReleased(int button, char character) {
        for (int exitCode : KeyCodes.EXIT) {
        	if (button == exitCode) {
        		quit();
        		break;
        	}
        	
        }
        // Optional hook
    }

    /**
     * Mouse button function.
     *
     * @param function the function
     */
    public void mouseButtonFunction(String function) {
        // Optional hook
    }

    /**
     * Mouse pressed.
     *
     * @param x             the x
     * @param y             the y
     * @param isRightButton the is right button
     */
    public void mousePressed(int x, int y, boolean isRightButton) {
        // Optional hook
    }

    /**
     * Mouse moved.
     *
     * @param x the x
     * @param y the y
     */
    public void mouseMoved(int x, int y) {
        // Optional hook
    }

    /**
     * Mouse dragged.
     *
     * @param x             the x
     * @param y             the y
     * @param isRightButton the is right button
     */
    public void mouseDragged(int x, int y, boolean isRightButton) {
        // Optional hook
    }

    /**
     * Mouse button released.
     *
     * @param x             the x
     * @param y             the y
     * @param isRightButton the is right button
     */
    public void mouseButtonReleased(int x, int y, boolean isRightButton) {
        // Optional hook
    }

    /**
     * Mouse wheel changed.
     *
     * @param changed the changed
     */
    public void mouseWheelChanged(int changed) {
        // Optional hook
    }

    /**
     * Draw overlay.
     */
    public void drawOverlay() {
        // Optional hook
    }

    /**
     * Gets main panel.
     *
     * @return the main panel
     */
    public MainPanel getMainPanel() {
        return mainPanel;
    }

}
