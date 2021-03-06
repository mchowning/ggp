package org.ggp.base.player.gamer.statemachine.mygamers;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.List;

// FIXME This is buggy.  It does not treat a single state transition as requiring a move from both players.
// Look at MyFixedDepthHeuristicGamer

public class MyMiniMaxGamer extends SampleGamer {

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
            int moveScore = getMinScore(nextState);
            if (moveScore == 100) {
                return move;
            } else if (moveScore >= bestScore) {
                bestScore = moveScore;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private int getMinScore(MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else {
            int minScore = Integer.MAX_VALUE;
            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
                MachineState nextState = getNextStateFromMove(state, move);
                int score = getMaxScore(nextState);
                if (score <= minScore) {
                    minScore = score;
                }
            }
            return minScore;
        }
    }

    private int getMaxScore(MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else {
            int maxScore = Integer.MIN_VALUE;
            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
                MachineState nextState = getNextStateFromMove(state, move);
                int score = getMinScore(nextState);
                if (score >= maxScore) {
                    maxScore = score;
                }
            }
            return maxScore;
        }
    }

    private int getGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }

    private MachineState getNextStateFromMove(MachineState state, Move move) throws TransitionDefinitionException, MoveDefinitionException {
        return getStateMachine().getRandomNextState(state, getRole(), move);
    }
}
