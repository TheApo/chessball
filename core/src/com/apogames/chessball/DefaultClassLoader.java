package com.apogames.chessball;

import com.apogames.chessball.ai.ChessBallPlayerAI;

import java.util.Collections;
import java.util.List;

public class DefaultClassLoader implements IClassLoader {

    @Override
    public List<ChessBallPlayerAI> loadPlayers() {
        return Collections.emptyList();
    }
}
