package org.ggp.base.player.gamer.statemachine.mygamers;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.List;
import java.util.Random;

public class MyBoundedDepthGamer extends SampleGamer {

    private static final int DEPTH_LIMIT = 3;

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
        int worstScore = Integer.MAX_VALUE;
        for (Move move : moves) {
            MachineState nextState = getNextStateFromMove(getCurrentState(), move);
            int moveScore = getMinScore(nextState, 0);
            if (moveScore == 100) {
                return move;
            } else if (moveScore >= bestScore) {
                bestScore = moveScore;
                bestMove = move;
            }
            if (moveScore <= worstScore) {
                worstScore = moveScore;
            }
        }
        if (worstScore == bestScore) {
            return getRandomMove(moves);
        } else {
            return bestMove;
        }
    }

    private Move getRandomMove(List<Move> moves) {
        return moves.get(new Random().nextInt(moves.size()));
    }

    private int getMinScore(MachineState state, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else {
            int minScore = Integer.MAX_VALUE;
            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
                MachineState nextState = getNextStateFromMove(state, move);
                int score = getMaxScore(nextState, level+1);
                if (score <= minScore) {
                    minScore = score;
                }
            }
            return minScore;
        }
    }

    private int getMaxScore(MachineState state, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else if (levelHitLimt(level)) {
            return getGoalScore(state);
        } else {
            int maxScore = Integer.MIN_VALUE;
            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
                MachineState nextState = getNextStateFromMove(state, move);
                int score = getMinScore(nextState, level);
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
//        return getStateMachine().getNextState(state, Collections.singletonList(move));

        // TODO add no op move(s) anytime there is more than 1 role?
    }

    private boolean levelHitLimt(int level) {
        return level >= DEPTH_LIMIT;
    }
}
