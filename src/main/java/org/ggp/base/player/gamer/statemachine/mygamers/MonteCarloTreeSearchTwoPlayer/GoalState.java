package org.ggp.base.player.gamer.statemachine.mygamers.MonteCarloTreeSearchTwoPlayer;

public class GoalState {

    final private int gamerGoalUtility;
    final private int opponentGoalUtility;

    public GoalState(int gamerGoalUtility, int opponentGoalUtility) {
        this.gamerGoalUtility = gamerGoalUtility;
        this.opponentGoalUtility = opponentGoalUtility;
    }

    public int getGamerGoalUtility() {
        return gamerGoalUtility;
    }

    public int getOpponentGoalUtility() {
        return opponentGoalUtility;
    }
}
