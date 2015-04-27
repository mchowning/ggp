package org.ggp.base.player.gamer.statemachine.mygamers;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MyMonteCarloTreeSearchTwoPlayerGamer extends SampleGamer {

    /*
     * NodeKey
     */

    private static class NodeKey {

        private MachineState state;
        private Move gamerMove;

        public static NodeKey forNode(Node node) {
            return new NodeKey(node.getState(), node.getGamerMove());
        }

        public static NodeKey forState(MachineState state) {
            return new NodeKey(state, null);
        }

        private NodeKey(MachineState state, Move gamerMove) {
            this.state = state;
            this.gamerMove = gamerMove;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NodeKey nodeKey = (NodeKey) o;

            if (state != null ? !state.equals(nodeKey.state) : nodeKey.state != null) return false;
            return !(gamerMove != null ? !gamerMove.equals(nodeKey.gamerMove) : nodeKey.gamerMove != null);
        }

        @Override
        public int hashCode() {
            int result = state != null ? state.hashCode() : 0;
            result = 31 * result + (gamerMove != null ? gamerMove.hashCode() : 0);
            return result;
        }
    }

    /*
     * Node
     */

    private class Node {

        private static final int UNINITIALIZED_UTILITY = Integer.MIN_VALUE + 1;

        private MachineState state;
        private Move gamerMove;
        private Node parent;
        private List<Move> movesFromParent;

        // TESTING
        private List<Move> availableGamerMoves;
        private List<Move> availableOpponentMoves;

        private List<Node> children = new ArrayList<>();
        private int numVisits = 0;
        private Integer utility = UNINITIALIZED_UTILITY;


        public Node(MachineState state, Move gamerMove, Node parent, List<Move> movesFromParent) throws MoveDefinitionException {
            this.state = state;
            this.gamerMove = gamerMove;
            this.parent = parent;
            this.movesFromParent = movesFromParent;
            fillAvailableMoves();
        }

        // TESTING
        private void fillAvailableMoves() throws MoveDefinitionException {
            for (Role role : getStateMachine().getRoles()) {
                List<Move> availableMovesForRole = getStateMachine().getLegalMoves(getState(), role);
                if (role.equals(getRole())) {
                    availableGamerMoves = availableMovesForRole;
                } else {
                    availableOpponentMoves = availableMovesForRole;
                }
            }
        }

        public MachineState getState() {
            return state;
        }

        public boolean isMaxNode() {
            return gamerMove == null;
        }

        public boolean isMinNode() {
            return !isMaxNode();
        }

        public Move getGamerMove() {
            return gamerMove;
        }

        public Node getParent() {
            return parent;
        }

        public void addChildNode(Node childNode) {
            children.add(childNode);
        }

        public Node createChildNode(MachineState state, Move gamerMove, List<Move> moveFromParent) throws MoveDefinitionException {
            Node childNode = new Node(state, gamerMove, this, moveFromParent);
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

        public boolean hasInitializedUtility() {
            return utility != UNINITIALIZED_UTILITY;
        }

        public int getUtility() {
            if (!hasInitializedUtility()) throw new RuntimeException("should not be getting uninitialized utility");
            return utility;
        }

        public void setUtility(int utility) {
            this.utility = utility;
        }

        public void updateUtilityIfAppropriate(int possibleNewUtilty) {
            if (utility == UNINITIALIZED_UTILITY ||
               ( isMaxNode() && possibleNewUtilty > utility ) ||
               ( isMinNode() && possibleNewUtilty < utility ) )
            {
                utility = possibleNewUtilty;
            }
        }

        public void setMovesFromParent(List<Move> moveList) {
            movesFromParent = moveList;
        }

        public List<Move> getMovesFromParent() {
            return movesFromParent;
        }
    }

    /*
     * Class methods
     */

    private static final int NUM_PROBES = 50;
    private Random random = new Random();

//    private Map<NodeKey, Node> nodeMap = new HashMap<>();
//
//    @Override
//    public void stateMachineStop() {
//        super.stateMachineStop();
//        nodeMap.clear();
//    }

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

//        NodeKey keyForStateWithNoGamerMove = NodeKey.forState(getCurrentState());
//        if (!nodeMap.containsKey(keyForStateWithNoGamerMove)) {
//            Node newNode = new Node(getCurrentState(), null, null, null);
//            nodeMap.put(keyForStateWithNoGamerMove, newNode);
//        }
//        startNode = nodeMap.get(keyForStateWithNoGamerMove);

        Node startNode = new Node(getCurrentState(), null, null, null);

        for (int i = 0; i < NUM_PROBES; i++) {
            System.out.println("probe #: " + i);
            Node selectedNode = selectNode(startNode);
            expandNode(selectedNode);
           int randomTerminalValue = getRandomTerminalValue(startNode);
            backpropogate(selectedNode, randomTerminalValue);
//            int terminalNodeValue = getRandomTerminalValue(selectedNode);
//            updateNodeValue(selectedNode, terminalNodeValue);
//            backpropogate(selectedNode, terminalNodeValue);
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

//    private void updateNodeValue(Node node, int score) {
//        int nodePrevValue = node.getUtility();
//        if (node.isMaxNode() && score > nodePrevValue) {
//            node.setUtility(score);
//        } else if (score < nodePrevValue) {
//            node.setUtility(score);
//        }
//    }

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

    private int selectFn(Node node) {
        Node parentNode = node.getParent();
        int nodeUtility = node.getUtility();
        int nodeVisits = node.getNumVisits();
        int parentNodeVisits = parentNode == null ? 0 : parentNode.getNumVisits();

        if (node.isMinNode()) {
            nodeUtility = 0 - nodeUtility;
        }

        return (int) (nodeUtility + Math.sqrt( 2 * Math.log(parentNodeVisits) / nodeVisits));
    }

    private void expandNode(Node node) throws MoveDefinitionException, TransitionDefinitionException {

        if (getStateMachine().isTerminal(node.getState())) throw new RuntimeException("Improperly tried to expand terminal node");

        if (node.getChildren().isEmpty()) {

            if (node.isMaxNode()) {
                Role gamerRole = getRole();
                List<Move> gamerMoves = getStateMachine().getLegalMoves(node.getState(), gamerRole);
                for (Move gamerMove : gamerMoves) {
//                    Node newNode = node.createChildNode(node.getState(), gamerMove, node.getMovesFromParent());
                    node.createChildNode(node.getState(), gamerMove, node.getMovesFromParent());

//                    addNodeToMap(newNode);
                }
            } else {
                Role opponentRole = getOpponentRole();
                Move gamerMove = node.getGamerMove();
                MachineState nodeState = node.getState();
                List<Move> opponentMoves = getStateMachine().getLegalMoves(node.getState(), opponentRole);
                for (Move opponentMove : opponentMoves) {
                    List<Move> transitionMoveList = getMoveListFor2Players(nodeState, gamerMove, opponentMove);
                    MachineState childState = getStateMachine().getNextState(nodeState, transitionMoveList);
//                    Node newNode = node.createChildNode(childState, null, node.getGamerMove());
                    node.createChildNode(childState, null, transitionMoveList);

//                    addNodeToMap(newNode);
                }
            }
        }
    }

//    private void addNodeToMap(Node newNode) {
//        NodeKey nodeKey = NodeKey.forNode(newNode);
//        nodeMap.put(nodeKey, newNode);
//    }

//    private MachineState getNextMachineState(MachineState state, Move gamerMove, Move opponentMove) throws TransitionDefinitionException {
//        List<Move> moveList = getMoveListFor2Players(state, gamerMove, opponentMove);
//        return getStateMachine().getNextState(state, moveList);
//    }

    private List<Move> getMoveListFor2Players(MachineState state, Move gamerMove, Move opponentMove) {
        List<Move> result = new ArrayList<>();
        if (isGamerMove(state)) {
            result.add(gamerMove);
            result.add(opponentMove);
        } else {
            result.add(opponentMove);
            result.add(gamerMove);
        }
        return result;
    }

    /* Only works for 2-player game */
    private boolean isGamerMove(MachineState state) {
        Set<GdlSentence> contents = state.getContents();
        for (GdlSentence sent : contents) {
            String sentType = sent.getBody().get(0).toSentence().getName().toString();
            if (sentType.equals("control")) {
                final String gamer = "red";
                final String opponent = "black";
                String controllingPlayer = sent.getBody().get(0).toSentence().getBody().get(0).toString();
                switch (controllingPlayer) {
                    case gamer:
                        return true;
                    case opponent:
                        return false;
                    default:
                        throw new RuntimeException("failed to parse in isGamerMove");
                }
            }
        }
        throw new RuntimeException("failed to get gamer move");
    }

    private Role getOpponentRole() {
        Role gamerRole = getRole();
        for (Role role : getStateMachine().getRoles()) {
            if (!role.equals(gamerRole)) return role;
        }
        throw new RuntimeException("Opponent role not found");
    }

    /* Applies score to current node if appropriate and then backpropogates in light of this, possibly update score */
    private void backpropogate(Node node, int score) {
        if (node == null) throw new RuntimeException("can't backpropogate null node");
        node.addVisit();
        node.updateUtilityIfAppropriate(score);
        // FIXME make this so it only backpropogates if the score is updated
        backpropogate(node.getParent());
    }

    private void backpropogate(Node node) {

        if (node == null) return;

        node.addVisit();

        int newUtility;
        if (node.isMaxNode()) {
            newUtility = getUtilityOfMaxChild(node);
        } else {
            newUtility = getUtilityOfMinChild(node);
        }
        node.updateUtilityIfAppropriate(newUtility);

        // FIXME make this so it only backpropogates if the score is updated
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
            System.out.println("got terminal score of: " + getGoalScore(state));
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

//    private Node depthCharge(Node node) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
//        if (getStateMachine().isTerminal(node.getState())) {
//            int score = getGoalScore(node.getState());
//            node.setUtility(score);
//            return node;
//        } else {
//
//            if (node.getChildren().isEmpty()) {
//                expandNode(node);
//            }
//
//            Node randomChildNode = getRandomChildNode(node);
//            return depthCharge(randomChildNode);
//        }
//    }
//
//    private Node getRandomChildNode(Node node) {
//        List<Node> childNodes = node.getChildren();
//        return childNodes.get(random.nextInt(childNodes.size()));
//    }

    private int getGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }
}
