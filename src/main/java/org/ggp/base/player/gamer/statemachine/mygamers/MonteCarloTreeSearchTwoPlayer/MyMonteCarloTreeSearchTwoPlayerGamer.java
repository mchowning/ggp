package org.ggp.base.player.gamer.statemachine.mygamers.MonteCarloTreeSearchTwoPlayer;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class MyMonteCarloTreeSearchTwoPlayerGamer extends SampleGamer {

    private static final long TIME_CUSHION_MILLIS = TimeUnit.SECONDS.toMillis(2);
    private static final int INDEX_OF_GAMER_ROLE = 0;

    private Random random = new Random();

    private Node rootNode;

    // TODO make sure this can handle if gamer is the second gamer

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

        long startTime = System.currentTimeMillis();

        long allowedSolveTimeWithCushion = timeout - TIME_CUSHION_MILLIS;
        Move bestMove = getBestMove(allowedSolveTimeWithCushion);

        long stop = System.currentTimeMillis();
        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        notifyObservers(new GamerSelectedMoveEvent(moves, bestMove, stop - startTime));

        return bestMove;
    }

    private Move getBestMove(long allowedSolveTime) throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {

        updateRootNode();

        while (System.currentTimeMillis() < allowedSolveTime) {
            performMonteCarloTreeSearch(rootNode);
        }

        Node childNodeForBestMove = getChildNodeForBestMove();
         System.out.println("number of visits to rootNode: " + rootNode.getNumVisits());
         System.out.println("best move gamer score: " + childNodeForBestMove.getGamerUtility() + "\n");
        return childNodeForBestMove.getMovesFromParent().get(INDEX_OF_GAMER_ROLE);
    }

    private void updateRootNode() {
        if (rootNode == null) {
            rootNode = new Node(getCurrentState(), null, null, getStateMachine(), getRole());
        } else {
            Node newRootNode = null;
            for (Node child : rootNode.getChildren()) {
                boolean childMatchesCurrentState = Helper.statesMatch(getCurrentState(), child.getState());
                if (childMatchesCurrentState) {
                    newRootNode = child;
                }
            }
            if (newRootNode == null) throw new RuntimeException("failed to find new root node");
            rootNode = newRootNode;
            rootNode.dropParent();
        }
    }

    private void performMonteCarloTreeSearch(Node node) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
        Node selectedNode = selectNode(node);
        expandNode(selectedNode);
        GoalState randomTerminalGoalState = getRandomTerminalValue(selectedNode);
        backpropogate(selectedNode, randomTerminalGoalState);
    }

    private Node getChildNodeForBestMove() {
        Node bestChildNode = null;
        for (Node node : rootNode.getChildren()) {
             System.out.println("gamer: " + node.getGamerUtility() + ", opp: " + node.getOpponentUtility() + " for move: " + node.getMovesFromParent() + ", with " + node.getNumVisits() + " visits");
            if (bestChildNode == null || node.getGamerUtility() > bestChildNode.getGamerUtility()) {
                bestChildNode = node;
            }
        }
        return bestChildNode;
    }

    private Node selectNode(Node startNode) throws MoveDefinitionException, TransitionDefinitionException {
        int minNumVisits = 10; // This is useful to insure decent sampling when the game is entirely just win/lose
        if (startNode.getNumVisits() < minNumVisits || getStateMachine().isTerminal(startNode.getState())) {
            return startNode;
        } else {
            Node childWithHighestSelectValue = null;
            double bestSelectValue = Double.MIN_VALUE;

            for (Node childNode : startNode.getChildren()) {
                if (childNode.getNumVisits() < minNumVisits) {
                    return childNode;
                }
                double childSelectValue = selectFn(childNode);
                if (childSelectValue > bestSelectValue) {
                    bestSelectValue = childSelectValue;
                    childWithHighestSelectValue = childNode;
                }
            }

            return selectNode(childWithHighestSelectValue);
        }
    }

    private double selectFn(Node node) {
        Node parentNode = node.getParent();
        int nodeVisits = node.getNumVisits();
        int parentNodeVisits = parentNode == null ? 0 : parentNode.getNumVisits();

        double nodeUtility = node.getUtilityForRole(parentNode.isGamerNode()); // because each node type is being picked by the opposite node type
        final int gameAppropriateConstant = 50; // TODO handle this constant better for different max game values
        final double explorationUtility = gameAppropriateConstant * Math.sqrt(Math.log(parentNodeVisits) / nodeVisits);
        return nodeUtility + explorationUtility;
    }

    private void expandNode(Node node) throws MoveDefinitionException, TransitionDefinitionException {

        if (getStateMachine().isTerminal(node.getState())) return;

        if (node.getChildren().isEmpty()) {
            for (Move gamerMove : node.getAvailableGamerMoves()) {
                for (Move opponentMove : node.getAvailableOpponentMoves()) {
                    List<Move> transitionMoveList = getMoveListFor2Players(gamerMove, opponentMove);
                    MachineState childState = getStateMachine().getNextState(node.getState(), transitionMoveList);
                    node.createChildNode(childState, transitionMoveList);
                }
            }
        }
    }

    /* Only works when gamer is first to move */
    private List<Move> getMoveListFor2Players(Move gamerMove, Move opponentMove) {
        if (gamerMove.equals(opponentMove)) {
            System.out.println("gamer and opponent move should not be the same");
        }
        List<Move> result = new ArrayList<>();
        result.add(gamerMove);
        result.add(opponentMove);
        return result;
    }

    private void backpropogate(Node node, GoalState score) {
        if (node != null) {
            node.addVisit();
            node.updateUtilityWithGoalState(score);
            backpropogate(node.getParent(), score);
        }
    }

    private GoalState getRandomTerminalValue(Node startNode) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        return depthCharge(startNode.getState());
    }

    private GoalState depthCharge(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(state)) {
            return getGoalState(state);
        } else {
            List<Move> transitionMoveList = new ArrayList<>();
            List<Role> roles = getStateMachine().getRoles();
            for (Role role : roles) {
                List<Move> legalMovesForRole = getStateMachine().getLegalMoves(state, role);
                Move selectedMove = getRandomMove(legalMovesForRole);
                transitionMoveList.add(selectedMove);
            }
            MachineState nextState = getStateMachine().getNextState(state, transitionMoveList);
            return depthCharge(nextState);
        }
    }

    private Move getRandomMove(List<Move> moves) {
        return moves.get(random.nextInt(moves.size()));
    }

    private GoalState getGoalState(MachineState state) throws GoalDefinitionException {
        int gamerValue = getGamerGoalScore(state);
        int opponentValue = getOpponentGoalScore(state);
        return new GoalState(gamerValue, opponentValue);
    }

    private int getGamerGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }

    /* Only works when 2-players */
    private int getOpponentGoalScore(MachineState state) throws GoalDefinitionException {
        List<Role> roles = getStateMachine().getRoles();
        Role opponent;
        final Role gamer = getRole();
        if (roles.get(0).equals(gamer)) {
            opponent = roles.get(1);
        } else {
            opponent = roles.get(0);
        }

        return getStateMachine().getGoal(state, opponent);
    }
}
