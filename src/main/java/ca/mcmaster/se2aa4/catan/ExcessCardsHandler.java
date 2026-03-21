package ca.mcmaster.se2aa4.catan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * R3.3: If agent has more than 7 cards, must spend. Returns any valid build.
 */
public class ExcessCardsHandler extends AgentActionHandler {

    private final Random random = new Random();

    public ExcessCardsHandler(AgentActionHandler next) {
        super(next);
    }

    @Override
    protected Command tryHandle(Player agent, CatanGame game, List<Player> allPlayers) {
        if (agent.getTotalResourceCards() <= 7) {
            return null;
        }
        Board board = game.getBoard();
        List<Command> options = new ArrayList<>();
        if (agent.canBuildCity()) {
            for (Node node : board.getUpgradeableNodes(agent)) {
                options.add(new BuildCityCommand(agent, node, game));
            }
        }
        if (agent.canBuildSettlement()) {
            for (Node node : board.getAvailableSettlementNodes(agent)) {
                options.add(new BuildSettlementCommand(agent, node, game));
            }
        }
        if (agent.canBuildRoad()) {
            for (Edge edge : board.getAvailableRoadEdges(agent)) {
                options.add(new BuildRoadCommand(agent, edge, game));
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        return options.get(random.nextInt(options.size()));
    }
}
