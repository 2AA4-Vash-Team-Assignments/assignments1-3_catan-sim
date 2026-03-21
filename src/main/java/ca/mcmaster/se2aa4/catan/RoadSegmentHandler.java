package ca.mcmaster.se2aa4.catan;

import java.util.List;
import java.util.Random;

/**
 * R3.3: If two road segments are at most 2 units apart, try to buy roads to connect.
 */
public class RoadSegmentHandler extends AgentActionHandler {

    private final Random random = new Random();

    public RoadSegmentHandler(AgentActionHandler next) {
        super(next);
    }

    @Override
    protected Command tryHandle(Player agent, CatanGame game, List<Player> allPlayers) {
        Board board = game.getBoard();
        if (!board.hasRoadSegmentsWithinTwoUnits(agent)) {
            return null;
        }
        List<Edge> available = board.getAvailableRoadEdges(agent);
        if (available.isEmpty()) {
            return null;
        }
        Edge edge = available.get(random.nextInt(available.size()));
        return new BuildRoadCommand(agent, edge, game);
    }
}
