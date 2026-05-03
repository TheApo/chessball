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

import com.apogames.chessball.Constants;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;

/**
 * The type Game.
 */
public abstract class Game implements ApplicationListener {

    private GameScreen screen;
    private static boolean needsRender = true;
    private int lastResizeWidth = -1;
    private int lastResizeHeight = -1;

    /**
     * Instantiates a new Game.
     */
    public Game() {
    }

    @Override
    public void dispose() {
        if (screen != null) screen.hide();
    }

    @Override
    public void pause() {
        if (screen != null) screen.pause();
    }

    @Override
    public void resume() {
        markDirty();
        if (screen != null) screen.resume();
    }

    @Override
    public void render() {
        if (screen != null) {
            float deltaTime = Gdx.graphics.getDeltaTime();
            screen.think(deltaTime);
            if (Constants.IS_HTML && !needsRender) {
                return;
            }
            needsRender = false;
            screen.render(deltaTime);
        }
    }

    public static void markDirty() {
        needsRender = true;
    }

    @Override
    public void resize(int width, int height) {
        if (width != lastResizeWidth || height != lastResizeHeight) {
            lastResizeWidth = width;
            lastResizeHeight = height;
            markDirty();
        }
        if (screen != null) screen.resize(width, height);
    }

    /**
     * Sets the current screen. {@link Screen#hide()} is called on any old screen, and {@link Screen#show()} is called
     * on the new screen, if any.
     *
     * @param screen may be {@code null}
     */
    public void setScreen(GameScreen screen) {
        markDirty();
        if (this.screen != null) this.screen.hide();
        if (screen != null) {
            this.screen = screen;
            this.screen.init();
            this.screen.show();
            this.screen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        } else {
            this.screen = screen;
        }
    }

    /**
     * Gets screen.
     *
     * @return the currently active {@link Screen}.
     */
    public Screen getScreen() {
        return screen;
    }
}
