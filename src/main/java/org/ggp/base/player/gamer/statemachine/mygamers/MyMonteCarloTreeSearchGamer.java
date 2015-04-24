package org.ggp.base.player.gamer.statemachine.mygamers;

import com.sun.javafx.beans.annotations.NonNull;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;

public class MyMonteCarloTreeSearchGamer extends SampleGamer {

    private enum NodeType { MAX, MIN }

    private class Node {

        private NodeType nodeType;
        private MachineState state;
        private Node parent;
        private Move moveFromParent;
        private List<Node> children = new ArrayList<>();
        private int numVisits = 0;
        private int utility = 0;

//        public Node(NodeType nodeType, MachineState state, Node parent, Move moveFromParent) {
        public Node(MachineState state, Node parent, Move moveFromParent) {
            this.nodeType = nodeType;
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

        public void addChildNode(Node childNode) {
            children.add(childNode);
        }

        public Node createChildNode(MachineState state, Move move) {
//        public Node createChildNode(NodeType nodeType, MachineState state, Move move) {
//            Node childNode = new Node(nodeType, state, this, move);
            Node childNode = new Node(state, this, move);
            children.add(childNode);
            return childNode;
        }

        @NonNull
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

//    private static final int DEPTH_LIMIT = 3;
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
//            startNode = new Node(NodeType.MAX, getCurrentState(), null, null);
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

//
//        Move bestMove = null;
//        int bestScore = Integer.MIN_VALUE;
//        int worstScore = Integer.MAX_VALUE;
//        for (Move move : moves) {
//            int moveScore = getMinScore(getCurrentState(), move, 0);
//            if (moveScore == 100) {
//                System.out.println("Winning move found");
//                return move;
//            } else if (moveScore >= bestScore) {
//                bestScore = moveScore;
//                bestMove = move;
//            }
//            if (moveScore <= worstScore) {
//                worstScore = moveScore;
//            }
//        }
//        if (worstScore == bestScore) {
//            System.out.println("worstScore == bestScore, with score of: " + bestScore);
//            return getRandomMove(moves);
//        } else {
//            System.out.println("bestScore of: " + bestScore);
//            return bestMove;
//        }
    }

    // FIXME only works for 1-player game
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

            // FIXME this needs to alternate based on whether it is a min or max node??
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
        int nodeTotalUtility = node.getTotalUtility();
        int nodeVisits = node.getNumVisits();
        int parentNodeVisits = parentNode == null ? 0 : parentNode.getNumVisits();
        return (int) (nodeTotalUtility + Math.sqrt( 2 * Math.log(parentNodeVisits) / nodeVisits));
    }

    // FIXME only works for 1-player game
    private void expandNode(Node node) throws MoveDefinitionException, TransitionDefinitionException {
        MachineState nodeState = node.getState();
        List<Move> moveList = getStateMachine().getLegalMoves(nodeState, getRole());
        for (Move move : moveList) {
            MachineState childState = getStateMachine().getNextState(nodeState, Collections.singletonList(move));
            Node newNode = node.createChildNode(childState, move);
            nodeMap.put(newNode.getState(), newNode);
        }
    }

    // FIXME only works for 1-player game
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






    /*
     * Previous code
     */

