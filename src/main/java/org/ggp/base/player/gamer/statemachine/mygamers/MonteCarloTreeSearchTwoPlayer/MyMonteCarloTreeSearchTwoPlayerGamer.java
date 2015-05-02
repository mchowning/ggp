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

    private static final int NUM_PROBES = 100;
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

//        NodeKey keyForCurrentStateWithNoMoves = NodeKey.forState(getCurrentState());
//        if (!nodeMap.containsKey(keyForCurrentStateWithNoMoves)) {
//            Node newNode = new Node(getCurrentState(), null, null, null, getRole());
//            nodeMap.put(keyForCurrentStateWithNoMoves, newNode);
//        }
//        startNode = nodeMap.get(keyForCurrentStateWithNoMoves);

//        if (rootNode == null) {
//            rootNode =  new Node(getCurrentState(), null, null, null);
//            rootNode.fillAvailableMoves(getStateMachine(), getRole());
//        } else {
//            Node newRootNode = null;
//            for (Node firstChild : rootNode.getChildren()) {
//                for (Node secondChild : firstChild.getChildren()) {
//                    if (matchesCurrentState(secondChild.getState())) {
//                        newRootNode = secondChild;
//                    }
//                }
//            }
//            if (newRootNode == null) throw new RuntimeException("failed to find new root node");
//            rootNode = newRootNode;
//            rootNode.dropParent();
//        }

        rootNode =  new Node(getCurrentState(), null, null, getStateMachine(), getRole());

//        while (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) < TIME_CUSHION_MILLIS) {
        while (System.currentTimeMillis() < allowedSolveTime) {



            Node selectedNode = selectNode(rootNode);
            expandNode(selectedNode);
            GoalState randomTerminalGoalState = getRandomTerminalValue(selectedNode);
            backpropogate(selectedNode, randomTerminalGoalState);
        }

        Node bestChildNode = null;
        for (Node node : rootNode.getChildren()) {
            System.out.println("gamer: " + node.getGamerUtility() + ", opp: " + node.getOpponentUtility() + " for move: " + node.getMovesFromParent() + ", with " + node.getNumVisits() + " visits");
            if (bestChildNode == null || node.getGamerUtility() > bestChildNode.getGamerUtility()) {
                bestChildNode = node;
            }
        }
        System.out.println("number of visits to rootNode: " + rootNode.getNumVisits());
        System.out.println("best move gamer score: " + bestChildNode.getGamerUtility() + "\n");
        return bestChildNode.getMovesFromParent().get(INDEX_OF_GAMER_ROLE);
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

    // TODO combine separate nodes for both moves to a single node where 1 move is predetermined

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
    private void backpropogate(Node node, GoalState score) {
//        if (node == null) throw new RuntimeException("can't backpropogate null node");
        if (node != null) {
            node.addVisit();
//            node.updateUtilityIfAppropriate(score);
//            if (node.isGamerNode()) {
//
//            } else {
//
//            }
            node.updateUtilityWithGoalState(score);
            backpropogate(node.getParent(), score);
        }
        // TODO make this so it only backpropogates if the score is updated
//        backpropogate(node.getParent());
    }

//    private void backpropogate(Node node) {
//
//        if (node == null) return;
//
//        node.addVisit();
//
//        int newUtility;
////        if (node.isGamerMoveNode()) {
//            newUtility = getUtilityOfMaxChild(node);
////        } else {
////            newUtility = getUtilityOfMinChild(node);
////        }
//        node.updateUtilityIfAppropriate(newUtility);
//
//        // TODO make this so it only backpropogates if the score is updated
//        backpropogate(node.getParent());
//    }

//    // TODO try using lambda expressions here?
//    private double getUtilityOfMaxChild(Node node) {
//        double maxUtility = Integer.MIN_VALUE;
//        for (Node childNode : node.getChildren()) {
//            double childUtility = childNode.getUtility();
//            if (childUtility > maxUtility) {
//                maxUtility = childUtility;
//            }
//        }
//        return maxUtility;
//    }
//
//    // TODO try using lambda expressions here?
//    private double getUtilityOfMinChild(Node node) {
//        double minUtility = Integer.MAX_VALUE;
//        for (Node childNode : node.getChildren()) {
//            double childUtility = childNode.getUtility();
//            if (childUtility < minUtility) {
//                minUtility = childUtility;
//            }
//        }
//        return minUtility;
//    }

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
//                Move randomMove = getRandomMove(legalMovesForRole);
//                transitionMoveList.add(randomMove);

//                Move selectedMove = isGamer(role) ? getAggressiveMove(legalMovesForRole) : getRandomMove(legalMovesForRole);
                // TODO problem is that no matter what, early jumps seemed to get evened out

//                Move selectedMove = isGamer(role) ? getAggressiveMove(legalMovesForRole) : getPassiveMove(legalMovesForRole);
                // TODO could try not allowing passive moves, i.e. moves that go backwards (or sideways?) unless they're a jump?

