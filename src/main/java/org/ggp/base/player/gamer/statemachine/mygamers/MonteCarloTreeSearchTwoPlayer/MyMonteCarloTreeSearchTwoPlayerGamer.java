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

public class MyMonteCarloTreeSearchTwoPlayerGamer extends SampleGamer {

    private static final int NUM_PROBES = 100;
    private Random random = new Random();
    private long startTime;

//    private Map<NodeKey, Node> nodeMap = new HashMap<>();
//
//    @Override
//    public void stateMachineStop() {
//        super.stateMachineStop();
//        nodeMap.clear();
//    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

        startTime = System.currentTimeMillis();

        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        Move bestMove = getBestMove();

        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(moves, bestMove, stop - startTime));

        return bestMove;
    }

    private Move getBestMove() throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {

//        NodeKey keyForStateWithNoGamerMove = NodeKey.forState(getCurrentState());
//        if (!nodeMap.containsKey(keyForStateWithNoGamerMove)) {
//            Node newNode = new Node(getCurrentState(), null, null, null);
//            nodeMap.put(keyForStateWithNoGamerMove, newNode);
//        }
//        startNode = nodeMap.get(keyForStateWithNoGamerMove);

        Node startNode = new Node(getCurrentState(), null, null, null);
        startNode.fillAvailableMoves(getStateMachine(), getRole());

        for (int i = 0; i < NUM_PROBES; i++) {
//        while (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) < 10) {
//            System.out.println("probe #: " + i);
            Node selectedNode = selectNode(startNode);
            expandNode(selectedNode);
           int randomTerminalValue = getRandomTerminalValue(startNode);
            backpropogate(selectedNode, randomTerminalValue);
        }

        Node bestChildNode = null;
        for (Node node : startNode.getChildren()) {
            if (bestChildNode == null || (node.hasInitializedUtility() && node.getUtility() > bestChildNode.getUtility())) {
                bestChildNode = node;
            }
        }
        System.out.println("best move score: " + bestChildNode.getUtility());
        return bestChildNode.getGamerMove();
    }

    private Node selectNode(Node startNode) throws MoveDefinitionException, TransitionDefinitionException {
        if (startNode.getNumVisits() == 0 || getStateMachine().isTerminal(startNode.getState())) {
            return startNode;
        } else {
            Node childWithHighestSelectValue = null;
            int bestSelectValue = Integer.MIN_VALUE;

            for (Node childNode : startNode.getChildren()) {
                if (childNode.getNumVisits() == 0) {
                    return childNode;
                }
                int childSelectValue = selectFn(childNode);
                if (childSelectValue > bestSelectValue) {
                    bestSelectValue = childSelectValue;
                    childWithHighestSelectValue = childNode;
                }
            }

            return selectNode(childWithHighestSelectValue);
        }
    }

    private int selectFn(Node childNode) {
        Node parentNode = childNode.getParent();
        int nodeUtility = childNode.getUtility();
        int nodeVisits = childNode.getNumVisits();
        int parentNodeVisits = parentNode == null ? 0 : parentNode.getNumVisits();

        if (childNode.isGamerMoveNode()) {             // Because max nodes are selected by min nodes
            nodeUtility = 0 - nodeUtility;
        }

        return (int) (nodeUtility + Math.sqrt( 2 * Math.log(parentNodeVisits) / nodeVisits));
    }

    private void expandNode(Node node) throws MoveDefinitionException, TransitionDefinitionException {

        if (getStateMachine().isTerminal(node.getState())) {
            System.out.println("Improperly tried to expand terminal node");
            return;
        }

        if (node.getChildren().isEmpty()) {

            if (node.isGamerMoveNode()) {
                for (Move gamerMove : node.getAvailableGamerMoves()) {
                    Node newNode = node.createChildNode(node.getState(), gamerMove, node.getMovesFromParent());
                    newNode.fillAvailableMoves(getStateMachine(), getRole());

//                    addNodeToMap(newNode);
                }
            } else {
                Move gamerMove = node.getGamerMove();
                MachineState nodeState = node.getState();
                for (Move opponentMove : node.getAvailableOpponentMoves()) {
                    List<Move> transitionMoveList = getMoveListFor2Players(gamerMove, opponentMove);
                    MachineState childState = getStateMachine().getNextState(nodeState, transitionMoveList);
                    Node newNode = node.createChildNode(childState, null, transitionMoveList);
                    newNode.fillAvailableMoves(getStateMachine(), getRole());

//                    addNodeToMap(newNode);
                }
            }
        }
    }

//    private void addNodeToMap(Node newNode) {
//        NodeKey nodeKey = NodeKey.forNode(newNode);
//        nodeMap.put(nodeKey, newNode);
//    }

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

    /* Applies score to current node if appropriate and then backpropogates in light of this, possibly update score */
    private void backpropogate(Node node, int score) {
        if (node == null) throw new RuntimeException("can't backpropogate null node");
        node.addVisit();
        node.updateUtilityIfAppropriate(score);
        // TODO make this so it only backpropogates if the score is updated
        backpropogate(node.getParent());
    }

    private void backpropogate(Node node) {

        if (node == null) return;

        node.addVisit();

        int newUtility;
        if (node.isGamerMoveNode()) {
            newUtility = getUtilityOfMaxChild(node);
        } else {
            newUtility = getUtilityOfMinChild(node);
        }
        node.updateUtilityIfAppropriate(newUtility);

        // TODO make this so it only backpropogates if the score is updated
        backpropogate(node.getParent());
    }

    // TODO try using lambda expressions here?
    private int getUtilityOfMaxChild(Node node) {
        int maxUtility = Integer.MIN_VALUE;
        for (Node childNode : node.getChildren()) {
            if (childNode.hasInitializedUtility()) {
                int childUtility = childNode.getUtility();
                if (childUtility > maxUtility) {
                    maxUtility = childUtility;
                }
            }
        }
        return maxUtility;
    }

    // TODO try using lambda expressions here?
    private int getUtilityOfMinChild(Node node) {
        int minUtility = Integer.MAX_VALUE;
        for (Node childNode : node.getChildren()) {
            if (childNode.hasInitializedUtility()) {
                int childUtility = childNode.getUtility();
                if (childUtility < minUtility) {
                    minUtility = childUtility;
                }
            }
        }
        return minUtility;
    }

    private int getRandomTerminalValue(Node startNode) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        return depthCharge(startNode.getState());
    }

    private int depthCharge(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(state)) {
//            System.out.println("got terminal score of: " + getGoalScore(state));
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

    private Move getRandomMove(List<Move> moves) {
        return moves.get(random.nextInt(moves.size()));
    }

    private int getGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }
}
