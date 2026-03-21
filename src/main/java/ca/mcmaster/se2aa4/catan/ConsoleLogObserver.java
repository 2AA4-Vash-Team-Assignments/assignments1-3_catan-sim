package ca.mcmaster.se2aa4.catan;

/**
 * Concrete observer that logs game state changes to the console.
 * Demonstrates the Observer pattern by reacting to CommandManager
 * notifications without coupling to any specific game logic.
 */
public class ConsoleLogObserver implements GameObserver {

    @Override
    public void onGameStateChanged() {
        System.out.println("[Observer] Game state updated.");
    }
}
