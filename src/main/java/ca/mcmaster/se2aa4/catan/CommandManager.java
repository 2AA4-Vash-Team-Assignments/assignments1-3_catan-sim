package ca.mcmaster.se2aa4.catan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the undo/redo history for all game actions (Command pattern)
 * and acts as the subject in the Observer pattern, notifying registered
 * GameObserver instances whenever game state changes.
 */
public class CommandManager {

    private final ArrayDeque<Command> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Command> redoStack = new ArrayDeque<>();
    private final List<GameObserver> observers = new ArrayList<>();

    /**
     * Executes the command. Records it for undo only if execution succeeded. Clears
     * redo stack. Notifies observers on success.
     *
     * @param cmd command to execute
     */
    public void execute(Command cmd) {
        if (cmd.execute()) {
            undoStack.push(cmd);
            redoStack.clear();
            notifyObservers();
        }
    }

    /**
     * Undoes the last command if it is undoable (turn boundaries block undo).
     * Notifies observers after a successful undo.
     */
    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        if (!undoStack.peek().isUndoable()) {
            return;
        }
        Command cmd = undoStack.pop();
        cmd.unexecute();
        redoStack.push(cmd);
        notifyObservers();
    }

    /**
     * Redoes the last undone command. If execution fails (e.g., resources were
     * spent after the undo), the command is preserved on the redo stack so the
     * user can retry later. Notifies observers on success.
     */
    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        Command cmd = redoStack.peek();
        if (cmd.execute()) {
            redoStack.pop();
            undoStack.push(cmd);
            notifyObservers();
        }
    }

    /** Marks a turn boundary so undo cannot reach into the previous turn. */
    public void closeTurn() {
        undoStack.push(new EndTurnCommand());
    }

    /** Registers an observer to be notified on state changes. */
    public void addObserver(GameObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    /** Removes a previously registered observer. */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /** Notifies all registered observers that game state has changed. */
    private void notifyObservers() {
        for (GameObserver observer : new ArrayList<>(observers)) {
            observer.onGameStateChanged();
        }
    }
}
