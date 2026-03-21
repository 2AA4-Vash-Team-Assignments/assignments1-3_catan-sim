package ca.mcmaster.se2aa4.catan;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * R3.3: If agent has more than 7 cards, must spend. Returns any valid build.
 */
public class ExcessCardsHandler extends AgentActionHandler {

    private final Random random = new SecureRandom();

    public ExcessCardsHandler(AgentActionHandler next) {
        super(next);
    }

    private static final double VALUE_VP = 1.0;
    private static final double VALUE_BUILD_NO_VP = 0.8;
    private static final double VALUE_SPEND_UNDER_FIVE = 0.5;

    @Override
    protected Command tryHandle(Player agent, CatanGame game, List<Player> allPlayers) {
        if (agent.getTotalResourceCards() <= 7) {
            return null;
        }
        Board board = game.getBoard();
        List<Command> options = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        if (agent.canBuildCity()) {
            for (Node node : board.getUpgradeableNodes(agent)) {
                options.add(new BuildCityCommand(agent, node, game));
                scores.add(scoreAction(agent, true, 5));
            }
        }
        if (agent.canBuildSettlement()) {
            for (Node node : board.getAvailableSettlementNodes(agent)) {
                options.add(new BuildSettlementCommand(agent, node, game));
                scores.add(scoreAction(agent, true, 4));
            }
        }
        if (agent.canBuildRoad()) {
            for (Edge edge : board.getAvailableRoadEdges(agent)) {
                options.add(new BuildRoadCommand(agent, edge, game));
                scores.add(scoreAction(agent, false, 2));
            }
        }
        if (options.isEmpty()) {
            return null;
        }
        // Pick the highest-scored option; break ties randomly
        double best = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        List<Integer> bestIndices = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i) == best) {
                bestIndices.add(i);
            }
        }
        return options.get(bestIndices.get(random.nextInt(bestIndices.size())));
    }

    private double scoreAction(Player agent, boolean earnsVP, int resourceCost) {
        if (earnsVP) {
            return VALUE_VP;
        }
        int cardsAfter = agent.getTotalResourceCards() - resourceCost;
        if (cardsAfter < 5) {
            return VALUE_SPEND_UNDER_FIVE;
        }
        return VALUE_BUILD_NO_VP;
    }
}
