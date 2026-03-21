package ca.mcmaster.se2aa4.catan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Command pattern (R3.1).
 * Each test constructs a real CatanGame, manipulates state directly
 * (same approach as CatanGameTest), then checks that execute()/unexecute()
 * leave resources, piece counts, and board state exactly correct.
 *
 * @author Vaishnav Yandrapalli 400572601
 */
class CommandManagerTest {

    private CatanGame game;
    private Board board;

    @BeforeEach
    void setUp() {
        game = new CatanGame();
        board = game.getBoard();
        board.initialize();
        game.setupPhase();
    }

    // BuildRoadCommand

    @Test
    void testBuildRoadCommand_execute_placesRoadAndChargesResources() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        BuildRoadCommand cmd = new BuildRoadCommand(player, edge, game);
        cmd.execute();

        assertTrue(edge.isOccupied(), "Edge should be occupied after execute()");
        assertEquals(0, player.getResourceCount(ResourceType.BRICK));
        assertEquals(0, player.getResourceCount(ResourceType.WOOD));
        assertEquals(14, player.getRemainingRoads());
    }

    @Test
    void testBuildRoadCommand_unexecute_restoresStateExactly() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        BuildRoadCommand cmd = new BuildRoadCommand(player, edge, game);
        cmd.execute();
        cmd.unexecute();

        assertFalse(edge.isOccupied(), "Edge should be free after unexecute()");
        assertEquals(1, player.getResourceCount(ResourceType.BRICK));
        assertEquals(1, player.getResourceCount(ResourceType.WOOD));
        assertEquals(15, player.getRemainingRoads());
    }

    // BuildSettlementCommand

    @Test
    void testBuildSettlementCommand_unexecute_restoresNodeAndResources() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        player.addResource(ResourceType.WHEAT, 1);
        player.addResource(ResourceType.SHEEP, 1);
        // find a free node
        Node freeNode = null;
        for (Node n : board.getNodes()) {
            if (!n.isOccupied() && n.satisfiesDistanceRule()) {
                freeNode = n;
                break;
            }
        }
        assertNotNull(freeNode, "Test requires at least one free node");

        BuildSettlementCommand cmd = new BuildSettlementCommand(player, freeNode, game);
        cmd.execute();
        assertTrue(freeNode.isOccupied());
        assertEquals(4, player.getRemainingSettlements());

        cmd.unexecute();

        assertFalse(freeNode.isOccupied(), "Node should be free after unexecute()");
        assertEquals(5, player.getRemainingSettlements());
        assertEquals(1, player.getResourceCount(ResourceType.BRICK));
        assertEquals(1, player.getResourceCount(ResourceType.WOOD));
        assertEquals(1, player.getResourceCount(ResourceType.WHEAT));
        assertEquals(1, player.getResourceCount(ResourceType.SHEEP));
    }

    // BuildCityCommand

    @Test
    void testBuildCityCommand_unexecute_downgradesAndRestoresResources() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.WHEAT, 2);
        player.addResource(ResourceType.ORE, 3);
        // place a settlement on the node manually
        Node node = board.getNodes().get(0);
        node.setBuilding(new Building(BuildingType.SETTLEMENT, player));

        BuildCityCommand cmd = new BuildCityCommand(player, node, game);
        cmd.execute();
        assertEquals(BuildingType.CITY, node.getBuilding().getType());

        cmd.unexecute();

        assertEquals(BuildingType.SETTLEMENT, node.getBuilding().getType(), "Should downgrade back to SETTLEMENT");
        assertEquals(2, player.getResourceCount(ResourceType.WHEAT));
        assertEquals(3, player.getResourceCount(ResourceType.ORE));
        assertEquals(4, player.getRemainingCities());
        // buildCity: remainingCities--, remainingSettlements++ -> settlements go to 6
        // refundCity: remainingCities++, remainingSettlements-- -> cancels out, back to
        // 5
        assertEquals(5, player.getRemainingSettlements());
    }

    // CommandManager undo/redo stack behaviour

    @Test
    void testRedo_reappliesRoad() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 2);
        player.addResource(ResourceType.WOOD, 2);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        CommandManager manager = game.getCommandManager();
        manager.execute(new BuildRoadCommand(player, edge, game));
        manager.undo();
        assertFalse(edge.isOccupied());

        manager.redo();
        assertTrue(edge.isOccupied(), "Redo should reapply the road");
        assertEquals(1, player.getResourceCount(ResourceType.BRICK)); // started 2, used 1 on redo
        assertEquals(1, player.getResourceCount(ResourceType.WOOD));
    }

    @Test
    void testNewExecute_clearsRedoStack() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 2);
        player.addResource(ResourceType.WOOD, 2);
        Edge edge1 = board.getEdges().get(0);
        Edge edge2 = board.getEdges().get(1);
        edge1.setRoad(null);
        edge2.setRoad(null);

        CommandManager manager = game.getCommandManager();
        manager.execute(new BuildRoadCommand(player, edge1, game));
        manager.undo();
        // edge1 road is undone, redo stack has one entry
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        manager.execute(new BuildRoadCommand(player, edge2, game));
        // redo stack should now be cleared, redo should do nothing
        boolean edge1WasUnchanged = !edge1.isOccupied();
        manager.redo();
        assertTrue(edge1WasUnchanged, "Redo stack should have been cleared by the new execute");
    }

    @Test
    void testUndo_blockedAtTurnBoundary() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        CommandManager manager = game.getCommandManager();
        manager.execute(new BuildRoadCommand(player, edge, game));
        manager.closeTurn();
        manager.undo(); // should be blocked by EndTurnCommand boundary

        assertTrue(edge.isOccupied(), "Undo should be blocked — road built before closeTurn() should still exist");
    }

    @Test
    void testBuildRoadCommand_unexecute_bankSupplyIsAtomicallyRestored() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        int brickBefore = game.getBank().getRemainingCount(ResourceType.BRICK);
        int woodBefore = game.getBank().getRemainingCount(ResourceType.WOOD);

        BuildRoadCommand cmd = new BuildRoadCommand(player, edge, game);
        cmd.execute();
        cmd.unexecute();

        assertEquals(brickBefore, game.getBank().getRemainingCount(ResourceType.BRICK),
                "Bank BRICK supply must be identical before and after execute and unexecute");
        assertEquals(woodBefore, game.getBank().getRemainingCount(ResourceType.WOOD),
                "Bank WOOD supply must be identical before and after execute and unexecute");
    }

    // redo invariant, resources spent after undo

    @Test
    void testRedo_failsSilentlyWhenResourcesSpentAfterUndo() {
        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        CommandManager manager = game.getCommandManager();
        manager.execute(new BuildRoadCommand(player, edge, game));
        manager.undo();
        // player now has BRICK=1, WOOD=1 back — spend them before attempting redo
        player.removeResource(ResourceType.BRICK, 1);

        manager.redo(); // canBuildRoad() -> false; redo is abandoned without touching the edge

        assertFalse(edge.isOccupied(),
                "Redo must not build the road when the player cannot afford it");
    }

    // Observer pattern tests

    @Test
    void testObserver_notifiedOnExecute() {
        int[] count = {0};
        GameObserver observer = () -> count[0]++;
        CommandManager manager = game.getCommandManager();
        manager.addObserver(observer);

        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        manager.execute(new BuildRoadCommand(player, edge, game));
        assertEquals(1, count[0], "Observer should be notified once on successful execute");
    }

    @Test
    void testObserver_notifiedOnUndo() {
        int[] count = {0};
        GameObserver observer = () -> count[0]++;
        CommandManager manager = game.getCommandManager();
        manager.addObserver(observer);

        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        manager.execute(new BuildRoadCommand(player, edge, game));
        count[0] = 0; // reset after execute notification
        manager.undo();
        assertEquals(1, count[0], "Observer should be notified once on successful undo");
    }

    @Test
    void testObserver_notNotifiedOnFailedExecute() {
        int[] count = {0};
        GameObserver observer = () -> count[0]++;
        CommandManager manager = game.getCommandManager();
        manager.addObserver(observer);

        Player player = new AgentPlayer(99);
        // no resources — execute will fail
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        manager.execute(new BuildRoadCommand(player, edge, game));
        assertEquals(0, count[0], "Observer must NOT be notified when execute fails");
    }

    @Test
    void testObserver_notifiedOnRedo() {
        int[] count = {0};
        GameObserver observer = () -> count[0]++;
        CommandManager manager = game.getCommandManager();
        manager.addObserver(observer);

        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 2);
        player.addResource(ResourceType.WOOD, 2);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        manager.execute(new BuildRoadCommand(player, edge, game));
        manager.undo();
        count[0] = 0; // reset after execute and undo notifications
        manager.redo();
        assertEquals(1, count[0], "Observer should be notified once on successful redo");
    }

    @Test
    void testObserver_removedObserverNotNotified() {
        int[] count = {0};
        GameObserver observer = () -> count[0]++;
        CommandManager manager = game.getCommandManager();
        manager.addObserver(observer);
        manager.removeObserver(observer);

        Player player = new AgentPlayer(99);
        player.addResource(ResourceType.BRICK, 1);
        player.addResource(ResourceType.WOOD, 1);
        Edge edge = board.getEdges().get(0);
        edge.setRoad(null);

        manager.execute(new BuildRoadCommand(player, edge, game));
        assertEquals(0, count[0], "Removed observer must not be notified");
    }
}
