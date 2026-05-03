package com.apogames.chessball.desktop;

import com.apogames.chessball.ChessBall;
import com.apogames.chessball.Constants;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();

        config.setForegroundFPS(60);
        config.setTitle(Constants.PROGRAM_NAME);
        config.useVsync(true);
        config.setBackBufferConfig(8, 8, 8, 8, 16, 0, 2);
        config.setWindowedMode(Constants.GAME_WIDTH, Constants.GAME_HEIGHT);

        new Lwjgl3Application(new ChessBall(new DesktopClassLoader(), new DesktopAIUpdate()), config);
    }
}
