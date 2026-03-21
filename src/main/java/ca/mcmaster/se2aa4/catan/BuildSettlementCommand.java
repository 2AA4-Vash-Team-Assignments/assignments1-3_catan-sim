package ca.mcmaster.se2aa4.catan;

/**
 * Implements the Command interface to support undo and redo for the
 * action of building a settlement on a given node.
 * 
 * @author Vaishnav Yandrapalli 400572601
 */
public class BuildSettlementCommand implements Command {

    private final Player player;
    private final Node node;
    private final CatanGame game;

    /**
     * Constructor for the BuildSettlementCommand class
     * 
     * @param player
     * @param node
     * @param game
     */
    public BuildSettlementCommand(Player player, Node node, CatanGame game) {
        this.player = player;
        this.node = node;
        this.game = game;
    }

    @Override
    public boolean execute() {
        if (!player.canBuildSettlement()) {
            System.out.println("Cannot build settlement (resources or pieces).");
            return false;
        }
        player.buildSettlement(node, game.getBank());
        System.out.println(game.getCurrentRound() + " / P" + player.getId()
                + ": Built settlement at node " + node.getId());
        return true;
    }

    @Override
    public void unexecute() {
        node.setBuilding(null);
        player.refundSettlement();
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        player.addResource(ResourceType.WHEAT, 1);
        player.addResource(ResourceType.SHEEP, 1);
        game.getBank().distributeResource(ResourceType.BRICK, 1);
        game.getBank().distributeResource(ResourceType.WOOD, 1);
        game.getBank().distributeResource(ResourceType.WHEAT, 1);
        game.getBank().distributeResource(ResourceType.SHEEP, 1);
    }
}
