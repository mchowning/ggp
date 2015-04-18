package org.ggp.base.player.gamer.statemachine.mygamers;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MyAlphaBetaGamer extends SampleGamer {

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

        long start = System.currentTimeMillis();

        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        Move bestMove = getBestMove(moves);

        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(moves, bestMove, stop - start));

        return bestMove;
    }

    private Move getBestMove(List<Move> moves) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        for (Move move : moves) {
            MachineState nextState = getNextStateFromMove(getCurrentState(), move);
            int moveScore = getMinScore(nextState, Integer.MIN_VALUE, Integer.MAX_VALUE);
            if (moveScore == 100) {
                return move;
            } else if (moveScore >= bestScore) {
                bestScore = moveScore;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int getMinScore(MachineState state, int alpha, int beta) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else {
            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
                MachineState nextState = getNextStateFromMove(state, move);
                int score = getMaxScore(nextState, alpha, beta);
                beta = Collections.min(Arrays.asList(score, beta));
                if (beta <= alpha) {
                    return alpha;
                }
            }
            return beta;
        }
    }

    private int getMaxScore(MachineState state, int alpha, int beta) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else {
            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
                MachineState nextState = getNextStateFromMove(state, move);
                int score = getMinScore(nextState, alpha, beta);
                alpha = Collections.max(Arrays.asList(score, alpha));
                if (alpha >= beta) {
                    return beta;
                }
            }
            return alpha;
        }
    }

    private int getGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }

    private MachineState getNextStateFromMove(MachineState state, Move move) throws TransitionDefinitionException, MoveDefinitionException {
        return getStateMachine().getRandomNextState(state, getRole(), move);
    }
}
