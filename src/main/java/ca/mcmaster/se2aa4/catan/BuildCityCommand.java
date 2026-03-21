package ca.mcmaster.se2aa4.catan;

/**
 * Implements the Command interface to support undo and redo for the
 * action of upgrading a settlement to a city on a given node.
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

    /** Executes the city upgrade if the player can afford it. */
    @Override
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

    /** Undoes the city upgrade, downgrading back to a settlement. */
    @Override
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