//                Move selectedMove;
//                if (legalMovesForRole.size() == 1) {
//                    selectedMove = legalMovesForRole.get(0);
//                } else {
//                    selectedMove = getProgressMove(legalMovesForRole, role);
//                }

                Move selectedMove = getRandomMove(legalMovesForRole);

                transitionMoveList.add(selectedMove);
            }
            MachineState nextState = getStateMachine().getNextState(state, transitionMoveList);
            return depthCharge(nextState);
        }
    }

    private Move getProgressMove(List<Move> moves, Role role) {
        List<Move> sidewaysMoves = new ArrayList<>();
        List<Move> forwardOrJumpMove = new ArrayList<>();
        for (Move move : moves) {
            if (isJumpMove(move) || isForwardMove(move, role)) {
                forwardOrJumpMove.add(move);
            } else if (isSidewaysMove(move)) {
                sidewaysMoves.add(move);
            }
        }
        if (forwardOrJumpMove.size() > 0) {
            return getRandomMove(forwardOrJumpMove);
        } else if (sidewaysMoves.size() > 0) {
            return getRandomMove(sidewaysMoves);
        } else {
            return getRandomMove(moves);
        }
    }

    private static final String BOTTOM_ROLE = "red";

    private boolean isForwardMove(Move move, Role role) {
        int startRow = getMoveStartRow(move);
        int endRow = getMoveEndRow(move);
        if (role.toString().equals(BOTTOM_ROLE)) {
            return startRow > endRow;
        } else {
            return startRow < endRow;
        }
    }

    private boolean isSidewaysMove(Move move) {
        return getMoveStartRow(move) == getMoveEndRow(move);
    }

    private static final String MOVE_JUMP = "jump";
    private static final String MOVE_REGULAR = "move";
    private static final String MOVE_NOOP = "noop";

    private boolean isJumpMove(Move move) {
        return move.toString().contains(MOVE_JUMP);
    }

    private boolean isNoopMove(Move move) {
        return move.toString().contains(MOVE_NOOP);
    }

//    private String getMoveType(Move move) {
//        return move.getContents().toSentence().getName().toString();
//    }

    private static final int MOVE_START_ROW_INDEX = 0;
    private static final int MOVE_START_COLUMN_INDEX = 1;
    private static final int MOVE_END_ROW_INDEX = 2;
    private static final int MOVE_END_COLUMN_INDEX = 3;

    private int getMoveStartRow(Move move) {
        return getMoveCoordinate(move, MOVE_START_ROW_INDEX);
    }

    private int getMoveStartColumn(Move move) {
        return getMoveCoordinate(move, MOVE_START_COLUMN_INDEX);
    }

    private int getMoveEndRow(Move move) {
        return getMoveCoordinate(move, MOVE_END_ROW_INDEX);
    }

    private int getMoveEndColumn(Move move) {
        return getMoveCoordinate(move, MOVE_END_COLUMN_INDEX);
    }

    /*
    * index 0 = [start row (top->bottom from 1)]
    *       1 = [start column (left->right from 1)]
    *       2 = [end row]
    *       3 = [end column]
    */
    private int getMoveCoordinate(Move move, int coordinateIndex) {
        try {
            String coordinateString = move.getContents().toSentence().getBody().get(coordinateIndex).toString();
            return Integer.parseInt(coordinateString);
        } catch (Exception e) {
            // TESTING
            e.printStackTrace();
            throw new RuntimeException("problem getting move coordinate");
        }
    }

    /*
     * move/noop/jump
     * [start row (top->bottom from 1)]
     * [start column (left->right from 1)]
     * [end row]
     * [end column]
     * move type = move.getContents().toSentence().getName().toString();
     * indexes for numbers = move.getContents().toSentence().getBody().get([index]).toString()
     */

    private boolean isGamer(Role role) {
        return role.equals(getRole());
    }

    private Move getRandomMove(List<Move> moves) {
        return moves.get(random.nextInt(moves.size()));
    }

    private Move getAggressiveMove(List<Move> moves) {
        List<Move> aggressiveMoves = new ArrayList<>();
        for (Move move : moves) {
            if (isJumpMove(move)) aggressiveMoves.add(move);
        }
        if (aggressiveMoves.isEmpty()) {
            return getRandomMove(moves);
        } else {
            return getRandomMove(aggressiveMoves);
        }
    }

    private Move getPassiveMove(List<Move> moves) {
        List<Move> passiveMoves = new ArrayList<>();
        for (Move move : moves) {
            if (!isJumpMove(move)) passiveMoves.add(move);
        }
        if (passiveMoves.isEmpty()) {
            return getRandomMove(moves);
        } else {
            return getRandomMove(passiveMoves);
        }
    }

    private GoalState getGoalState(MachineState state) throws GoalDefinitionException {
        int gamerValue = getGamerGoalScore(state);
        int opponentValue = getOpponentGoalScore(state);
        return new GoalState(gamerValue, opponentValue);
    }

    private int getGamerGoalScore(MachineState currentState) throws GoalDefinitionException {
        return getStateMachine().getGoal(currentState, getRole());
    }

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
