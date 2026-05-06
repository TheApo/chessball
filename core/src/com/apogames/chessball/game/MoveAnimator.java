package com.apogames.chessball.game;

import com.apogames.chessball.Constants;
import com.apogames.chessball.ai.ChessBallStep;
import com.apogames.chessball.game.enums.ChessBallFigure;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Step-by-step animator for a sequence of {@link ChessBallStep}s. Used by
 * {@link com.apogames.chessball.game.game.ChessBallGame} (AI / animated turns)
 * and by {@link com.apogames.chessball.game.menu.ChessBallMenu} (demo playback).
 *
 * Each tick either decrements the inter-step wait timer or advances the current
 * step's movement vector by 1/35 toward the destination. When a step finishes,
 * the figure is committed to the board and {@code recordAction} is called so
 * per-match stats stay accurate. Higher-level concerns (demo-string recording,
 * win detection, turn flipping) are left to the caller and triggered via
 * {@link Phase#STEP_DONE} + {@link #isLastStepFinal()}.
 */
public class MoveAnimator {

    public enum Phase {
        /** Nothing loaded; tick is a no-op. */
        IDLE,
        /** Showing the next step's possible-move circles before motion starts. */
        WAITING,
        /** Current step's figure is mid-motion. */
        ANIMATING,
        /** A step just committed this tick; check {@link #isLastStepFinal()} for end-of-sequence. */
        STEP_DONE
    }

    private static final float STEP_INCREMENT = 1f / 35f;

    private final ChessBallBoard board;
    private final List<ChessBallStep> steps = new ArrayList<>();

    private float waitTime;
    private ChessBallStep lastFinishedStep;
    private ChessBallFigure lastFinishedFigure;
    private ChessBallFigure lastFinishedCaptured;
    private boolean lastStepFinal;

    public MoveAnimator(ChessBallBoard board) {
        this.board = board;
    }

    /** True while a step is queued, animating, or waiting to start. */
    public boolean isActive() {
        return !steps.isEmpty() || waitTime > 0;
    }

    public ChessBallStep getLastFinishedStep() {
        return lastFinishedStep;
    }

    public ChessBallFigure getLastFinishedFigure() {
        return lastFinishedFigure;
    }

    public ChessBallFigure getLastFinishedCaptured() {
        return lastFinishedCaptured;
    }

    /** True when the most recent {@link Phase#STEP_DONE} was the last step in the sequence. */
    public boolean isLastStepFinal() {
        return lastStepFinal;
    }

    /** Reset all animator state. Does not modify the board. */
    public void clear() {
        steps.clear();
        waitTime = 0;
        lastFinishedStep = null;
        lastFinishedFigure = null;
        lastFinishedCaptured = null;
        lastStepFinal = false;
    }

    /** Begin animating the given sequence. The first step's possible-move circles are
     *  drawn immediately so the player sees what's about to happen during the wait. */
    public void start(List<ChessBallStep> newSteps) {
        clear();
        if (newSteps == null || newSteps.isEmpty()) {
            return;
        }
        steps.addAll(newSteps);
        waitTime = Constants.WAIT_UNTIL_MOVE_IN_MILLISECONDS;
        ChessBallStep first = steps.get(0);
        board.setPossibleStepsForPosition(first.getFigureX(), first.getFigureY());
    }

    public Phase tick(float delta) {
        // STEP_DONE is a one-tick signal — clear so callers don't reread stale data.
        lastFinishedStep = null;
        lastFinishedFigure = null;
        lastFinishedCaptured = null;
        lastStepFinal = false;

        if (steps.isEmpty()) {
            return Phase.IDLE;
        }
        if (waitTime > 0) {
            waitTime -= delta;
            return Phase.WAITING;
        }

        ChessBallStep step = steps.get(0);
        Vector2 vector = board.getMovement()[step.getFigureX()][step.getFigureY()];

        int dx = step.getStepFigureX() - step.getFigureX();
        int dy = step.getStepFigureY() - step.getFigureY();
        if (dx < 0)      vector.x -= STEP_INCREMENT;
        else if (dx > 0) vector.x += STEP_INCREMENT;
        if (dy < 0)      vector.y -= STEP_INCREMENT;
        else if (dy > 0) vector.y += STEP_INCREMENT;

        // Clamp once we've passed the target along an axis.
        if ((dx < 0 && dx >= vector.x) || (dx > 0 && dx <= vector.x)) {
            vector.x = dx;
        }
        if ((dy < 0 && dy >= vector.y) || (dy > 0 && dy <= vector.y)) {
            vector.y = dy;
        }

        if (vector.x != dx || vector.y != dy) {
            return Phase.ANIMATING;
        }

        // Step complete — commit on the board.
        ChessBallFigure mover = board.getBoard()[step.getFigureX()][step.getFigureY()];
        ChessBallFigure captured = board.getBoard()[step.getStepFigureX()][step.getStepFigureY()];
        board.recordAction(mover, captured);
        board.getBoard()[step.getFigureX()][step.getFigureY()] = ChessBallFigure.EMPTY;
        board.getBoard()[step.getStepFigureX()][step.getStepFigureY()] = mover;
        // Reset movement offsets at BOTH endpoints. Source's stale offset would
        // mis-render any later piece that lands there; a later step with dx=0 (or
        // dy=0) could never terminate because the termination check is "vector ==
        // delta" — a stale vector.x=1 vs delta=0 would deadlock the animation.
        board.getMovement()[step.getFigureX()][step.getFigureY()].set(0f, 0f);
        board.getMovement()[step.getStepFigureX()][step.getStepFigureY()].set(0f, 0f);
        board.deleteCircle();

        steps.remove(0);
        lastFinishedStep = step;
        lastFinishedFigure = mover;
        lastFinishedCaptured = captured;

        if (steps.isEmpty()) {
            lastStepFinal = true;
        } else {
            ChessBallStep next = steps.get(0);
            board.setPossibleStepsForPosition(next.getFigureX(), next.getFigureY());
            waitTime = Constants.WAIT_UNTIL_MOVE_IN_MILLISECONDS;
        }
        return Phase.STEP_DONE;
    }
}
