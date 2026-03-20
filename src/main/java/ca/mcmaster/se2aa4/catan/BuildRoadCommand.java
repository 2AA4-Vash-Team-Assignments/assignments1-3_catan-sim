package ca.mcmaster.se2aa4.catan;

import java.util.List;

/**
 * Class impliments command interface to be able to undo and redo the human
 * action of building a road between two nodes.
 * 
 * @author Vaishnav Yandrapalli 400572601
 */
public class BuildRoadCommand implements Command {

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
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        // bank gave out these cards on execute so return them (decrease supply)
        game.getBank().distributeResource(ResourceType.BRICK, 1);
        game.getBank().distributeResource(ResourceType.WOOD, 1);
    }
}
