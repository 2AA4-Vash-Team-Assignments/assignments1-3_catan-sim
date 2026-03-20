package ca.mcmaster.se2aa4.catan;

/**
 * This class handles impliments the command interface to be able to undo and
 * redo the human action of building a city in the game, on some given tile.
 * 
 * @author Vaishnav Yandrapalli 400572601
 */
public class BuildCityCommand implements Command {

    private final Player player;
    private final Node node;
    private final CatanGame game;

    /**
     * Constructor for BuildCityCommand
     * 
     * @param player player building the city
     * @param node   node to build the city on
     * @param game   game the city is being built in
     */
    public BuildCityCommand(Player player, Node node, CatanGame game) {
        this.player = player;
        this.node = node;
        this.game = game;
    }

    @Override
    /**
     * Executes the command.
     */
    public boolean execute() {
        if (!player.canBuildCity()) {
            System.out.println("Cannot build city (resources or pieces).");
            return false;
        }
        player.buildCity(node, game.getBank());
        System.out.println(game.getCurrentRound() + " / P" + player.getId()
                + ": Built city at node " + node.getId());
        return true;
    }

    @Override
    /**
     * Undoes the command.
     */
    public void unexecute() {
        // downgrade back to settlement; reverse buildCity's piece accounting
        node.getBuilding().setType(BuildingType.SETTLEMENT);
        player.refundCity();
        player.addResource(ResourceType.WHEAT, 2);
        player.addResource(ResourceType.ORE, 3);
        // bank picked up these cards on execute, so return them (decrease supply back)
        game.getBank().distributeResource(ResourceType.WHEAT, 2);
        game.getBank().distributeResource(ResourceType.ORE, 3);
    }
}
