package ca.mcmaster.se2aa4.catan;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * R3.2: Evaluates actions by value (VP=1.0, build without VP=0.8, spend to &lt;5=0.5)
 * and selects the highest. Ties broken randomly. No successor—terminal handler.
 */
public class ValueMaximizingHandler extends AgentActionHandler {

    private static final double VALUE_VP = 1.0;
    private static final double VALUE_BUILD_NO_VP = 0.8;
    private static final double VALUE_SPEND_UNDER_FIVE = 0.5;

    private final Random random = new Random();

    public ValueMaximizingHandler() {
        super(null);
    }

    @Override
    protected Command tryHandle(Player agent, CatanGame game, List<Player> allPlayers) {
        List<ScoredCommand> candidates = collectScoredActions(agent, game);
        if (candidates.isEmpty()) {
            return null;
        }
        double best = candidates.stream().mapToDouble(ScoredCommand::getScore).max().orElse(0);
        List<ScoredCommand> bestList = candidates.stream()
                .filter(sc -> sc.getScore() == best)
                .toList();
        return bestList.get(random.nextInt(bestList.size())).getCommand();
    }

    private List<ScoredCommand> collectScoredActions(Player agent, CatanGame game) {
        Board board = game.getBoard();
        List<ScoredCommand> result = new ArrayList<>();

        if (agent.canBuildCity()) {
            for (Node node : board.getUpgradeableNodes(agent)) {
                double value = evaluateAction(agent, true, 5);
                result.add(new ScoredCommand(new BuildCityCommand(agent, node, game), value));
            }
        }
        if (agent.canBuildSettlement()) {
            for (Node node : board.getAvailableSettlementNodes(agent)) {
                double value = evaluateAction(agent, true, 4);
                result.add(new ScoredCommand(new BuildSettlementCommand(agent, node, game), value));
            }
        }
        if (agent.canBuildRoad()) {
            for (Edge edge : board.getAvailableRoadEdges(agent)) {
                double value = evaluateAction(agent, false, 2);
                result.add(new ScoredCommand(new BuildRoadCommand(agent, edge, game), value));
            }
        }
        return result;
    }

    private double evaluateAction(Player agent, boolean earnsVP, int resourceCost) {
        if (earnsVP) {
            return VALUE_VP;
        }
        int cardsAfter = agent.getTotalResourceCards() - resourceCost;
        if (cardsAfter < 5) {
            return VALUE_SPEND_UNDER_FIVE;
        }
        return VALUE_BUILD_NO_VP;
    }

    private static class ScoredCommand {
        private final Command command;
        private final double score;

        ScoredCommand(Command command, double score) {
            this.command = command;
            this.score = score;
        }

        Command getCommand() {
            return command;
        }

        double getScore() {
            return score;
        }
    }
}
