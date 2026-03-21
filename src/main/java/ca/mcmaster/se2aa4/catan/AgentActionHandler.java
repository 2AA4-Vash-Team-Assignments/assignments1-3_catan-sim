package ca.mcmaster.se2aa4.catan;

import java.util.List;

/**
 * Abstract handler for the Chain of Responsibility pattern. Each handler
 * processes the "select action" request: if it can handle (constraint applies
 * or it's the value-maximizing fallback), it returns a Command; otherwise it
 * passes the request to the next handler in the chain. This matches R3.3:
 * constraints must be resolved before value-added actions.
 *
 * @author Task 2 - Chain of Responsibility
 */
public abstract class AgentActionHandler {

    protected final AgentActionHandler next;

    public AgentActionHandler(AgentActionHandler next) {
        this.next = next;
    }

    /**
     * Handles the request. If this handler applies, returns an action;
     * otherwise delegates to the next handler.
     */
    public Command handle(Player agent, CatanGame game, List<Player> allPlayers) {
        Command result = tryHandle(agent, game, allPlayers);
        if (result != null) {
            return result;
        }
        return next != null ? next.handle(agent, game, allPlayers) : null;
    }

    /**
     * Attempts to handle the request. Returns a Command if this handler applies,
     * null to pass to the next handler.
     */
    protected abstract Command tryHandle(Player agent, CatanGame game, List<Player> allPlayers);
}
