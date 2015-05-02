package org.ggp.base.player.gamer.statemachine.mygamers.MonteCarloTreeSearchTwoPlayer;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import sun.plugin.dom.exception.InvalidStateException;

import java.util.ArrayList;
import java.util.List;

class Node {

//    private static final int UNINITIALIZED_UTILITY = Integer.MIN_VALUE + 1;
    private final static String NOOP_MOVE_STRING = "noop";

    private MachineState state;
    private Node parent;
    private List<Move> movesFromParent;
    private StateMachine stateMachine;
    private Role gamerRole;

    private List<Move> availableGamerMoves;
    private List<Move> availableOpponentMoves;

    private List<Node> children = new ArrayList<>();
    private int numVisits = 0;
    private int gamerTotalUtility = 0;
    private int opponentTotalUtility = 0;
//    private Integer utility = 0;
//    private Integer utility = UNINITIALIZED_UTILITY;

    public Node(MachineState state, Node parent, List<Move> movesFromParent, StateMachine stateMachine, Role gamerRole) {
        if (stateMachine == null || gamerRole == null) throw new InvalidStateException("Node must have stateMachine and gamerRole defined");
        this.state = state;
        this.parent = parent;
        this.movesFromParent = movesFromParent;
        this.stateMachine = stateMachine;
        this.gamerRole = gamerRole;
        fillAvailableMoves();
    }

    public void fillAvailableMoves() {
        try {
            for (Role role : stateMachine.getRoles()) {
                if (!stateMachine.isTerminal(getState())) {
                    List<Move> availableMovesForRole = stateMachine.getLegalMoves(getState(), role);
                    if (role.equals(gamerRole)) {
                        availableGamerMoves = availableMovesForRole;
                    } else {
                        availableOpponentMoves = availableMovesForRole;
                    }
                }
            }
        } catch (MoveDefinitionException e) {
            System.out.println("Could not fill available moves for node");
        }
    }

    public List<Move> getAvailableGamerMoves() {
        return availableGamerMoves;
    }

    public List<Move> getAvailableOpponentMoves() {
        return availableOpponentMoves;
    }

    public MachineState getState() {
        return state;
    }

    public boolean isGamerNode() {
        int numGamerMoves = availableGamerMoves.size();
        int numOppMoves = availableOpponentMoves.size();
        if (numGamerMoves > 1 && numOppMoves == 1) {
            return true;
        } else if (numGamerMoves == 1 && numOppMoves > 1) {
            return false;
        } else if (numGamerMoves == 1 && numOppMoves == 1) {
            // TODO is this right?
            return true;  // doesn't matter because move is forced
        } else {
            throw new RuntimeException("failed to parse whether node was gamer node");
        }
    }

    public boolean isOpponentNode() {
        return !isGamerNode();
    }

    public Node getParent() {
        return parent;
    }

    public void dropParent() {
        parent = null;
    }

    public Node createChildNode(MachineState state, List<Move> moveFromParent) throws MoveDefinitionException {
        Node childNode = new Node(state, this, moveFromParent, stateMachine, gamerRole);
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

//    public boolean hasInitializedUtility() {
//        return utility != UNINITIALIZED_UTILITY;
//    }

//    public double getUtility() {
////        if (!hasInitializedUtility()) {
////            throw new RuntimeException("should not be getting uninitialized utility");
////        }
////            return utility;
//
//        if (numVisits == 0) {
//            return 0;
//        } else {
//            return utility / numVisits;
//        }
//    }

    public double getGamerUtility() {
        return getAverageUtility(gamerTotalUtility);
    }

    public double getOpponentUtility() {
        return getAverageUtility(opponentTotalUtility);
    }

    private double getAverageUtility(int totalUtility) {
        if (numVisits == 0) return 0;
        return totalUtility / numVisits;

    }

//    public int getUtilityForNodeRole() {
//        return isGamerNode() ? gamerTotalUtility : opponentTotalUtility;
//    }

    public double getUtilityForRole(boolean isGamerUtility) {
        return isGamerUtility ? getGamerUtility() : getOpponentUtility();
    }

    public List<Move> getMovesFromParent() {
        return movesFromParent;
    }

    public void updateUtilityWithGoalState(GoalState score) {
        gamerTotalUtility += score.getGamerGoalUtility();
        opponentTotalUtility += score.getOpponentGoalUtility();
    }
}
