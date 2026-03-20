package ca.mcmaster.se2aa4.catan;

import java.util.EnumMap;
import java.util.Map;

/**
 * Abstract base for all player types in the Catan simulator.
 * Manages resources, piece counts, and build eligibility.
 * Subclasses define how the player acts during their turn (OCP).
 */
public abstract class Player {

    protected final int id;
    protected final Map<ResourceType, Integer> resources;
    protected int remainingSettlements;
    protected int remainingCities;
    protected int remainingRoads;

    protected Player(int id) {
        this.id = id;
        this.resources = new EnumMap<>(ResourceType.class);
        for (ResourceType type : ResourceType.values()) {
            resources.put(type, 0);
        }
        this.remainingSettlements = 5;
        this.remainingCities = 4;
        this.remainingRoads = 15;
    }

    /**
     * Execute this player's turn. Agent players act automatically;
     * human players read commands from the console.
     */
    public abstract void takeTurn(CatanGame game);

    public int getId() {
        return id;
    }

    public int getTotalResourceCards() {
        int total = 0;
        for (int count : resources.values()) {
            total += count;
        }
        return total;
    }

    public int getResourceCount(ResourceType type) {
        return resources.get(type);
    }

    public void addResource(ResourceType type, int amount) {
        resources.put(type, resources.get(type) + amount);
    }

    public void removeResource(ResourceType type, int amount) {
        resources.put(type, Math.max(0, resources.get(type) - amount));
    }

    public boolean canBuildRoad() {
        return remainingRoads > 0
                && getResourceCount(ResourceType.BRICK) >= 1
                && getResourceCount(ResourceType.WOOD) >= 1;
    }

    public boolean canBuildSettlement() {
        return remainingSettlements > 0
                && getResourceCount(ResourceType.BRICK) >= 1
                && getResourceCount(ResourceType.WOOD) >= 1
                && getResourceCount(ResourceType.WHEAT) >= 1
                && getResourceCount(ResourceType.SHEEP) >= 1;
    }

    public boolean canBuildCity() {
        return remainingCities > 0
                && getResourceCount(ResourceType.WHEAT) >= 2
                && getResourceCount(ResourceType.ORE) >= 3;
    }

    public void buildRoad(Edge edge, Bank bank) {
        removeResource(ResourceType.BRICK, 1);
        removeResource(ResourceType.WOOD, 1);
        bank.collectResource(ResourceType.BRICK, 1);
        bank.collectResource(ResourceType.WOOD, 1);
        Road road = new Road(this, edge);
        edge.setRoad(road);
        remainingRoads--;
    }

    public void buildSettlement(Node node, Bank bank) {
        removeResource(ResourceType.BRICK, 1);
        removeResource(ResourceType.WOOD, 1);
        removeResource(ResourceType.WHEAT, 1);
        removeResource(ResourceType.SHEEP, 1);
        bank.collectResource(ResourceType.BRICK, 1);
        bank.collectResource(ResourceType.WOOD, 1);
        bank.collectResource(ResourceType.WHEAT, 1);
        bank.collectResource(ResourceType.SHEEP, 1);
        Building settlement = new Building(BuildingType.SETTLEMENT, this);
        node.setBuilding(settlement);
        remainingSettlements--;
    }

    public void buildCity(Node node, Bank bank) {
        removeResource(ResourceType.WHEAT, 2);
        removeResource(ResourceType.ORE, 3);
        bank.collectResource(ResourceType.WHEAT, 2);
        bank.collectResource(ResourceType.ORE, 3);
        node.getBuilding().setType(BuildingType.CITY);
        remainingCities--;
        remainingSettlements++;
    }

    public void useSetupSettlement() {
        remainingSettlements--;
    }

    public void useSetupRoad() {
        remainingRoads--;
    }

    public void refundRoad() {
        remainingRoads++;
    }

    public void refundSettlement() {
        remainingSettlements++;
    }

    // reverses buildCity action: returns the city piece and consumes the settlement
    // piece that buildCity freed.
    public void refundCity() {
        remainingCities++;
        remainingSettlements--;
    }

    public int getRemainingSettlements() {
        return remainingSettlements;
    }

    public int getRemainingCities() {
        return remainingCities;
    }

    public int getRemainingRoads() {
        return remainingRoads;
    }
}
