package org.ggp.base.player.gamer.statemachine.mygamers.MonteCarloTreeSearchTwoPlayer;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;

class NodeKey {

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
