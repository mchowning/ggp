package org.ggp.base.player.gamer.statemachine.mygamers;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;

public class MyMonteCarloTreeSearchSinglePlayerGamer extends SampleGamer {

    private class Node {

        private MachineState state;
        private Node parent;
        private Move moveFromParent;
        private List<Node> children = new ArrayList<>();
        private int numVisits = 0;
        private int utility = 0;

        public Node(MachineState state, Node parent, Move moveFromParent) {
            this.state = state;
            this.parent = parent;
            this.moveFromParent = moveFromParent;
        }

        public MachineState getState() {
            return state;
        }

        public Node getParent() {
            return parent;
        }

        public Node createChildNode(MachineState state, Move move) {
            Node childNode = new Node(state, this, move);
            children.add(childNode);
            return childNode;
        }

        public List<Node> getChildren() {
            return children;
        }

        public void addVisit() {
            numVisits++;
        }

        public int getNumVisits() {
            return numVisits;
        }

        public int getTotalUtility() {
            return utility;
        }

        public int getEffectiveUtility() {
            if (numVisits == 0) {
                return Integer.MIN_VALUE;
            } else {
                return utility / numVisits;
            }
        }

        public void setUtility(int utility) {
            this.utility = utility;
        }

        public Move getMoveFromParent() {
            return moveFromParent;
        }
    }

    private static final int NUM_PROBES = 50;
    private Random random = new Random();

    private Map<MachineState, Node> nodeMap = new HashMap<>();

    @Override
    public void stateMachineStop() {
        super.stateMachineStop();
        nodeMap.clear();
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

        long start = System.currentTimeMillis();

        List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
        Move bestMove = getBestMove();

        long stop = System.currentTimeMillis();
        notifyObservers(new GamerSelectedMoveEvent(moves, bestMove, stop - start));

        return bestMove;
    }

    private Move getBestMove() throws TransitionDefinitionException, GoalDefinitionException, MoveDefinitionException {

        Node startNode;
        if (nodeMap.containsKey(getCurrentState())) {
            startNode = nodeMap.get(getCurrentState());
        } else {
            startNode = new Node(getCurrentState(), null, null);
        }


        Node selectedUnexpandedNode = selectNode(startNode);
        expandNode(selectedUnexpandedNode);
        for (int i = 0; i < NUM_PROBES; i++) {
            int terminalNodeValue = getValueOfRandomTerminalNode(selectedUnexpandedNode);
            backpropogate(selectedUnexpandedNode, terminalNodeValue);
        }

        Node bestChildNode = null;
        for (Node node : startNode.getChildren()) {
            if (bestChildNode == null || node.getEffectiveUtility() > bestChildNode.getEffectiveUtility()) {
                bestChildNode = node;
            }
        }
        System.out.println(bestChildNode.getEffectiveUtility());
        return bestChildNode.getMoveFromParent();

    }

    private Node selectNode(Node startNode) throws MoveDefinitionException, TransitionDefinitionException {
        if (startNode.getNumVisits() == 0 || startNode.getChildren().isEmpty()) {
            return startNode;
        } else {
            List<Node> children = startNode.getChildren();
            for (Node childNode: children) {
                if (childNode.getNumVisits() == 0) {
                    return childNode;
                }
            }

            int bestSelectValue = Integer.MIN_VALUE;
            Node resultingNode = null;
            for (Node childNode : children) {
                int childSelectValue = selectFn(childNode);
                if (childSelectValue > bestSelectValue) {
                    bestSelectValue = childSelectValue;
                    resultingNode = childNode;
                }
            }
            return selectNode(resultingNode);
        }
    }

    private int selectFn(Node node) {
        Node parentNode = node.getParent();
        int nodeTotalUtility = node.getTotalUtility(); // FIXME this should be the average utility
        int nodeVisits = node.getNumVisits();
        int parentNodeVisits = parentNode == null ? 0 : parentNode.getNumVisits();
        return (int) (nodeTotalUtility + Math.sqrt( 2 * Math.log(parentNodeVisits) / nodeVisits));
    }

    private void expandNode(Node node) throws MoveDefinitionException, TransitionDefinitionException {
        MachineState nodeState = node.getState();
        List<Move> moveList = getStateMachine().getLegalMoves(nodeState, getRole());
        for (Move move : moveList) {
            MachineState childState = getStateMachine().getNextState(nodeState, Collections.singletonList(move));
            Node newNode = node.createChildNode(childState, move);
            nodeMap.put(newNode.getState(), newNode);
        }
    }

    private void backpropogate(Node node, int score) {
        node.addVisit();
        int prevUtility = node.getTotalUtility();
        int newUtility = prevUtility + score;
        node.setUtility(newUtility);

        Node nodeParent = node.getParent();
        if (nodeParent != null) {
            backpropogate(nodeParent, score);
        }
    }

    private int getValueOfRandomTerminalNode(Node startNode) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        return depthCharge(startNode);
    }

    private int depthCharge(Node node) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
        if (getStateMachine().isTerminal(node.getState())) {
            int score = getGoalScore(node.getState());
            backpropogate(node, score);
            return score;
        } else {

            if (node.getChildren().isEmpty()) {
                expandNode(node);
            }

            Node randomChildNode = getRandomChildNode(node);
            return depthCharge(randomChildNode);
        }
    }

    private Node getRandomChildNode(Node node) {
        List<Node> childNodes = node.getChildren();
        return childNodes.get(random.nextInt(childNodes.size()));
    }

    private int getGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }
}
