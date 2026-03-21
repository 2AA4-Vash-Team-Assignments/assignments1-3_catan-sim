# Assignment 3 — Catan Simulator

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=2AA4-Vash-Team-Assignments_assignment1-catan-sim&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=2AA4-Vash-Team-Assignments_assignment1-catan-sim)

SFWRENG 2AA4 — Software Design I (McMaster University, Winter 2026)

## Overview

This project implements a simplified Settlers of Catan board game simulator. Four players (three computer agents and one optional human) play on a standard 19-tile hex board, collecting resources, building roads, settlements, and cities, and competing to be the first to reach 10 victory points.

A3 extends the simulator with three design patterns: the **Command pattern** for undo/redo functionality (R3.1), the **Chain of Responsibility pattern** for rule-based machine intelligence (R3.2, R3.3), and the **Observer pattern** for game state notifications.

## Requirements

- Java 21 (JDK)
- Apache Maven 3.9+

## Building and Running

Compile the project:
```bash
mvn compile
```

Run the simulator with default settings (50 rounds, all agents):
```bash
mvn exec:java
```

Run with a configuration file:
```bash
mvn exec:java -Dexec.args="config.txt"
```
On Windows PowerShell, use: `mvn exec:java "-Dexec.args=config.txt"`

### Configuration

The simulator accepts a configuration file with the following format:
```
turns=100
human=1
```
- `turns` — number of rounds (1–8192). Defaults to 50.
- `human` — player ID (1–4) to control as human. Omit for all-agent mode.

### Human Commands

When a human player is configured, the following commands are available during the human's turn:

| Command | Description |
|---------|-------------|
| `Roll` | Roll the dice and collect resources |
| `Go` | End turn (proceed to next player) |
| `List` | Show cards currently in hand |
| `Build settlement N` | Build a settlement at node N |
| `Build city N` | Upgrade settlement at node N to a city |
| `Build road N1,N2` | Build a road between nodes N1 and N2 |
| `Undo` | Undo the last build action |
| `Redo` | Redo a previously undone action |

Between agent turns, the game waits for `Go` to step forward (R2.4).

### Visualizer

In human mode, game state is written to `state.json` after each turn. The instructor's Catanatron-based Python visualizer reads `base_map.json` and `state.json` to render the board. See the [visualizer documentation](https://github.com/ssm-lab/2aa4-2026-base/tree/main/assignments/visualize).

## Running Tests

```bash
mvn test
```

89 unit tests across 12 test classes, organized in a JUnit 5 test suite (`CatanTestSuite`).

## Project Structure

```
assignment1-catan-sim/
├── model/                                     # Design artifacts
│   ├── catan-domain-model.mmd                 #   A1 domain model (Mermaid)
│   ├── catan-domain-model-a2.mmd              #   A2 extended domain model
│   ├── catan-domain-model-A3Task1.mmd         #   A3 domain model (all 3 patterns)
│   ├── agent-turn-automaton.mmd               #   Turn automaton (state machine)
│   └── DESIGN-A2.md                           #   A2 design documentation
├── src/main/java/ca/mcmaster/se2aa4/catan/
│   ├── CatanGame.java              # Game engine and orchestration
│   ├── Board.java                  # Hex board topology (19 tiles, 54 nodes, 72 edges)
│   ├── Tile.java                   # Hexagonal land tile
│   ├── Node.java                   # Intersection (settlement/city location)
│   ├── Edge.java                   # Path between nodes (road location)
│   ├── Player.java                 # Abstract base for all player types
│   ├── AgentPlayer.java            # Rule-based AI agent (Chain of Responsibility)
│   ├── HumanPlayer.java            # Human-controlled player (console input)
│   ├── Command.java                # Command interface (undo/redo, R3.1)
│   ├── CommandManager.java         # Undo/redo stacks + Observer subject
│   ├── BuildSettlementCommand.java # Reversible settlement build
│   ├── BuildCityCommand.java       # Reversible city upgrade
│   ├── BuildRoadCommand.java       # Reversible road build
│   ├── EndTurnCommand.java         # Turn boundary marker
│   ├── AgentActionHandler.java     # Abstract handler (Chain of Responsibility)
│   ├── ExcessCardsHandler.java     # R3.3: spend when >7 cards
│   ├── RoadSegmentHandler.java     # R3.3: connect nearby road segments
│   ├── LongestRoadHandler.java     # R3.3: defend longest road
│   ├── ValueMaximizingHandler.java # R3.2: value-based action selection
│   ├── GameObserver.java           # Observer interface (Task 3)
│   ├── ConsoleLogObserver.java     # Concrete observer
│   ├── HumanInputReader.java       # Interface for reading input (DIP)
│   ├── ConsoleInputReader.java     # Console implementation
│   ├── CommandParser.java          # Regex-based command parser
│   ├── CommandType.java            # Enum: ROLL, GO, LIST, BUILD_*, UNDO, REDO
│   ├── ParsedCommand.java          # Parsed command value object
│   ├── Building.java               # Settlement or city
│   ├── Road.java                   # Road placed on an edge
│   ├── Bank.java                   # Resource supply manager
│   ├── Dice.java                   # Two six-sided dice
│   ├── Robber.java                 # Robber entity
│   ├── RobberHandler.java          # Robber sequence logic
│   ├── TurnPhase.java              # Enum: 8-state turn automaton
│   ├── GameStateWriter.java        # JSON state serializer
│   ├── Configuration.java          # Config file parser
│   ├── Demonstrator.java           # Entry point (static void main)
│   ├── ResourceType.java           # Enum: WOOD, BRICK, WHEAT, ORE, SHEEP
│   └── BuildingType.java           # Enum: SETTLEMENT, CITY
├── src/test/java/ca/mcmaster/se2aa4/catan/   # Tests (89 tests, 12 classes)
├── base_map.json               # Tile layout for visualizer
├── pom.xml                     # Maven build configuration
└── README.md
```

## Output Format

The simulator prints actions in the following format:
```
[RoundNumber] / P[PlayerID]: [Action]
```

Victory points are printed at the end of each round and a summary is displayed when the game ends.
