package ca.mcmaster.se2aa4.catan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for the Chain of Responsibility handlers (R3.2, R3.3).
 * Covers ExcessCardsHandler, ValueMaximizingHandler, LongestRoadHandler,
 * and the chain ordering logic.
 */
class AgentHandlerTest {

    private CatanGame game;
    private Board board;

    @BeforeEach
    void setUp() {
        game = new CatanGame();
        board = game.getBoard();
        board.initialize();
        game.setupPhase();
    }

    // --- ExcessCardsHandler ---

    @Test
    void testExcessCardsHandler_moreThanSevenCards_returnsCommand() {
        Player agent = new AgentPlayer(99);
        agent.addResource(ResourceType.BRICK, 3);
        agent.addResource(ResourceType.WOOD, 3);
        agent.addResource(ResourceType.WHEAT, 1);
        agent.addResource(ResourceType.SHEEP, 1);
        assertEquals(8, agent.getTotalResourceCards());

        Node freeNode = findFreeNode();
        assertNotNull(freeNode, "Need a free node for setup");
        freeNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));

        ExcessCardsHandler handler = new ExcessCardsHandler(null);
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNotNull(result, "ExcessCardsHandler should return a command when agent has >7 cards");
    }

    @Test
    void testExcessCardsHandler_sevenOrFewerCards_delegatesToNext() {
        Player agent = new AgentPlayer(99);
        agent.addResource(ResourceType.BRICK, 2);
        agent.addResource(ResourceType.WOOD, 2);
        agent.addResource(ResourceType.WHEAT, 2);
        agent.addResource(ResourceType.SHEEP, 1);
        assertEquals(7, agent.getTotalResourceCards());

        ExcessCardsHandler handler = new ExcessCardsHandler(null);
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNull(result, "ExcessCardsHandler should delegate (return null) when agent has <=7 cards");
    }

    // --- ValueMaximizingHandler ---

    @Test
    void testValueMaximizingHandler_prefersVPActionOverRoad() {
        Player agent = new AgentPlayer(99);
        // Give enough resources for a city upgrade (highest VP action)
        agent.addResource(ResourceType.WHEAT, 2);
        agent.addResource(ResourceType.ORE, 3);
        agent.addResource(ResourceType.BRICK, 1);
        agent.addResource(ResourceType.WOOD, 1);

        // Place a settlement on a node, then the handler should prefer upgrading
        // to a city (VP=1.0) over building a road (0.8)
        Node node = board.getNodes().get(0);
        node.setBuilding(new Building(BuildingType.SETTLEMENT, agent));

        ValueMaximizingHandler handler = new ValueMaximizingHandler();
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNotNull(result, "Handler should return a command when actions are available");
        assertTrue(result instanceof BuildCityCommand,
                "ValueMaximizingHandler should prefer city upgrade (VP=1.0) over road (0.8)");
    }

    @Test
    void testValueMaximizingHandler_noResources_returnsNull() {
        Player agent = new AgentPlayer(99);

        ValueMaximizingHandler handler = new ValueMaximizingHandler();
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNull(result, "ValueMaximizingHandler should return null when no actions are available");
    }

    // --- LongestRoadHandler ---

    @Test
    void testLongestRoadHandler_noResourcesToBuild_delegatesToNext() {
        // Even if the threat condition would be met, handler should delegate
        // when the agent cannot afford to build a road.
        Player agent = new AgentPlayer(99);
        // No resources — canBuildRoad() returns false

        Node agentNode = findFreeNode();
        assertNotNull(agentNode);
        agentNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));
        Edge agentEdge = findFreeAdjacentEdge(agentNode);
        assertNotNull(agentEdge);
        agentEdge.setRoad(new Road(agent, agentEdge));

        LongestRoadHandler handler = new LongestRoadHandler(null);
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNull(result, "LongestRoadHandler should delegate when agent cannot afford a road");
    }

    @Test
    void testLongestRoadHandler_withResourcesAndThreat_returnsCommand() {
        // When the agent has resources and opponents threaten, handler should fire.
        Player agent = new AgentPlayer(99);
        agent.addResource(ResourceType.BRICK, 1);
        agent.addResource(ResourceType.WOOD, 1);

        Node agentNode = findFreeNode();
        assertNotNull(agentNode);
        agentNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));
        Edge agentEdge = findFreeAdjacentEdge(agentNode);
        assertNotNull(agentEdge);
        agentEdge.setRoad(new Road(agent, agentEdge));

        // Setup players have roads of length 1-2 from setupPhase, and agent has 1,
        // so opponents are within 1 of agent's road — threat condition is met
        LongestRoadHandler handler = new LongestRoadHandler(null);
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNotNull(result, "LongestRoadHandler should return a command when opponents threaten");
        assertTrue(result instanceof BuildRoadCommand,
                "LongestRoadHandler should return a BuildRoadCommand");
    }

    // --- Chain ordering ---

    @Test
    void testChainOrder_excessCardsHandlerTakesPriorityOverValueMaximizing() {
        Player agent = new AgentPlayer(99);
        agent.addResource(ResourceType.BRICK, 3);
        agent.addResource(ResourceType.WOOD, 3);
        agent.addResource(ResourceType.WHEAT, 2);
        agent.addResource(ResourceType.SHEEP, 1);
        assertTrue(agent.getTotalResourceCards() > 7);

        Node freeNode = findFreeNode();
        assertNotNull(freeNode, "Need a free node for setup");
        freeNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));

        AgentActionHandler chain = new ExcessCardsHandler(
                new RoadSegmentHandler(
                        new LongestRoadHandler(
                                new ValueMaximizingHandler())));

        Command result = chain.handle(agent, game, game.getPlayers());

        assertNotNull(result, "Chain should return a command when agent has >7 cards "
                + "(ExcessCardsHandler should fire before ValueMaximizingHandler)");
    }

    // --- Helpers ---

    private Node findFreeNode() {
        for (Node n : board.getNodes()) {
            if (!n.isOccupied() && n.satisfiesDistanceRule()) {
                return n;
            }
        }
        return null;
    }

    private Edge findFreeAdjacentEdge(Node node) {
        for (Edge e : node.getAdjacentEdges()) {
            if (!e.isOccupied()) {
                return e;
            }
        }
        return null;
    }

    private Node getOtherEndpoint(Edge edge, Node current) {
        List<Node> endpoints = edge.getEndpoints();
        return endpoints.get(0) == current ? endpoints.get(1) : endpoints.get(0);
    }
}
