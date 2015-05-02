package org.ggp.base.player.gamer.statemachine.mygamers.MonteCarloTreeSearchTwoPlayer;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

import java.util.List;

class NodeKey {

    private MachineState state;
    private List<Move> movesFromParent;

    public static NodeKey forNode(Node node) {
        return new NodeKey(node.getState(), node.getMovesFromParent());
    }

    public static NodeKey forState(MachineState state) {
        return new NodeKey(state, null);
    }

    private NodeKey(MachineState state, List<Move> movesFromParent) {
        this.state = state;
        this.movesFromParent = movesFromParent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NodeKey nodeKey = (NodeKey) o;

        if (state != null ? !state.equals(nodeKey.state) : nodeKey.state != null) return false;
        return !(movesFromParent != null ? !movesFromParent.equals(nodeKey.movesFromParent) : nodeKey.movesFromParent != null);
    }

    @Override
    public int hashCode() {
        int result = state != null ? state.hashCode() : 0;
        result = 31 * result + (movesFromParent != null ? movesFromParent.hashCode() : 0);
        return result;
    }
}
