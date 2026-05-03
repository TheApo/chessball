package com.apogames.chessball.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.asset.AssetLoader;
import com.apogames.chessball.game.enums.ChessBallColor;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.apogames.chessball.game.enums.ChessBallWinState;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;

public class ChessBallBoard {

    private static final int ADD_X = 15;
    private static final int ADD_Y = 24;

    private ChessBallColor currentColor = ChessBallColor.WHITE;

    private final boolean[][] canGo = new boolean[Constants.BOARD_COLS][Constants.BOARD_ROWS];
    private final ChessBallFigure[][] board = new ChessBallFigure[Constants.BOARD_COLS][Constants.BOARD_ROWS];
    private final ChessBallFigure[][] circles = new ChessBallFigure[Constants.BOARD_COLS][Constants.BOARD_ROWS];
    private final Vector2[][] movement = new Vector2[Constants.BOARD_COLS][Constants.BOARD_ROWS];

    private int scoreWhite = 0;
    private int scoreBlack = 0;
    private int passesLeft = Constants.MAX_PASSES;
    private int playersMove = Constants.MAX_MOVES;

    // Per-match statistics (reset only on full new game).
    private int passesWhite = 0;
    private int passesBlack = 0;
    private int movesWhite = 0;
    private int movesBlack = 0;
    private int capturedByWhite = 0;
    private int capturedByBlack = 0;

    private GridPoint2 mouseOver = new GridPoint2(-1, -1);

    /** The last move applied via {@link #checkToSetFigure(int, int, GridPoint2)} —
     *  read by {@code ChessBallGame} for demo recording. {@code null} if last call
     *  didn't apply a move. */
    private ChessBallStep lastStep;
    private ChessBallFigure lastStepFigure;

    public ChessBallBoard() {
        this.reset();
    }

