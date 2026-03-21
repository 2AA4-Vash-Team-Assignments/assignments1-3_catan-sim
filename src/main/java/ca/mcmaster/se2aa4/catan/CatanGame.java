package ca.mcmaster.se2aa4.catan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatanGame {

    private int currentRound;
    private int longestRoadLength;
    private Player longestRoadHolder;
    private int diceRollThisTurn;
    private TurnPhase currentTurnPhase;
    private Player currentPlayer;

    private final Board board;
    private final List<Player> players;
    private final Dice dice;
    private final Bank bank;
    private final Configuration configuration;
    private final Random random;
    private final Robber robber;
    private final RobberHandler robberHandler;
    private final CommandManager commandManager;
    private String stateFilePath;

    public CatanGame() {
        this.board = new Board();
        this.players = new ArrayList<>();
        this.dice = new Dice();
        this.bank = new Bank();
        this.configuration = new Configuration();
        this.random = new Random();
        this.robber = new Robber();
        this.robberHandler = new RobberHandler(board, players, robber, bank, random);
        this.commandManager = new CommandManager();
        this.stateFilePath = null;
        this.currentRound = 0;
        this.longestRoadLength = 4;
        this.longestRoadHolder = null;
        this.diceRollThisTurn = 0;
        this.currentTurnPhase = TurnPhase.AWAIT_ROLL;

        for (int i = 1; i <= 4; i++) {
            players.add(new AgentPlayer(i));
        }
    }

    public void setStateFilePath(String path) {
        this.stateFilePath = path;
    }

    public void play() {
        if (configuration.isHumanGame()) {
            int id = configuration.getHumanPlayerId();
            players.set(id - 1, new HumanPlayer(id, new ConsoleInputReader(), new CommandParser()));
        }
        board.initialize();
        setupPhase();
        placeRobberOnDesert();
        writeState();

        while (currentRound < configuration.getMaxRounds()) {
            currentRound++;
            if (executeRound()) {
                break;
            }
        }

        printRoundSummary();
    }

    private void placeRobberOnDesert() {
        for (Tile t : board.getTiles()) {
            if (t.getResourceType() == null) {
                robber.placeOn(t);
                break;
            }
        }
    }

    public void setupPhase() {
        for (Player player : players) {
            placeInitialSettlementAndRoad(player, false);
        }
        for (int i = players.size() - 1; i >= 0; i--) {
            placeInitialSettlementAndRoad(players.get(i), true);
        }
    }

    private void placeInitialSettlementAndRoad(Player player, boolean isSecondPlacement) {
        List<Node> availableNodes = board.getAvailableSetupNodes();
        if (availableNodes.isEmpty())
            return;

        Node chosenNode = availableNodes.get(random.nextInt(availableNodes.size()));
        Building settlement = new Building(BuildingType.SETTLEMENT, player);
        chosenNode.setBuilding(settlement);
        player.useSetupSettlement();
        System.out.println("0 / P" + player.getId() + ": Placed settlement at node " + chosenNode.getId());

        if (isSecondPlacement) {
            for (Tile tile : chosenNode.getAdjacentTiles()) {
                ResourceType resource = tile.getResourceType();
                if (resource != null && bank.hasEnoughResources(resource, 1)) {
                    bank.distributeResource(resource, 1);
                    player.addResource(resource, 1);
                    System.out.println("0 / P" + player.getId() + ": Received 1 " + resource + " (starting resources)");
                }
            }
        }

        List<Edge> adjacentEdges = chosenNode.getAdjacentEdges();
        List<Edge> freeEdges = new ArrayList<>();
        for (Edge edge : adjacentEdges) {
            if (!edge.isOccupied()) {
                freeEdges.add(edge);
            }
        }
        if (!freeEdges.isEmpty()) {
            Edge chosenEdge = freeEdges.get(random.nextInt(freeEdges.size()));
            Road road = new Road(player, chosenEdge);
            chosenEdge.setRoad(road);
            player.useSetupRoad();
            List<Node> endpoints = chosenEdge.getEndpoints();
            System.out.println("0 / P" + player.getId() + ": Placed road between nodes "
                    + endpoints.get(0).getId() + " and " + endpoints.get(1).getId());
        }
    }

    public boolean executeRound() {
        for (Player player : players) {
            diceRollThisTurn = 0;
            currentPlayer = player;
            executeTurn(player);
            commandManager.closeTurn();
            writeState();
            if (checkWinCondition()) {
                printVictoryPoints();
                return true;
            }
            waitForGo();
        }
        printVictoryPoints();
        return false;
    }

    private void printVictoryPoints() {
        StringBuilder vpLine = new StringBuilder(currentRound + " / VP:");
        for (Player player : players) {
            int vp = calculateVictoryPoints(player);
            vpLine.append(" P").append(player.getId()).append("=").append(vp);
        }
        System.out.println(vpLine);
    }

    /**
     * Delegates the turn to the player via polymorphism (OCP).
     * Resets turn phase to AWAIT_ROLL before handing off to the player.
     */
    public void executeTurn(Player player) {
        currentTurnPhase = TurnPhase.AWAIT_ROLL;
        player.takeTurn(this);
        currentTurnPhase = TurnPhase.AWAIT_GO;
    }

    /**
     * Rolls the dice, transitions to ROLL_DICE phase, and returns the value.
     * Called by both AgentPlayer and HumanPlayer during their turn.
     */
    public int rollDice() {
        diceRollThisTurn = dice.roll();
        currentTurnPhase = TurnPhase.ROLL_DICE;
        return diceRollThisTurn;
    }

    public void waitForGo() {
        if (!configuration.isHumanGame()) {
            return;
        }
        // Reuse the HumanPlayer's input reader and parser to avoid duplicate Scanners
        HumanPlayer human = null;
        for (Player p : players) {
            if (p instanceof HumanPlayer) {
                human = (HumanPlayer) p;
                break;
            }
        }
        if (human == null)
            return;
        HumanInputReader input = human.getInputReader();
        CommandParser parser = human.getCommandParser();
        while (true) {
            System.out.print("> ");
            String line = input.hasNextLine() ? input.readLine() : "";
            if (parser.parse(line).getCommandType() == CommandType.GO) {
                return;
            }
        }
    }

    public void handleRollSeven(Player roller) {
        currentTurnPhase = TurnPhase.ROBBER_DISCARD;
        robberHandler.execute(roller, currentRound);
        currentTurnPhase = TurnPhase.POST_ROLL;
    }

    public void tryBuildSettlement(Player player, int nodeId) {
        Node node = validateSettlementNode(player, nodeId);
        if (node != null) {
            player.buildSettlement(node, bank);
            System.out.println(currentRound + " / P" + player.getId() + ": Built settlement at node " + nodeId);
        }
    }

    /**
     * returns the target Node if all preconditions pass, null otherwise (with a
     * message).
     */
    public Node validateSettlementNode(Player player, int nodeId) {
        if (currentTurnPhase != TurnPhase.BUILD_OR_TRADE && currentTurnPhase != TurnPhase.POST_ROLL) {
            System.out.println("Cannot build right now (wrong phase).");
            return null;
        }
        if (nodeId < 0 || nodeId >= board.getNodes().size()) {
            System.out.println("Invalid node id.");
            return null;
        }
        Node node = board.getNodes().get(nodeId);
        if (!player.canBuildSettlement()) {
            System.out.println("Cannot build settlement (resources or pieces).");
            return null;
        }
        if (!board.getAvailableSettlementNodes(player).contains(node)) {
            System.out.println("Node not available for settlement.");
            return null;
        }
        return node;
    }

    public void tryBuildCity(Player player, int nodeId) {
        Node node = validateCityNode(player, nodeId);
        if (node != null) {
            player.buildCity(node, bank);
            System.out.println(currentRound + " / P" + player.getId() + ": Built city at node " + nodeId);
        }
    }

    /** returns the target Node if all preconditions pass */
    public Node validateCityNode(Player player, int nodeId) {
        if (currentTurnPhase != TurnPhase.BUILD_OR_TRADE && currentTurnPhase != TurnPhase.POST_ROLL) {
            System.out.println("Cannot build right now (wrong phase).");
            return null;
        }
        if (nodeId < 0 || nodeId >= board.getNodes().size()) {
            System.out.println("Invalid node id.");
            return null;
        }
        Node node = board.getNodes().get(nodeId);
        if (!player.canBuildCity()) {
            System.out.println("Cannot build city (resources or pieces).");
            return null;
        }
        if (!board.getUpgradeableNodes(player).contains(node)) {
            System.out.println("Node does not have your settlement to upgrade.");
            return null;
        }
        return node;
    }

    public void tryBuildRoad(Player player, int fromId, int toId) {
        Edge edge = validateRoadEdge(player, fromId, toId);
        if (edge != null) {
            player.buildRoad(edge, bank);
            System.out.println(
                    currentRound + " / P" + player.getId() + ": Built road between nodes " + fromId + " and " + toId);
        }
    }

    /** returns the target Edge if all preconditions pass */
    public Edge validateRoadEdge(Player player, int fromId, int toId) {
        if (currentTurnPhase != TurnPhase.BUILD_OR_TRADE && currentTurnPhase != TurnPhase.POST_ROLL) {
            System.out.println("Cannot build right now (wrong phase).");
            return null;
        }
        if (fromId < 0 || toId < 0) {
            System.out.println("Invalid edge.");
            return null;
        }
        Edge edge = findEdge(fromId, toId);
        if (edge == null) {
            System.out.println("No such edge.");
            return null;
        }
        if (!player.canBuildRoad()) {
            System.out.println("Cannot build road (resources or pieces).");
            return null;
        }
        if (!board.getAvailableRoadEdges(player).contains(edge)) {
            System.out.println("Edge not available for road.");
            return null;
        }
        return edge;
    }

    private Edge findEdge(int a, int b) {
        for (Edge e : board.getEdges()) {
            List<Node> ep = e.getEndpoints();
            if ((ep.get(0).getId() == a && ep.get(1).getId() == b)
                    || (ep.get(0).getId() == b && ep.get(1).getId() == a)) {
                return e;
            }
        }
        return null;
    }

    private void writeState() {
        if (stateFilePath == null)
            return;
        try {
            int activePlayerId = (currentPlayer != null) ? currentPlayer.getId() : 1;
            GameStateWriter writer = new GameStateWriter(board, players, robber, currentRound, activePlayerId);
            writer.write(stateFilePath);
        } catch (IOException e) {
            System.err.println("Could not write state: " + e.getMessage());
        }
    }

    public void distributeResources(int diceRoll) {
        currentTurnPhase = TurnPhase.POST_ROLL;
        Tile blocked = robber.getCurrentTile();
        List<Tile> activeTiles = board.getTilesForNumber(diceRoll);
        for (Tile tile : activeTiles) {
            if (tile == blocked)
                continue;
            ResourceType resource = tile.getResourceType();
            if (resource == null)
                continue;
            for (Node node : tile.getAdjacentNodes()) {
                if (node.isOccupied()) {
                    Building building = node.getBuilding();
                    Player owner = building.getOwner();
                    int amount = building.getResourceMultiplier();
                    if (bank.hasEnoughResources(resource, amount)) {
                        bank.distributeResource(resource, amount);
                        owner.addResource(resource, amount);
                        System.out.println(currentRound + " / P" + owner.getId()
                                + ": Received " + amount + " " + resource);
                    }
                }
            }
        }
        currentTurnPhase = TurnPhase.BUILD_OR_TRADE;
    }

    public boolean checkWinCondition() {
        for (Player player : players) {
            int vp = calculateVictoryPoints(player);
            if (vp >= 10) {
                System.out.println(currentRound + " / P" + player.getId()
                        + ": Wins with " + vp + " victory points!");
                return true;
            }
        }
        return false;
    }

    private int calculateVictoryPoints(Player player) {
        int vp = 0;
        for (Node node : board.getNodes()) {
            if (node.isOccupied() && node.getBuilding().getOwner() == player) {
                vp += node.getBuilding().getVictoryPoints();
            }
        }
        if (longestRoadHolder == player) {
            vp += 2;
        }
        return vp;
    }

    public void updateLongestRoad() {
        int bestLength = 0;
        Player bestPlayer = null;

        for (Player player : players) {
            int roadLength = board.calculateLongestRoad(player);
            if (roadLength > bestLength) {
                bestLength = roadLength;
                bestPlayer = player;
            } else if (roadLength == bestLength && player == longestRoadHolder) {
                bestPlayer = player;
            }
        }

        if (bestLength < 5) {
            bestPlayer = null;
        }

        if (bestPlayer != longestRoadHolder) {
            longestRoadHolder = bestPlayer;
            longestRoadLength = (bestPlayer != null) ? bestLength : 4;
            if (bestPlayer != null) {
                System.out.println(currentRound + " / P" + bestPlayer.getId()
                        + ": Claimed longest road (" + bestLength + ")");
            }
        } else if (bestPlayer != null) {
            longestRoadLength = bestLength;
        }
    }

    public void printRoundSummary() {
        System.out.println("=== Game Over ===");
        System.out.println("Rounds played: " + currentRound);
        for (Player player : players) {
            int vp = calculateVictoryPoints(player);
            System.out.println("P" + player.getId() + ": " + vp + " VP, "
                    + player.getTotalResourceCards() + " resource cards");
        }
        if (longestRoadHolder != null) {
            System.out.println("Longest road: P" + longestRoadHolder.getId()
                    + " (" + longestRoadLength + ")");
        }
    }

    public Board getBoard() {
        return board;
    }

    public Bank getBank() {
        return bank;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public TurnPhase getCurrentTurnPhase() {
        return currentTurnPhase;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public List<Player> getPlayers() {
        return players;
    }

    /** Registers an observer for game state change notifications (Observer pattern). */
    public void addObserver(GameObserver observer) {
        commandManager.addObserver(observer);
    }

    /** Removes a previously registered observer (Observer pattern). */
    public void removeObserver(GameObserver observer) {
        commandManager.removeObserver(observer);
    }
}
