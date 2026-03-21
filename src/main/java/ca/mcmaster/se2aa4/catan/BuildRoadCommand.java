package ca.mcmaster.se2aa4.catan;

import java.util.List;

/**
 * Implements the Command interface to support undo and redo for the
 * action of building a road between two nodes.
 * 
 * @author Vaishnav Yandrapalli 400572601
 */
public class BuildRoadCommand implements Command {

    static final int BRICK_COST = 1;
    static final int WOOD_COST = 1;
    static final int TOTAL_COST = BRICK_COST + WOOD_COST;

    private final Player player;
    private final Edge edge;
    private final CatanGame game;

    public BuildRoadCommand(Player player, Edge edge, CatanGame game) {
        this.player = player;
        this.edge = edge;
        this.game = game;
    }

    @Override
    public boolean execute() {
        if (!player.canBuildRoad()) {
            System.out.println("Cannot build road (resources or pieces).");
            return false;
        }
        player.buildRoad(edge, game.getBank());
        List<Node> ep = edge.getEndpoints();
        System.out.println(game.getCurrentRound() + " / P" + player.getId()
                + ": Built road between nodes " + ep.get(0).getId() + " and " + ep.get(1).getId());
        return true;
    }

    @Override
    public void unexecute() {
        edge.setRoad(null);
        player.refundRoad();
        player.addResource(ResourceType.BRICK, BRICK_COST);
        player.addResource(ResourceType.WOOD, WOOD_COST);
        game.getBank().distributeResource(ResourceType.BRICK, BRICK_COST);
        game.getBank().distributeResource(ResourceType.WOOD, WOOD_COST);
    }
}
