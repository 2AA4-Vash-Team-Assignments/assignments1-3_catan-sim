package ca.mcmaster.se2aa4.catan;

/**
 * Demonstrator program for the Catan simulator (R2.6).
 *
 * Demonstrates key A2 functionality:
 *  - 4-player game with configurable rounds (config file or defaults)
 *  - Human player mode: when config contains "human=N", player N is
 *    controlled via console commands (Roll, Go, List, Build). The other
 *    three players remain automated random agents.
 *  - Visualizer integration: in human mode, game state is written to
 *    state.json after each turn so the instructor's Python visualizer
 *    (light_visualizer.py) can render the board.
 *  - Robber mechanism: rolling a 7 triggers discard (>7 cards), random
 *    robber placement, and stealing from a qualifying player (R2.5).
 *  - Step-forward: between agent turns, the game waits for a "go" command
 *    so the human can follow along at their own pace (R2.4).
 *
 * Usage:
 *   mvn exec:java                         (default 50 rounds, all agents)
 *   mvn exec:java -Dexec.args="config.txt" (custom config)
 *
 * Config file format:
 *   turns=100
 *   human=1
 */
public class Demonstrator {

    public static void main(String[] args) {
        // Create the game — initializes board, 4 agent players, dice, bank
        CatanGame game = new CatanGame();

        // Register the console log observer for game state notifications (Observer pattern)
        game.addObserver(new ConsoleLogObserver());

        // Load configuration if a config file path was provided as an argument
        if (args.length > 0) {
            game.getConfiguration().load(args[0]);
        }

        // If a human player is configured, enable state file output for the
        // visualizer (R2.2). The visualizer reads base_map.json + state.json.
        if (game.getConfiguration().isHumanGame()) {
            game.setStateFilePath("state.json");
        }

        // Run the game — setup phase (snake draft), then rounds until
        // max rounds or a player reaches 10 VP
        game.play();
    }
}
