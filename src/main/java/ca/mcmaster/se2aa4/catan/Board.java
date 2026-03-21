package ca.mcmaster.se2aa4.catan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public class Board {

    private final List<Tile> tiles;
    private final List<Node> nodes;
    private final List<Edge> edges;

    public Board() {
        this.tiles = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }

    public void initialize() {
        createNodes();
        createTiles();
        createEdges();
    }

    private void createNodes() {
        for (int i = 0; i < 54; i++) {
            nodes.add(new Node(i));
        }
    }

    private void createTiles() {
        // Tile data: id, resource, number token
        // From assignment specification
        ResourceType W = ResourceType.WOOD;
        ResourceType B = ResourceType.BRICK;
        ResourceType H = ResourceType.WHEAT;
        ResourceType O = ResourceType.ORE;
        ResourceType S = ResourceType.SHEEP;

        int[][] tileData = {
            {0, 0, 10}, {1, 2, 11}, {2, 1, 8},  {3, 3, 3},
            {4, 4, 11}, {5, 4, 5},  {6, 4, 12}, {7, 2, 3},
            {8, 3, 6},  {9, 0, 4},  {10, 3, 6}, {11, 2, 9},
            {12, 0, 5}, {13, 1, 9}, {14, 1, 8}, {15, 2, 4},
            {16, -1, 0}, {17, 0, 2}, {18, 4, 10}
        };

        ResourceType[] typeMap = {W, B, H, O, S};

        for (int[] data : tileData) {
            int id = data[0];
            int resIndex = data[1];
            int number = data[2];

            if (resIndex == -1) {
                // Desert tile - no resource, no number
                tiles.add(new Tile(id, 0, null));
            } else {
                tiles.add(new Tile(id, number, typeMap[resIndex]));
            }
        }

        // Wire up tile-node adjacency based on assignment spec hex layout
        // Vertices listed clockwise from top for each tile
        // Tile 0 (center)
        setTileNodes(0, new int[]{5, 0, 1, 2, 3, 4});

        // Inner ring (tiles 1-6)
        setTileNodes(1, new int[]{1, 6, 7, 8, 9, 2});
        setTileNodes(2, new int[]{3, 2, 9, 10, 11, 12});
        setTileNodes(3, new int[]{15, 4, 3, 12, 13, 14});
        setTileNodes(4, new int[]{18, 16, 5, 4, 15, 17});
        setTileNodes(5, new int[]{21, 19, 20, 0, 5, 16});
        setTileNodes(6, new int[]{20, 22, 23, 6, 1, 0});

        // Outer ring (tiles 7-18)
        setTileNodes(7, new int[]{7, 24, 25, 26, 27, 8});
        setTileNodes(8, new int[]{9, 8, 27, 28, 29, 10});
        setTileNodes(9, new int[]{11, 10, 29, 30, 31, 32});
        setTileNodes(10, new int[]{13, 12, 11, 32, 33, 34});
        setTileNodes(11, new int[]{37, 14, 13, 34, 35, 36});
        setTileNodes(12, new int[]{39, 17, 15, 14, 37, 38});
        setTileNodes(13, new int[]{42, 40, 18, 17, 39, 41});
        setTileNodes(14, new int[]{44, 43, 21, 16, 18, 40});
        setTileNodes(15, new int[]{45, 47, 46, 19, 21, 43});
        setTileNodes(16, new int[]{46, 48, 49, 22, 20, 19});
        setTileNodes(17, new int[]{49, 50, 51, 52, 23, 22});
        setTileNodes(18, new int[]{23, 52, 53, 24, 7, 6});

        // Also set up Node -> Tile back-references
        for (Tile tile : tiles) {
            for (Node node : tile.getAdjacentNodes()) {
                node.addAdjacentTile(tile);
            }
        }
    }

    private void setTileNodes(int tileId, int[] nodeIds) {
        Tile tile = tiles.get(tileId);
        for (int nodeId : nodeIds) {
            Node node = nodes.get(nodeId);
            tile.addAdjacentNode(node);
        }
    }

    private void createEdges() {
        // Create edges by connecting adjacent nodes on each tile
        Set<String> created = new HashSet<>();

        for (Tile tile : tiles) {
            List<Node> tileNodes = tile.getAdjacentNodes();
            for (int i = 0; i < tileNodes.size(); i++) {
                Node a = tileNodes.get(i);
                Node b = tileNodes.get((i + 1) % tileNodes.size());

                String key = Math.min(a.getId(), b.getId()) + "-" + Math.max(a.getId(), b.getId());
                if (!created.contains(key)) {
                    Edge edge = new Edge(a, b);
                    edges.add(edge);
                    a.addAdjacentNode(b);
                    b.addAdjacentNode(a);
                    created.add(key);
                }
            }
        }
    }

    public List<Tile> getTilesForNumber(int number) {
        List<Tile> result = new ArrayList<>();
        for (Tile tile : tiles) {
            if (tile.getNumberToken() == number && tile.getResourceType() != null) {
                result.add(tile);
            }
        }
        return result;
    }

    public List<Node> getAvailableSettlementNodes(Player player) {
        List<Node> available = new ArrayList<>();
        for (Node node : nodes) {
            if (!node.isOccupied() && node.satisfiesDistanceRule() && hasConnectedRoad(node, player)) {
                available.add(node);
            }
        }
        return available;
    }

    public List<Edge> getAvailableRoadEdges(Player player) {
        List<Edge> available = new ArrayList<>();
        for (Edge edge : edges) {
            if (!edge.isOccupied() && isConnectedToPlayer(edge, player)) {
                available.add(edge);
            }
        }
        return available;
    }

    public List<Node> getUpgradeableNodes(Player player) {
        List<Node> upgradeable = new ArrayList<>();
        for (Node node : nodes) {
            if (node.isOccupied()
                    && node.getBuilding().getOwner() == player
                    && node.getBuilding().getType() == BuildingType.SETTLEMENT) {
                upgradeable.add(node);
            }
        }
        return upgradeable;
    }

    public List<Node> getAvailableSetupNodes() {
        List<Node> available = new ArrayList<>();
        for (Node node : nodes) {
            if (!node.isOccupied() && node.satisfiesDistanceRule()) {
                available.add(node);
            }
        }
        return available;
    }

    public int calculateLongestRoad(Player player) {
        int longest = 0;
        for (Edge edge : edges) {
            if (edge.isOccupied() && edge.getRoad().getOwner() == player) {
                int length = dfsRoadLength(edge, player, new HashSet<>());
                longest = Math.max(longest, length);
            }
        }
        return longest;
    }

    private int dfsRoadLength(Edge current, Player player, Set<Edge> visited) {
        visited.add(current);
        int maxLength = 1;

        for (Node endpoint : current.getEndpoints()) {
            // Stop if another player has a building here (breaks the road)
            if (endpoint.isOccupied() && endpoint.getBuilding().getOwner() != player) {
                continue;
            }
            for (Edge adjacent : endpoint.getAdjacentEdges()) {
                if (!visited.contains(adjacent) && adjacent.isOccupied()
                        && adjacent.getRoad().getOwner() == player) {
                    int length = 1 + dfsRoadLength(adjacent, player, visited);
                    maxLength = Math.max(maxLength, length);
                }
            }
        }

        visited.remove(current);
        return maxLength;
    }

    private boolean hasConnectedRoad(Node node, Player player) {
        for (Edge edge : node.getAdjacentEdges()) {
            if (edge.isOccupied() && edge.getRoad().getOwner() == player) {
                return true;
            }
        }
        return false;
    }

    private boolean isConnectedToPlayer(Edge edge, Player player) {
        for (Node endpoint : edge.getEndpoints()) {
            if (endpoint.isOccupied() && endpoint.getBuilding().getOwner() == player) {
                return true;
            }
            for (Edge adj : endpoint.getAdjacentEdges()) {
                if (adj.isOccupied() && adj.getRoad().getOwner() == player) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the player has two disconnected road segments that are at most
     * 2 empty edges apart (R3.3: connect nearby segments).
     */
    public boolean hasRoadSegmentsWithinTwoUnits(Player player) {
        List<Set<Edge>> segments = getRoadSegments(player);
        if (segments.size() < 2) {
            return false;
        }
        for (int i = 0; i < segments.size(); i++) {
            for (int j = i + 1; j < segments.size(); j++) {
                if (getMinGapBetweenSegments(segments.get(i), segments.get(j), player) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the player's road network as a list of connected components (segments).
     */
    public List<Set<Edge>> getRoadSegments(Player player) {
        Set<Edge> playerEdges = collectPlayerRoadEdges(player);
        List<Set<Edge>> segments = new ArrayList<>();
        Set<Edge> visited = new HashSet<>();
        for (Edge start : playerEdges) {
            if (visited.contains(start)) {
                continue;
            }
            Set<Edge> segment = bfsExpandSegment(start, player, visited);
            segments.add(segment);
        }
        return segments;
    }

    private Set<Edge> collectPlayerRoadEdges(Player player) {
        Set<Edge> result = new HashSet<>();
        for (Edge edge : edges) {
            if (edge.isOccupied() && edge.getRoad().getOwner() == player) {
                result.add(edge);
            }
        }
        return result;
    }

    private Set<Edge> bfsExpandSegment(Edge start, Player player, Set<Edge> visited) {
        Set<Edge> segment = new HashSet<>();
        Queue<Edge> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            Edge current = queue.poll();
            if (visited.contains(current)) {
                continue;
            }
            visited.add(current);
            segment.add(current);
            for (Edge adj : getUnvisitedAdjacentPlayerEdges(current, player, visited)) {
                queue.add(adj);
            }
        }
        return segment;
    }

    private List<Edge> getUnvisitedAdjacentPlayerEdges(Edge current, Player player, Set<Edge> visited) {
        List<Edge> result = new ArrayList<>();
        for (Node endpoint : current.getEndpoints()) {
            for (Edge adj : endpoint.getAdjacentEdges()) {
                if (isPlayerRoad(adj, player) && !visited.contains(adj)) {
                    result.add(adj);
                }
            }
        }
        return result;
    }

    private boolean isPlayerRoad(Edge edge, Player player) {
        return edge.isOccupied() && edge.getRoad().getOwner() == player;
    }

    /**
     * Returns the minimum number of empty edges needed to connect two road segments.
     * Uses BFS: traverse via player's roads (cost 0) or empty edges (cost 1).
     */
    private int getMinGapBetweenSegments(Set<Edge> segA, Set<Edge> segB, Player player) {
        Set<Node> segBNodes = new HashSet<>();
        for (Edge e : segB) {
            segBNodes.addAll(e.getEndpoints());
        }
        int minGap = Integer.MAX_VALUE;
        for (Edge startEdge : segA) {
            for (Node startNode : startEdge.getEndpoints()) {
                int gap = bfsGapToSegment(startNode, segBNodes, segA, player);
                minGap = Math.min(minGap, gap);
            }
        }
        return minGap == Integer.MAX_VALUE ? Integer.MAX_VALUE : minGap;
    }

    private int bfsGapToSegment(Node start, Set<Node> targetNodes, Set<Edge> segA, Player player) {
        Set<Node> segANodes = collectSegmentNodes(segA);
        java.util.Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[] { start.getId(), 0 });
        Set<Integer> visited = new HashSet<>();
        while (!queue.isEmpty()) {
            int[] state = queue.pollFirst();
            if (visited.contains(state[0])) {
                continue;
            }
            visited.add(state[0]);
            Node current = nodes.get(state[0]);
            int cost = state[1];
            if (isReachableTarget(current, targetNodes, segANodes)) {
                return cost;
            }
            enqueueAdjacentNodes(current, state[0], cost, player, visited, queue);
        }
        return Integer.MAX_VALUE;
    }

    private Set<Node> collectSegmentNodes(Set<Edge> segment) {
        Set<Node> nodes = new HashSet<>();
        for (Edge e : segment) {
            nodes.addAll(e.getEndpoints());
        }
        return nodes;
    }

    private boolean isReachableTarget(Node current, Set<Node> targetNodes, Set<Node> segANodes) {
        return targetNodes.contains(current) && !segANodes.contains(current);
    }

    private void enqueueAdjacentNodes(Node current, int currentId, int cost, Player player,
            Set<Integer> visited, java.util.Deque<int[]> queue) {
        for (Edge adj : current.getAdjacentEdges()) {
            for (Node next : adj.getEndpoints()) {
                if (next.getId() == currentId || visited.contains(next.getId())) {
                    continue;
                }
                int edgeCost = isPlayerRoad(adj, player) ? 0 : 1;
                int[] nextState = new int[] { next.getId(), cost + edgeCost };
                if (edgeCost == 0) {
                    queue.addFirst(nextState);
                } else {
                    queue.addLast(nextState);
                }
            }
        }
    }

    public List<Tile> getTiles() {
        return tiles;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
