package org.ggp.base.player.gamer.statemachine.mygamers.MonteCarloTreeSearchTwoPlayer;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

import java.util.ArrayList;
import java.util.List;

class Node {

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
//        private Integer utility = UNINITIALIZED_UTILITY;
    private Integer utility = 0;


    public Node(MachineState state, Move gamerMove, Node parent, List<Move> movesFromParent) throws MoveDefinitionException {
        this.state = state;
        this.gamerMove = gamerMove;
        this.parent = parent;
        this.movesFromParent = movesFromParent;
    }

    public void fillAvailableMoves(StateMachine machine, Role gamerRole) {
        try {
            for (Role role : machine.getRoles()) {
                if (!machine.isTerminal(getState())) {
                    List<Move> availableMovesForRole = machine.getLegalMoves(getState(), role);
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

    public boolean isGamerMoveNode() {
        return gamerMove == null;
    }

    public boolean isOpponentMoveNode() {
        return !isGamerMoveNode();
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
//            return utility;
        if (numVisits == 0) {
            return 0;
        } else {
            return utility / numVisits;
        }
    }

//        public void setUtility(int utility) {
//            this.utility = utility;
//        }

    public void updateUtilityIfAppropriate(int possibleNewUtilty) {
//            if (utility == UNINITIALIZED_UTILITY ||
//               ( isOpponentMoveNode() && possibleNewUtilty > utility ) ||  // these are opposite because the value of a max node is determined by a min node in a 2-player game
//               ( isGamerMoveNode() && possibleNewUtilty < utility ) )
//            {
//                utility = possibleNewUtilty;
//            }
        utility += possibleNewUtilty;
    }

    public void setMovesFromParent(List<Move> moveList) {
        movesFromParent = moveList;
    }

    public List<Move> getMovesFromParent() {
        return movesFromParent;
    }
}
