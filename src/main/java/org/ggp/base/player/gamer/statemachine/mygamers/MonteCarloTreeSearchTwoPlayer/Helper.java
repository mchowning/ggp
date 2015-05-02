package org.ggp.base.player.gamer.statemachine.mygamers.MonteCarloTreeSearchTwoPlayer;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;

public class Helper {

    public static boolean statesMatch(MachineState state1, MachineState state2) {
        for (GdlSentence state1Sent : state1.getContents()) {
            boolean hasMatch = false;
            for (GdlSentence state2Sent : state2.getContents()) {
                if (state1Sent.equals(state2Sent)) {
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) return false;
        }
        return true;
    }
}
