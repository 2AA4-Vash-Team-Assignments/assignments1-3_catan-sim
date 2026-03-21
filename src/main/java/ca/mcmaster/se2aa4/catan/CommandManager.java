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
     * @author Vaishnav Yandrapalli 400572601
     */
    public void execute(Command cmd) {
        if (cmd.execute()) {
            undoStack.push(cmd);
            redoStack.clear();
            notifyObservers();
        }
    }

    /**
     * Undoes the last command performed by the user iff the command is not an
     * EndTurnCommand. Notifies observers after a successful undo.
     */
    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo.");
            return;
        }
        if (undoStack.peek() instanceof EndTurnCommand) {
            System.out.println("Cannot undo past the start of this turn.");
            return;
        }
        Command cmd = undoStack.pop();
        cmd.unexecute();
        redoStack.push(cmd);
        notifyObservers();
    }

    /**
     * Redoes the last, previously undone command. Notifies observers on success.
     */
    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo.");
            return;
        }
        Command cmd = redoStack.pop();
        if (cmd.execute()) {
            undoStack.push(cmd);
            notifyObservers();
        }
        // if execute() returns false (resources spent since undo), redo is silently
        // abandoned.
    }

    /** ensures a turn boundary so undo cannot go into the previous turn. */
    public void closeTurn() {
        undoStack.push(new EndTurnCommand());
    }

    /** Registers an observer to be notified on state changes. */
    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    /** Removes a previously registered observer. */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /** Notifies all registered observers that game state has changed. */
    private void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.onGameStateChanged();
        }
    }
}
