package org.ggp.base.player.gamer.statemachine.mygamers;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;

public class MyMonteCarloSearchGamer extends SampleGamer {

    private static final int DEPTH_LIMIT = 3;
    private static final int NUM_PROBES = 4;

    private Random random = new Random();

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
            int moveScore = getMinScore(getCurrentState(), move, 0);
            if (moveScore == 100) {
                System.out.println("Winning move found");
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
            System.out.println("worstScore == bestScore, with score of: " + bestScore);
            return getRandomMove(moves);
        } else {
            System.out.println("bestScore of: " + bestScore);
            return bestMove;
        }
    }

    private Move getRandomMove(List<Move> moves) {
        return moves.get(random.nextInt(moves.size()));
    }

    private int getMaxScore(MachineState state, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else if (hasReachedDepthLimit(level)) {
            return monteCarloSearch(state);
        } else {
            int maxScore = Integer.MIN_VALUE;
            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
                int score = getMinScore(state, move, level);
                if (score >= maxScore) {
                    maxScore = score;
                }
            }
            return maxScore;
        }
    }

    /*
     * NOTE: Only works for 1-player or 2-player games
     */
    private int getMinScore(MachineState state, Move maximizedMove, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {

        List<Role> roles = getStateMachine().getRoles();
        switch (roles.size()) {
            case 1:
                // single player game
                MachineState nextState = getStateMachine().getNextState(state, Collections.singletonList(maximizedMove));
                return getMaxScore(nextState, level+1);
            case 2:
                // two player game
                return getMinScoreFor2PlayerOpponent(state, maximizedMove, level, roles);
//                return getAverageScoreForRandom2PlayerOpponent(state, maximizedMove, level, roles);

            default:
                throw new RuntimeException("Game must have 1 or 2 roles, but has " + roles.size());
        }
    }

    private int monteCarloSearch(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (NUM_PROBES == 0) {
            return 0;
            // FIXME this needs to return a neutral result, by returning a 0 it views this as equal to losing
        } else {
            int total = 0;
            for (int i = 0; i < NUM_PROBES; i++) {
                total += depthCharge(state);
            }
            return total / NUM_PROBES;
        }
    }

    private int depthCharge(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalScore(state);
        } else {
            List<Move> moveList = new ArrayList<>();
            List<Role> roles = getStateMachine().getRoles();
            for (Role role : roles) {
                List<Move> legalMovesForRole = getStateMachine().getLegalMoves(state, role);
                Move randomMove = getRandomMove(legalMovesForRole);
                moveList.add(randomMove);
            }
            MachineState nextState = getStateMachine().getNextState(state, moveList);
            return depthCharge(nextState);
        }
    }

    private int getAverageScoreForRandom2PlayerOpponent(MachineState state, Move maximizedMove, int level, List<Role> roles) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        MachineState nextState;
        Role opponent = getOpponentIn2PlayerGame(roles);

        int sumOfScores = 0;
        List<Move> opponentLegalMoves = getStateMachine().getLegalMoves(state, opponent);
        for (Move opponentMove : opponentLegalMoves) {
            List<Move> moveList = generateMoveListFor2Players(roles, maximizedMove, opponentMove);
            nextState = getStateMachine().getNextState(state, moveList);
            int score = getMaxScore(nextState, level + 1);
            sumOfScores += score;
        }
        return sumOfScores / opponentLegalMoves.size();
    }

    private int getMinScoreFor2PlayerOpponent(MachineState state, Move maximizedMove, int level, List<Role> roles) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        MachineState nextState;
        Role opponent = getOpponentIn2PlayerGame(roles);

        int minScore = Integer.MAX_VALUE;
        for (Move opponentMove : getStateMachine().getLegalMoves(state, opponent)) {
            List<Move> moveList = generateMoveListFor2Players(roles, maximizedMove, opponentMove);
            nextState = getStateMachine().getNextState(state, moveList);
            int score = getMaxScore(nextState, level + 1);
            if (score < minScore) {
                minScore = score;
            }
        }
        return minScore;
    }

    private List<Move> generateMoveListFor2Players(List<Role> roles, Move gamerMove, Move opponentMove) {
        Role gamerRole = getRole();
        if (roles.get(0).equals(gamerRole)) {
            return Arrays.asList(gamerMove, opponentMove);
        } else {
            return Arrays.asList(opponentMove, gamerMove);
        }
    }

    private Role getOpponentIn2PlayerGame(List<Role> roles) {
        assert(roles.size() == 2);
        Role gamerRole = getRole();
        return roles.get(0).equals(gamerRole) ? roles.get(1) : roles.get(0);
    }

    private int getGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }

    private boolean hasReachedDepthLimit(int level) {
        return level >= DEPTH_LIMIT;
    }
}
