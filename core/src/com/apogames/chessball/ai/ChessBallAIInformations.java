package com.apogames.chessball.ai;

import java.util.ArrayList;
import java.util.List;

import com.apogames.chessball.Utils;
import com.apogames.chessball.game.ChessBallBoard;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.badlogic.gdx.math.GridPoint2;

public class ChessBallAIInformations {

    private final int scoreWhite;
    private final int scoreBlack;

    private final int leftPasses;
    private final int movesPossible;

    private final ChessBallFigure[][] board;
    private final boolean isBlack;

    public ChessBallAIInformations(ChessBallBoard board, boolean isBlack) {
        this.leftPasses = board.getPassesLeft();
        this.movesPossible = board.getPlayersMove();
        this.isBlack = isBlack;

        // switch positions for player black, that he thinks he is white
        if (isBlack) {
            this.scoreBlack = board.getScoreWhite();
            this.scoreWhite = board.getScoreBlack();

            ChessBallFigure[][] copy = Utils.getCopy(board.getBoard());
            this.board = new ChessBallFigure[copy.length][copy[0].length];
            for (int x = 0; x < this.board.length; x++) {
                for (int y = 0; y < this.board[0].length; y++) {
                    this.board[x][y] = copy[this.board.length - 1 - x][this.board[0].length - 1 - y];
                    switch (this.board[x][y]) {
                        case WHITE_QUEEN: this.board[x][y] = ChessBallFigure.BLACK_QUEEN; break;
                        case WHITE_BISHOP: this.board[x][y] = ChessBallFigure.BLACK_BISHOP; break;
                        case WHITE_KNIGHT: this.board[x][y] = ChessBallFigure.BLACK_KNIGHT; break;
                        case WHITE_KING: this.board[x][y] = ChessBallFigure.BLACK_KING; break;
                        case WHITE_PAWN: this.board[x][y] = ChessBallFigure.BLACK_PAWN; break;
                        case WHITE_ROOK: this.board[x][y] = ChessBallFigure.BLACK_ROOK; break;
                        case BLACK_QUEEN: this.board[x][y] = ChessBallFigure.WHITE_QUEEN; break;
                        case BLACK_BISHOP: this.board[x][y] = ChessBallFigure.WHITE_BISHOP; break;
                        case BLACK_KNIGHT: this.board[x][y] = ChessBallFigure.WHITE_KNIGHT; break;
                        case BLACK_KING: this.board[x][y] = ChessBallFigure.WHITE_KING; break;
                        case BLACK_PAWN: this.board[x][y] = ChessBallFigure.WHITE_PAWN; break;
                        case BLACK_ROOK: this.board[x][y] = ChessBallFigure.WHITE_ROOK; break;
                    }
                }
            }
        } else {
            this.scoreBlack = board.getScoreBlack();
            this.scoreWhite = board.getScoreWhite();

            this.board = Utils.getCopy(board.getBoard());
        }
    }

    public int getLeftPasses() {
        return leftPasses;
    }

    /** True if the original board was mirrored x↔(W-1-x), y↔(H-1-y) and colors
     *  swapped because this AI plays as black. AI logic always treats itself as
     *  white internally; un-mirror coords with this flag when logging. */
    public boolean isBlack() {
        return isBlack;
    }

    public int getScoreWhite() {
        return scoreWhite;
    }

    public int getScoreBlack() {
        return scoreBlack;
    }

    public int getMovesPossible() {
        return movesPossible;
    }

    public ChessBallFigure[][] getBoard() {
        return board;
    }

    public boolean[][] getPossibleStepsFor(ChessBallFigure[][] board, int figureX, int figureY) {
        boolean[][] canGo = new boolean[board.length][board[0].length];

        List<GridPoint2> gridPoint2s = this.getPossibleStepsPointsFor(board, figureX, figureY);
        for (GridPoint2 p : gridPoint2s) {
            canGo[p.x][p.y] = true;
        }

        return canGo;
    }

    /**
     * get a list of Points where this figure on figureX and figureY can go
     * @param board the board with the figures
     * @param figureX x value of the figure
     * @param figureY y value of the figure
     * @return a list of Points where this figure on figureX and figureY can go
     */
    public List<GridPoint2> getPossibleStepsPointsFor(ChessBallFigure[][] board, int figureX, int figureY) {
        ArrayList<GridPoint2> result = new ArrayList<>();
        if (board[figureX][figureY] != ChessBallFigure.EMPTY) {
            ChessBallBoard chessBallBoard = new ChessBallBoard();
            chessBallBoard.setBoard(board);

            chessBallBoard.setPossibleStepsForPosition(figureX, figureY);

            for (int x = 0; x < this.board.length; x++) {
                for (int y = 0; y < this.board[0].length; y++) {
                    if (chessBallBoard.getCircles()[x][y] != ChessBallFigure.EMPTY) {
                        result.add(new GridPoint2(x, y));
                    }
                }
            }
        }
        return result;
    }

    /**
     * get a boolean array where true is a field you can visit
     * @return get a boolean array where true is a field you can visit
     */
    public boolean[][] canGo() {
        ChessBallBoard chessBallBoard = new ChessBallBoard();
        return chessBallBoard.getCanGo();
    }
}
