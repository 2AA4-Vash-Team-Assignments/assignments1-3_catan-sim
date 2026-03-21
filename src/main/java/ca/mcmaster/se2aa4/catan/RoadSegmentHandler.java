package ca.mcmaster.se2aa4.catan;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * R3.3: If two road segments are at most 2 units apart, try to buy roads to connect.
 */
public class RoadSegmentHandler extends AgentActionHandler {

    private final Random random = new SecureRandom();

    public RoadSegmentHandler(AgentActionHandler next) {
        super(next);
    }

    @Override
    protected Command tryHandle(Player agent, CatanGame game, List<Player> allPlayers) {
        Board board = game.getBoard();
        if (!board.hasRoadSegmentsWithinTwoUnits(agent) || !agent.canBuildRoad()) {
            return null;
        }
        List<Edge> available = board.getAvailableRoadEdges(agent);
        if (available.isEmpty()) {
            return null;
        }
        // Filter to edges adjacent to nodes that are part of the player's road segments
        // so we extend existing roads rather than building disconnected ones
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
}