    private Move getRandomMove(List<Move> moves) {
        return moves.get(random.nextInt(moves.size()));
    }

//    private int getMaxScore(MachineState state, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
//        if (getStateMachine().isTerminal(state)) {
//            return getGoalScore(state);
//        } else if (hasReachedDepthLimit(level)) {
//            return monteCarloSearch(state);
//        } else {
//            int maxScore = Integer.MIN_VALUE;
//            for (Move move : getStateMachine().getLegalMoves(state, getRole())) {
//                int score = getMinScore(state, move, level);
//                if (score >= maxScore) {
//                    maxScore = score;
//                }
//            }
//            return maxScore;
//        }
//    }

//    /*
//     * NOTE: Only works for 1-player or 2-player games
//     */
//    private int getMinScore(MachineState state, Move maximizedMove, int level) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
//
//        List<Role> roles = getStateMachine().getRoles();
//        switch (roles.size()) {
//            case 1:
//                // single player game
//                MachineState nextState = getStateMachine().getNextState(state, Collections.singletonList(maximizedMove));
//                return getMaxScore(nextState, level+1);
//            case 2:
//                // two player game
//                return getMinScoreFor2PlayerOpponent(state, maximizedMove, level, roles);
////                return getAverageScoreForRandom2PlayerOpponent(state, maximizedMove, level, roles);
//
//            default:
//                throw new RuntimeException("Game must have 1 or 2 roles, but has " + roles.size());
//        }
//    }

//    private int monteCarloSearch(MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
//        if (NUM_PROBES == 0) {
//            return 50;
//        } else {
//            int total = 0;
//            for (int i = 0; i < NUM_PROBES; i++) {
//                total += depthCharge(state);
//            }
//            return total / NUM_PROBES;
//        }
//    }

    // FIXME only works for 1-player
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


            // 2-player implementation of depth charge
//            List<Move> moveList = new ArrayList<>();
//            List<Role> roles = getStateMachine().getRoles();
//            for (Role role : roles) {
//                List<Move> legalMovesForRole = getStateMachine().getLegalMoves(state, role);
//                Move randomMove = getRandomMove(legalMovesForRole);
//                moveList.add(randomMove);
//            }
//            MachineState nextState = getStateMachine().getNextState(state, moveList);
//            return depthCharge(nextState);
        }
    }

    private Node getRandomChildNode(Node node) {
        List<Node> childNodes = node.getChildren();
        return childNodes.get(random.nextInt(childNodes.size()));
    }

//    private int getAverageScoreForRandom2PlayerOpponent(MachineState state, Move maximizedMove, int level, List<Role> roles) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
//        MachineState nextState;
//        Role opponent = getOpponentIn2PlayerGame(roles);
//
//        int sumOfScores = 0;
//        List<Move> opponentLegalMoves = getStateMachine().getLegalMoves(state, opponent);
//        for (Move opponentMove : opponentLegalMoves) {
//            List<Move> moveList = generateMoveListFor2Players(roles, maximizedMove, opponentMove);
//            nextState = getStateMachine().getNextState(state, moveList);
//            int score = getMaxScore(nextState, level + 1);
//            sumOfScores += score;
//        }
//        return sumOfScores / opponentLegalMoves.size();
//    }
//
//    private int getMinScoreFor2PlayerOpponent(MachineState state, Move maximizedMove, int level, List<Role> roles) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
//        MachineState nextState;
//        Role opponent = getOpponentIn2PlayerGame(roles);
//
//        int minScore = Integer.MAX_VALUE;
//        for (Move opponentMove : getStateMachine().getLegalMoves(state, opponent)) {
//            List<Move> moveList = generateMoveListFor2Players(roles, maximizedMove, opponentMove);
//            nextState = getStateMachine().getNextState(state, moveList);
//            int score = getMaxScore(nextState, level + 1);
//            if (score < minScore) {
//                minScore = score;
//            }
//        }
//        return minScore;
//    }
//
//    private List<Move> generateMoveListFor2Players(List<Role> roles, Move gamerMove, Move opponentMove) {
//        Role gamerRole = getRole();
//        if (roles.get(0).equals(gamerRole)) {
//            return Arrays.asList(gamerMove, opponentMove);
//        } else {
//            return Arrays.asList(opponentMove, gamerMove);
//        }
//    }
//
//    private Role getOpponentIn2PlayerGame(List<Role> roles) {
//        assert(roles.size() == 2);
//        Role gamerRole = getRole();
//        return roles.get(0).equals(gamerRole) ? roles.get(1) : roles.get(0);
//    }

    private int getGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }

//    private boolean hasReachedDepthLimit(int level) {
//        return level >= DEPTH_LIMIT;
//    }
}
