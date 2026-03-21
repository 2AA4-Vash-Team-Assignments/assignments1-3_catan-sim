package ca.mcmaster.se2aa4.catan;

/**
 * Observer interface for the Observer design pattern (Task 3).
 * Concrete observers register with CommandManager and are notified
 * whenever game state changes via execute, undo, or redo.
 */
public interface GameObserver {
    void onGameStateChanged();
}
