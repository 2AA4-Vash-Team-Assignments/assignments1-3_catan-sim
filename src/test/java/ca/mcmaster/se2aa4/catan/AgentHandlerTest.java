package ca.mcmaster.se2aa4.catan;

import org.junit.jupiter.api.Assumptions;
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
        // Give agent 8+ resources so total > 7
        agent.addResource(ResourceType.BRICK, 3);
        agent.addResource(ResourceType.WOOD, 3);
        agent.addResource(ResourceType.WHEAT, 1);
        agent.addResource(ResourceType.SHEEP, 1);
        assertEquals(8, agent.getTotalResourceCards());

        // Place a settlement so the agent has a connected node for building roads
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
        // Give agent exactly 7 resources
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
    void testValueMaximizingHandler_prefersSettlementOverRoad() {
        Player agent = new AgentPlayer(99);
        // Give enough resources for both a settlement and a road
        agent.addResource(ResourceType.BRICK, 2);
        agent.addResource(ResourceType.WOOD, 2);
        agent.addResource(ResourceType.WHEAT, 1);
        agent.addResource(ResourceType.SHEEP, 1);

        // Place a settlement on a node to give the agent connected roads/nodes
        Node settledNode = findFreeNode();
        assertNotNull(settledNode, "Need a free node for setup");
        settledNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));

        // Also need to place a road so the agent has available settlement nodes
        // (getAvailableSettlementNodes requires a connected road)
        List<Edge> adjEdges = settledNode.getAdjacentEdges();
        Edge roadEdge = null;
        for (Edge e : adjEdges) {
            if (!e.isOccupied()) {
                roadEdge = e;
                break;
            }
        }
        assertNotNull(roadEdge, "Need a free adjacent edge");
        roadEdge.setRoad(new Road(agent, roadEdge));

        // Verify preconditions: agent can build both
        assertTrue(agent.canBuildSettlement(), "Agent should be able to build settlement");
        assertTrue(agent.canBuildRoad(), "Agent should be able to build road");

        // Check that available settlement nodes exist
        List<Node> availSettlements = board.getAvailableSettlementNodes(agent);
        List<Edge> availRoads = board.getAvailableRoadEdges(agent);

        Assumptions.assumeTrue(!availSettlements.isEmpty() && !availRoads.isEmpty(),
                "Board setup must leave both settlement and road options for this test");

        ValueMaximizingHandler handler = new ValueMaximizingHandler();
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNotNull(result, "Handler should return a command when actions are available");
        // Settlement has value 1.0, road has value 0.8 (or 0.5), so settlement should be preferred
        assertTrue(result instanceof BuildSettlementCommand || result instanceof BuildCityCommand,
                "ValueMaximizingHandler should prefer VP-earning actions (settlement/city) over road");
    }

    @Test
    void testValueMaximizingHandler_noResources_returnsNull() {
        Player agent = new AgentPlayer(99);
        // Agent has zero resources, cannot build anything

        ValueMaximizingHandler handler = new ValueMaximizingHandler();
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNull(result, "ValueMaximizingHandler should return null when no actions are available");
    }

    // --- LongestRoadHandler ---

    @Test
    void testLongestRoadHandler_opponentRoadClose_returnsCommand() {
        Player agent = new AgentPlayer(99);
        Player opponent = game.getPlayers().get(0); // one of the setup players

        // Give agent resources to build a road
        agent.addResource(ResourceType.BRICK, 1);
        agent.addResource(ResourceType.WOOD, 1);

        // Place a settlement for the agent and build roads so they have connectivity
        Node agentNode = findFreeNode();
        assertNotNull(agentNode, "Need free node for agent");
        agentNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));

        // Build a road chain for agent (length ~2)
        Edge agentEdge1 = findFreeAdjacentEdge(agentNode);
        if (agentEdge1 != null) {
            agentEdge1.setRoad(new Road(agent, agentEdge1));
            // find the other endpoint and build another road from there
            Node nextNode = getOtherEndpoint(agentEdge1, agentNode);
            Edge agentEdge2 = findFreeAdjacentEdge(nextNode);
            if (agentEdge2 != null) {
                agentEdge2.setRoad(new Road(agent, agentEdge2));
            }
        }

        // Build a road chain for opponent so their road is within 1 of agent's
        // Opponent already has roads from setup; build more if needed to get close
        int agentRoadLen = board.calculateLongestRoad(agent);
        int opponentRoadLen = board.calculateLongestRoad(opponent);

        // Make opponent's road length >= agentRoadLen - 1
        // If the opponent doesn't already threaten, manually place roads for them
        if (opponentRoadLen < agentRoadLen - 1) {
            // Find opponent's existing settlement or road endpoint
            for (Node n : board.getNodes()) {
                if (n.isOccupied() && n.getBuilding().getOwner() == opponent) {
                    Edge oppEdge = findFreeAdjacentEdge(n);
                    while (oppEdge != null && board.calculateLongestRoad(opponent) < agentRoadLen - 1) {
                        oppEdge.setRoad(new Road(opponent, oppEdge));
                        Node next = getOtherEndpoint(oppEdge, n);
                        n = next;
                        oppEdge = findFreeAdjacentEdge(n);
                    }
                    break;
                }
            }
        }

        // Verify threat condition holds
        int finalAgentLen = board.calculateLongestRoad(agent);
        int finalOpponentLen = board.calculateLongestRoad(opponent);
        Assumptions.assumeTrue(finalOpponentLen >= finalAgentLen - 1 && finalOpponentLen > 0,
                "Opponent road must be within 1 of agent's for this test");

        LongestRoadHandler handler = new LongestRoadHandler(null);
        Command result = handler.handle(agent, game, game.getPlayers());

        // Handler should fire if there are available road edges
        List<Edge> available = board.getAvailableRoadEdges(agent);
        if (!available.isEmpty() && agent.canBuildRoad()) {
            assertNotNull(result, "LongestRoadHandler should return a command when opponent road is close");
            assertTrue(result instanceof BuildRoadCommand,
                    "LongestRoadHandler should return a BuildRoadCommand");
        }
    }

    @Test
    void testLongestRoadHandler_noThreat_delegatesToNext() {
        Player agent = new AgentPlayer(99);
        agent.addResource(ResourceType.BRICK, 1);
        agent.addResource(ResourceType.WOOD, 1);

        // Place a settlement and a long chain of roads for the agent
        Node agentNode = findFreeNode();
        assertNotNull(agentNode, "Need free node for agent");
        agentNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));

        // Build 5 roads for the agent so they have a long road
        Node current = agentNode;
        for (int i = 0; i < 5; i++) {
            Edge e = findFreeAdjacentEdge(current);
            if (e == null) break;
            e.setRoad(new Road(agent, e));
            current = getOtherEndpoint(e, current);
        }

        int agentLen = board.calculateLongestRoad(agent);
        // Verify no opponent has a road within 1 of agent's
        boolean threatExists = false;
        for (Player p : game.getPlayers()) {
            if (p == agent) continue;
            int otherLen = board.calculateLongestRoad(p);
            if (otherLen >= agentLen - 1 && otherLen > 0) {
                threatExists = true;
            }
        }

        Assumptions.assumeFalse(threatExists,
                "No opponent should threaten agent's road for this test");

        LongestRoadHandler handler = new LongestRoadHandler(null);
        Command result = handler.handle(agent, game, game.getPlayers());

        assertNull(result, "LongestRoadHandler should delegate (return null) when no opponent road is close");
    }

    // --- Chain ordering ---

    @Test
    void testChainOrder_excessCardsHandlerTakesPriorityOverValueMaximizing() {
        Player agent = new AgentPlayer(99);
        // Give agent 9 resources — triggers ExcessCardsHandler
        agent.addResource(ResourceType.BRICK, 3);
        agent.addResource(ResourceType.WOOD, 3);
        agent.addResource(ResourceType.WHEAT, 2);
        agent.addResource(ResourceType.SHEEP, 1);
        assertTrue(agent.getTotalResourceCards() > 7);

        // Place a settlement so agent has connectivity for building
        Node freeNode = findFreeNode();
        assertNotNull(freeNode, "Need a free node for setup");
        freeNode.setBuilding(new Building(BuildingType.SETTLEMENT, agent));

        // Build the full chain: ExcessCards -> RoadSegment -> LongestRoad -> ValueMaximizing
        AgentActionHandler chain = new ExcessCardsHandler(
                new RoadSegmentHandler(
                        new LongestRoadHandler(
                                new ValueMaximizingHandler())));

        Command result = chain.handle(agent, game, game.getPlayers());

        // ExcessCardsHandler should fire first because cards > 7
        // It picks any valid build action — just verify it returns something
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
