package ca.mcmaster.se2aa4.catan;

import java.util.List;

/**
 * A computer-controlled player with rule-based machine intelligence.
 * Uses Chain of Responsibility for decision-making (R3.2, R3.3): each handler
 * in the chain checks if it applies (constraint or value-maximization); if so
 * returns an action, else passes to the next handler. All builds go through
 * CommandManager for undo/redo integration.
 */
public class AgentPlayer extends Player {

    private final AgentActionHandler actionChain;

    public AgentPlayer(int id) {
        super(id);
        this.actionChain = new ExcessCardsHandler(
                new RoadSegmentHandler(
                        new LongestRoadHandler(
                                new ValueMaximizingHandler())));
    }

    /**
     * Constructor for testing: inject custom handler chain.
     */
    AgentPlayer(int id, AgentActionHandler actionChain) {
        super(id);
        this.actionChain = actionChain;
    }

    @Override
    public void takeTurn(CatanGame game) {
        int diceRoll = game.rollDice();
        if (diceRoll == 7) {
            game.handleRollSeven(this);
        } else {
            game.distributeResources(diceRoll);
        }
        chooseAction(game);
        game.updateLongestRoad();
    }

    /**
     * R3.2, R3.3: Passes the action-selection request through the handler chain.
     * The loop re-enters the chain after each action so constraint handlers are
     * re-evaluated. Terminates when the chain returns no action (the agent has
     * nothing profitable to do or cannot afford anything).
     */
    private void chooseAction(CatanGame game) {
        CommandManager manager = game.getCommandManager();
        List<Player> allPlayers = game.getPlayers();

        Command cmd = actionChain.handle(this, game, allPlayers);
        while (cmd != null) {
            manager.execute(cmd);
            cmd = actionChain.handle(this, game, allPlayers);
        }
    }
}