    public ChessBallBoard getClone() {
        ChessBallBoard boardClone = new ChessBallBoard();

        boardClone.scoreBlack = this.scoreBlack;
        boardClone.scoreWhite = this.scoreWhite;
        boardClone.passesLeft = this.passesLeft;
        boardClone.playersMove = this.playersMove;
        boardClone.currentColor = this.currentColor;
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                boardClone.canGo[x][y] = this.canGo[x][y];
                boardClone.board[x][y] = this.board[x][y];
                boardClone.circles[x][y] = this.circles[x][y];
            }
        }

        return boardClone;
    }

    public Vector2[][] getMovement() {
        return movement;
    }

    public void reset() {
        this.setStartPosition(true);
    }

    public ChessBallColor getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(ChessBallColor currentColor) {
        this.currentColor = currentColor;
    }

    public int getScoreWhite() {
        return scoreWhite;
    }

    public int getScoreBlack() {
        return scoreBlack;
    }

    public int getPassesLeft() {
        return passesLeft;
    }

    public int getPlayersMove() {
        return playersMove;
    }

    public ChessBallWinState isGameOver() {
        if (this.scoreBlack >= Constants.NEEDED_GOALS_TO_WIN) {
            return ChessBallWinState.BLACK_WIN;
        }
        if (this.scoreWhite >= Constants.NEEDED_GOALS_TO_WIN) {
            return ChessBallWinState.WHITE_WIN;
        }
        return ChessBallWinState.GAME;
    }

    public void addGoalBlack() {
        this.scoreBlack += 1;
        this.nextPlayer();
        this.setStartPosition(false);
    }

    public void addGoalWhite() {
        this.scoreWhite += 1;
        this.nextPlayer();
        this.setStartPosition(false);
    }

    public ChessBallFigure[][] getCircles() {
        return circles;
    }

    public boolean[][] getCanGo() {
        return canGo;
    }

    public void setBoard(final ChessBallFigure[][] board) {
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                this.board[x][y] = board[x][y];
                this.circles[x][y] = ChessBallFigure.EMPTY;
            }
        }
    }

    public boolean isOneStepPossible() {
        if (this.passesLeft <= 0 && this.playersMove <= 0) {
            return false;
        }
        if (this.playersMove <= 0) {
            for (int x = 0; x < this.board.length; x++) {
                for (int y = 0; y < this.board[0].length; y++) {
                    if (this.board[x][y] == ChessBallFigure.BALL) {
                        GridPoint2 p = new GridPoint2(x, y);
                        boolean isFriendlyNeighbor = isFriendlyNeighbor(p, -1, -1);
                        isFriendlyNeighbor |= isFriendlyNeighbor(p, 0, -1);
                        isFriendlyNeighbor |= isFriendlyNeighbor(p, 1, -1);
                        isFriendlyNeighbor |= isFriendlyNeighbor(p, -1, 0);
                        isFriendlyNeighbor |= isFriendlyNeighbor(p, 1, 0);
                        isFriendlyNeighbor |= isFriendlyNeighbor(p, -1, 1);
                        isFriendlyNeighbor |= isFriendlyNeighbor(p, 0, 1);
                        isFriendlyNeighbor |= isFriendlyNeighbor(p, 1, 1);
                        return isFriendlyNeighbor;
                    }
                }
            }
        }
        return true;
    }

    private boolean isFriendlyNeighbor(GridPoint2 p, int addX, int addY) {
        boolean bWhite = this.currentColor == ChessBallColor.WHITE;
        ChessBallFigure neighbor = getNeighbor(p, addX, addY, bWhite);
        if (neighbor != ChessBallFigure.EMPTY) {
            return true;
        }
        return false;
    }

    private void setStartPosition(boolean bCompleteNew) {
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                this.movement[x][y] = new Vector2(0, 0);
                board[x][y] = ChessBallFigure.EMPTY;
                circles[x][y] = ChessBallFigure.EMPTY;
                canGo[x][y] = true;
                if ((y == 0) || (y == this.board[0].length - 1)) {
                    if (x < 3 || x > 5)
                        canGo[x][y] = false;
                }
            }
        }
        // 9x15 start formation (from ChessballAndroid: level[y][x] -> board[x][y])
        board[4][0] = ChessBallFigure.WHITE_KING;
        board[4][14] = ChessBallFigure.BLACK_KING;
        board[4][3] = ChessBallFigure.WHITE_PAWN;
        board[4][11] = ChessBallFigure.BLACK_PAWN;
        board[4][5] = ChessBallFigure.WHITE_QUEEN;
        board[4][9] = ChessBallFigure.BLACK_QUEEN;
        board[3][4] = ChessBallFigure.WHITE_KNIGHT;
        board[5][4] = ChessBallFigure.WHITE_KNIGHT;
        board[3][10] = ChessBallFigure.BLACK_KNIGHT;
        board[5][10] = ChessBallFigure.BLACK_KNIGHT;
        board[2][1] = ChessBallFigure.WHITE_BISHOP;
        board[6][1] = ChessBallFigure.WHITE_ROOK;
        board[2][13] = ChessBallFigure.BLACK_ROOK;
        board[6][13] = ChessBallFigure.BLACK_BISHOP;
        board[2][6] = ChessBallFigure.WHITE_PAWN;
        board[6][6] = ChessBallFigure.WHITE_PAWN;
        board[2][8] = ChessBallFigure.BLACK_PAWN;
        board[6][8] = ChessBallFigure.BLACK_PAWN;
        board[1][3] = ChessBallFigure.WHITE_ROOK;
        board[7][3] = ChessBallFigure.WHITE_BISHOP;
        board[1][11] = ChessBallFigure.BLACK_BISHOP;
        board[7][11] = ChessBallFigure.BLACK_ROOK;

        board[4][7] = ChessBallFigure.BALL;

        if (bCompleteNew) {
            currentColor = ChessBallColor.WHITE;
            scoreWhite = 0;
            scoreBlack = 0;
            passesWhite = 0;
            passesBlack = 0;
            movesWhite = 0;
            movesBlack = 0;
            capturedByWhite = 0;
            capturedByBlack = 0;
        }
        passesLeft = Constants.MAX_PASSES;
        playersMove = Constants.MAX_MOVES;
    }

    public int getPassesWhite()     { return passesWhite; }
    public int getPassesBlack()     { return passesBlack; }
    public int getMovesWhite()      { return movesWhite; }
    public int getMovesBlack()      { return movesBlack; }
    public int getCapturedByWhite() { return capturedByWhite; }
    public int getCapturedByBlack() { return capturedByBlack; }

    /**
     * Record a player action for statistics. Call this BEFORE applying the move on
     * the board (so {@code captured} can still be read at the destination square).
     *
     * @param mover    the figure being moved (BALL = ball pass; otherwise a piece)
     * @param captured what stood at the destination before the move (EMPTY for none)
     */
    public void recordAction(ChessBallFigure mover, ChessBallFigure captured) {
        if (mover == null || mover == ChessBallFigure.EMPTY) return;
        boolean whiteSide = (mover == ChessBallFigure.BALL)
                ? this.currentColor == ChessBallColor.WHITE
                : mover.isWhite();
        if (mover == ChessBallFigure.BALL) {
            if (whiteSide) passesWhite++; else passesBlack++;
        } else {
            if (whiteSide) movesWhite++; else movesBlack++;
        }
        if (captured != null && captured != ChessBallFigure.EMPTY && captured != ChessBallFigure.BALL) {
            if (whiteSide) capturedByWhite++; else capturedByBlack++;
        }
    }

    public GridPoint2 getMouseOver() {
        return mouseOver;
    }

    public ChessBallFigure[][] getBoard() {
        return board;
    }

    public GridPoint2 getMouseChange(int touchX, int touchY) {
        GridPoint2 p = new GridPoint2();
        p.x = touchX - ADD_X - ((touchX - ADD_X) / 50) * 50;
        p.y = touchY - ADD_Y - ((touchY - ADD_Y) / 50) * 50;
        return p;
    }

    public GridPoint2 getPointForPosition(int touchX, int touchY) {
        if (touchX < ADD_X || touchX >= ADD_X + this.board.length * 50
                || touchY < ADD_Y || touchY >= ADD_Y + this.board[0].length * 50) {
            return null;
        }
        return new GridPoint2((touchX - ADD_X) / 50,(touchY - ADD_Y) / 50);
    }

    public boolean hasBall() {
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                if (this.board[x][y] == ChessBallFigure.BALL) return true;
            }
        }
        return false;
    }

    /**
     * Apply a single demo move to the board (no validation, no animation):
     * source becomes empty, destination receives the figure encoded by
     * {@link com.apogames.chessball.game.puzzle.ChessBallAIMove#getFigure()}.
     */
    public void applyMove(com.apogames.chessball.game.puzzle.ChessBallAIMove move) {
        board[move.getStartX()][move.getStartY()] = ChessBallFigure.EMPTY;
        board[move.getDestinationX()][move.getDestinationY()] = ChessBallFigure.fromFieldValue(move.getFigure());
    }

    public void deleteCircle() {
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                circles[x][y] = ChessBallFigure.EMPTY;
            }
        }
        this.getMouseOver().x = -1;
    }

    public void setPossibleStepsForPosition(int x, int y) {
        ChessBallFigure figure = this.board[x][y];
        if (figure == ChessBallFigure.BALL && this.passesLeft <= 0) {
            return;
        }
        if (figure != ChessBallFigure.BALL && this.playersMove <= 0) {
            return;
        }
        setPossibleStepsForPosition(x, y, figure, true);
    }

    public void nextPlayer() {
        if (currentColor == ChessBallColor.WHITE) {
            currentColor = ChessBallColor.BLACK;
        } else {
            currentColor = ChessBallColor.WHITE;
        }
        passesLeft = Constants.MAX_PASSES;
        playersMove = Constants.MAX_MOVES;
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                this.movement[x][y].x = 0f;
                this.movement[x][y].y = 0f;
            }
        }
    }

    public ChessBallWinState winCheck() {
        for (int x = 3; x < 6; x++) {
            if (this.board[x][0] == ChessBallFigure.BALL) {
                return ChessBallWinState.BLACK_GOAL;
            }
            if (this.board[x][this.board[0].length - 1] == ChessBallFigure.BALL) {
                return ChessBallWinState.WHITE_GOAL;
            }
        }

        boolean bWhiteKing = false;
        boolean bBlackKing = false;
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                if (this.board[x][y] == ChessBallFigure.WHITE_KING) {
                    bWhiteKing = true;
                }
                if (this.board[x][y] == ChessBallFigure.BLACK_KING) {
                    bBlackKing = true;
                }
            }
        }
        if (!bBlackKing) {
            return ChessBallWinState.BLACK_NO_KING;
        }
        if (!bWhiteKing) {
            return ChessBallWinState.WHITE_NO_KING;
        }
        return ChessBallWinState.GAME;
    }

    public void setPossibleStepsForPosition(int x, int y, ChessBallFigure figure, boolean bYellow) {
        if ((figure == ChessBallFigure.BLACK_BISHOP || figure == ChessBallFigure.WHITE_BISHOP) ||
            (figure == ChessBallFigure.BLACK_QUEEN || figure == ChessBallFigure.WHITE_QUEEN) ||
            (figure == ChessBallFigure.BLACK_KING || figure == ChessBallFigure.WHITE_KING)) {
            boolean bNext = !(figure == ChessBallFigure.BLACK_KING || figure == ChessBallFigure.WHITE_KING);
            addCircle(figure, x, y, 1, 1, bNext, bYellow);
            addCircle(figure, x, y, -1, -1, bNext, bYellow);
            addCircle(figure, x, y, 1, -1, bNext, bYellow);
            addCircle(figure, x, y, -1, 1, bNext, bYellow);
        }
        if ((figure == ChessBallFigure.BLACK_ROOK || figure == ChessBallFigure.WHITE_ROOK) ||
            (figure == ChessBallFigure.BLACK_QUEEN || figure == ChessBallFigure.WHITE_QUEEN) ||
            (figure == ChessBallFigure.BLACK_KING || figure == ChessBallFigure.WHITE_KING)) {
            boolean bNext = !(figure == ChessBallFigure.BLACK_KING || figure == ChessBallFigure.WHITE_KING);
            addCircle(figure, x, y, 1, 0, bNext, bYellow);
            addCircle(figure, x, y, -1, 0, bNext, bYellow);
            addCircle(figure, x, y, 0, -1, bNext, bYellow);
            addCircle(figure, x, y, 0, 1, bNext, bYellow);
        }
        if (figure == ChessBallFigure.BLACK_KNIGHT || figure == ChessBallFigure.WHITE_KNIGHT) {
            boolean bNext = false;
            addCircle(figure, x, y, 1, -2, bNext, bYellow);
            addCircle(figure, x, y, 1, 2, bNext, bYellow);
            addCircle(figure, x, y, -1, -2, bNext, bYellow);
            addCircle(figure, x, y, -1, 2, bNext, bYellow);
            addCircle(figure, x, y, 2, -1, bNext, bYellow);
            addCircle(figure, x, y, 2, 1, bNext, bYellow);
            addCircle(figure, x, y, -2, -1, bNext, bYellow);
            addCircle(figure, x, y, -2, 1, bNext, bYellow);
        }
        if (figure == ChessBallFigure.BLACK_PAWN) {
            addCircle(figure, x, y, 0, -1, false, bYellow);
            if (y - 1 >= 0 && bYellow && this.board[x][y-1] != ChessBallFigure.EMPTY) {
                this.circles[x][y-1] = ChessBallFigure.EMPTY;
            }

            if (bYellow) {
                addCircle(figure, x, y, -1, -1, false, bYellow);
                if (x - 1 >= 0 && y - 1 > 0 && board[x - 1][y - 1] == ChessBallFigure.EMPTY) {
                    this.circles[x - 1][y - 1] = ChessBallFigure.EMPTY;
                }
                addCircle(figure, x, y, 1, -1, false, bYellow);
                if (x + 1 < board.length && y - 1 > 0 && board[x + 1][y - 1] == ChessBallFigure.EMPTY) {
                    this.circles[x + 1][y - 1] = ChessBallFigure.EMPTY;
                }
            }
        }
        if (figure == ChessBallFigure.WHITE_PAWN) {
            addCircle(figure, x, y, 0, 1, false, bYellow);
            if (y + 1 < this.board[0].length && bYellow && this.board[x][y+1] != ChessBallFigure.EMPTY) {
                this.circles[x][y+1] = ChessBallFigure.EMPTY;
            }

            if (bYellow) {
                addCircle(figure, x, y, -1, 1, false, bYellow);
                if (x - 1 >= 0 && y + 1 < board[0].length - 1 && board[x - 1][y + 1] == ChessBallFigure.EMPTY) {
                    this.circles[x - 1][y + 1] = ChessBallFigure.EMPTY;
                }
                addCircle(figure, x, y, 1, 1, false, bYellow);
                if (x + 1 < board.length && y + 1 < board[0].length - 1 && board[x + 1][y + 1] == ChessBallFigure.EMPTY) {
                    this.circles[x + 1][y + 1] = ChessBallFigure.EMPTY;
                }
            }
        }
        if (figure == ChessBallFigure.BALL) {
            GridPoint2 p = new GridPoint2(x, y);
            fillBallMovement(p, -1, -1);
            fillBallMovement(p, 0, -1);
            fillBallMovement(p, 1, -1);
            fillBallMovement(p, -1, 0);
            fillBallMovement(p, 1, 0);
            fillBallMovement(p, -1, 1);
            fillBallMovement(p, 0, 1);
            fillBallMovement(p, 1, 1);
        }
    }

    private void fillBallMovement(GridPoint2 p, int addX, int addY) {
        boolean bWhite = this.currentColor == ChessBallColor.WHITE;
        ChessBallFigure neighbor = getNeighbor(p, addX, addY, bWhite);
        if (neighbor != ChessBallFigure.EMPTY) {
            setPossibleStepsForPosition(p.x, p.y, neighbor, false);
        }
    }

    private void addCircle(ChessBallFigure figure, int startX, int startY, int addX, int addY, boolean bYellow) {
        this.addCircle(figure, startX, startY, addX, addY, true, bYellow);
    }

    private void addCircle(ChessBallFigure figure, int startX, int startY, int addX, int addY, boolean bNext, boolean bYellow) {
        if (startX + addX < 0 || startX + addX >= this.board.length || startY + addY < 0 || startY + addY >= this.board[0].length) {
            return;
        }
        if ((figure != ChessBallFigure.BALL) && (figure != ChessBallFigure.BLACK_KING) && (figure != ChessBallFigure.WHITE_KING)) {
            if (startY + addY < 1 || startY + addY > this.board[0].length - 2) {
                boolean isGoPossible = this.canGo[startX + addX][startY + addY];
                if (isGoPossible && bYellow && (this.board[startX + addX][startY + addY] == ChessBallFigure.EMPTY)) {
                    return;
                } else if (isGoPossible && !bYellow && (this.board[startX + addX][startY + addY] != ChessBallFigure.EMPTY)) {
                    return;
                } else if (!isGoPossible) {
                    return;
                }
            }
        } else {
            if (!canGo[startX + addX][startY + addY]) {
                return;
            }
        }

        if ((this.board[startX + addX][startY + addY] == ChessBallFigure.EMPTY)
           || ((this.board[startX + addX][startY + addY].isWhite() != figure.isWhite())) && bYellow && (this.board[startX + addX][startY + addY] != ChessBallFigure.BALL)) {
            if (!bYellow) {
                this.circles[startX + addX][startY + addY] = ChessBallFigure.RED_CIRCLE;
            } else {
                this.circles[startX + addX][startY + addY] = ChessBallFigure.YELLOW_CIRCLE;
            }
            if ((bNext) && (this.board[startX + addX][startY + addY] == ChessBallFigure.EMPTY)) {
                addCircle(figure, startX + addX, startY + addY, addX, addY, bYellow);
            }
        }
    }

    public ChessBallStep getLastStep()             { return lastStep; }
    public ChessBallFigure getLastStepFigure()     { return lastStepFigure; }

    public void checkToSetFigure(int x, int y, GridPoint2 figurePosition) {
        this.lastStep = null;
        this.lastStepFigure = null;
        GridPoint2 currentPosition = getPointForPosition(x, y);
        if (currentPosition == null) {
            return;
        }
        if (this.circles[currentPosition.x][currentPosition.y] != ChessBallFigure.EMPTY) {
            ChessBallFigure figure = this.board[figurePosition.x][figurePosition.y];
            ChessBallFigure captured = this.board[currentPosition.x][currentPosition.y];
            recordAction(figure, captured);
            this.lastStep = new ChessBallStep(figurePosition.x, figurePosition.y, currentPosition.x, currentPosition.y);
            this.lastStepFigure = figure;
            this.board[currentPosition.x][currentPosition.y] = figure;
            this.board[figurePosition.x][figurePosition.y] = ChessBallFigure.EMPTY;
            if (figure == ChessBallFigure.BALL) {
                this.passesLeft -= 1;
            } else {
                this.playersMove -= 1;
            }
            if (figure == ChessBallFigure.WHITE_PAWN && currentPosition.y == this.board[0].length - 2) {
                this.board[currentPosition.x][currentPosition.y] = ChessBallFigure.WHITE_QUEEN;
            }
            if (figure == ChessBallFigure.BLACK_PAWN && currentPosition.y == 1) {
                this.board[currentPosition.x][currentPosition.y] = ChessBallFigure.BLACK_QUEEN;
            }

        }
    }

    public boolean hasNeighborsForBall(GridPoint2 p) {
        boolean bWhite = this.currentColor == ChessBallColor.WHITE;
        if (hasNeighbor(p, -1, -1, bWhite) ||
                hasNeighbor(p, 0, -1, bWhite) ||
                hasNeighbor(p, 1, -1, bWhite) ||
                hasNeighbor(p, -1, 0, bWhite) ||
                hasNeighbor(p, 1, 0, bWhite) ||
                hasNeighbor(p, -1, 1, bWhite) ||
                hasNeighbor(p, 0, 1, bWhite) ||
                hasNeighbor(p, 1, 1, bWhite)) {
            return true;
        }
        return false;
    }

    private boolean hasNeighbor(GridPoint2 p, int addX, int addY, boolean bWhite) {
        return getNeighbor(p, addX, addY, bWhite) != ChessBallFigure.EMPTY;
    }

    private ChessBallFigure getNeighbor(GridPoint2 p, int addX, int addY, boolean bWhite) {
        if (p.x + addX < 0 || p.x + addX >= this.board.length || p.y + addY < 0 || p.y + addY >= this.board[0].length) {
            return ChessBallFigure.EMPTY;
        }
        if (this.board[p.x + addX][p.y + addY] != ChessBallFigure.EMPTY && this.board[p.x + addX][p.y + addY].isWhite() == bWhite) {
            return this.board[p.x + addX][p.y + addY];
        }
        return ChessBallFigure.EMPTY;
    }

    public void renderBoard(MainPanel mainPanel) {
        for (int x = 0; x < this.board.length; x++) {
            for (int y = 0; y < this.board[0].length; y++) {
                if ((this.mouseOver.x >= 0) && this.mouseOver.x == x && this.mouseOver.y == y) {
                    mainPanel.spriteBatch.draw(AssetLoader.figures[13], ADD_X + x * 50, ADD_Y + y * 50);
                }
                if (circles[x][y] != ChessBallFigure.EMPTY) {
                    mainPanel.spriteBatch.draw(AssetLoader.figures[circles[x][y].getImageValue()], ADD_X + x * 50f, ADD_Y + y * 50f);
                }
                if (board[x][y] != ChessBallFigure.EMPTY) {
                    mainPanel.spriteBatch.draw(AssetLoader.figures[board[x][y].getImageValue()], ADD_X + (x + movement[x][y].x) * 50f, ADD_Y + (y + movement[x][y].y) * 50f);
                }
            }
        }
        if (Constants.SHOW_COORDS) {
            for (int x = 0; x < this.board.length; x++) {
                for (int y = 0; y < this.board[0].length; y++) {
                    mainPanel.drawString(x + "," + y,
                            ADD_X + x * 50 + 5, ADD_Y + y * 50 + 12,
                            Constants.COLOR_WHITE, AssetLoader.font15,
                            com.apogames.chessball.backend.DrawString.BEGIN);
                }
            }
        }
    }

    public void renderInformations(MainPanel mainPanel) {
        mainPanel.spriteBatch.draw(AssetLoader.color[this.currentColor.getImageValue()], 33, 35);

        mainPanel.spriteBatch.draw(AssetLoader.numbers[this.scoreWhite], 350, 35);
        mainPanel.spriteBatch.draw(AssetLoader.numbers[this.scoreBlack], 425, 35);
        mainPanel.spriteBatch.draw(AssetLoader.numbers[this.passesLeft], 112, 35);
    }
}
