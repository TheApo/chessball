package com.apogames.chessball;

import com.apogames.chessball.ai.ChessBallPlayerAI;

import java.util.List;

public interface IClassLoader {

    public List<ChessBallPlayerAI> loadPlayers();
}
