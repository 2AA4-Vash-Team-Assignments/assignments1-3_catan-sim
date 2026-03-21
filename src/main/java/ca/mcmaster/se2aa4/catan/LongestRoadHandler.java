package ca.mcmaster.se2aa4.catan;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * R3.3: If another player has longest road at most 1 shorter than the agent's,
 * buy a connected road segment.
 */
public class LongestRoadHandler extends AgentActionHandler {

    private final Random random = new SecureRandom();

    public LongestRoadHandler(AgentActionHandler next) {
        super(next);
    }

    @Override
    protected Command tryHandle(Player agent, CatanGame game, List<Player> allPlayers) {
        if (!isLongestRoadThreat(agent, game, allPlayers) || !agent.canBuildRoad()) {
            return null;
        }
        Board board = game.getBoard();
        List<Edge> available = board.getAvailableRoadEdges(agent);
        if (available.isEmpty()) {
            return null;
        }
        // Filter to edges connected to existing roads so we extend the longest road
        Set<Node> roadNodes = collectPlayerRoadNodes(board, agent);
        List<Edge> connected = new ArrayList<>();
        for (Edge e : available) {
            for (Node endpoint : e.getEndpoints()) {
                if (roadNodes.contains(endpoint)) {
                    connected.add(e);
                    break;
                }
            }
        }
        List<Edge> candidates = connected.isEmpty() ? available : connected;
        Edge edge = candidates.get(random.nextInt(candidates.size()));
        return new BuildRoadCommand(agent, edge, game);
    }

    private Set<Node> collectPlayerRoadNodes(Board board, Player player) {
        Set<Node> roadNodes = new HashSet<>();
        for (Edge edge : board.getEdges()) {
            if (edge.isOccupied() && edge.getRoad().getOwner() == player) {
                roadNodes.addAll(edge.getEndpoints());
            }
        }
        return roadNodes;
    }

    private boolean isLongestRoadThreat(Player agent, CatanGame game, List<Player> allPlayers) {
        int myLength = game.getBoard().calculateLongestRoad(agent);
        for (Player other : allPlayers) {
            if (other == agent) {
                continue;
            }
            int otherLength = game.getBoard().calculateLongestRoad(other);
            if (otherLength >= myLength - 1 && otherLength <= myLength && myLength > 0) {
                return true;
            }
        }
        return false;
    }
}
