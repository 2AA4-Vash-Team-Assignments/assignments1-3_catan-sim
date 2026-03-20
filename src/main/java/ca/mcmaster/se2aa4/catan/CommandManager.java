package ca.mcmaster.se2aa4.catan;

import java.util.ArrayDeque;

public class CommandManager {

    private final ArrayDeque<Command> undoStack = new ArrayDeque<>();
    private final ArrayDeque<Command> redoStack = new ArrayDeque<>();

    /**
     * Executes the command. Records it for undo only if execution succeeded. Clears
     * redo stack.
     * 
     * @param cmd command to execute
     * @author Vaishnav Yandrapalli 400572601
     */
    public void execute(Command cmd) {
        if (cmd.execute()) {
            undoStack.push(cmd);
            redoStack.clear();
        }
    }

    /**
     * Undoes the last command performed by the user iff the command is not an
     * EndTurnCommand.
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
    }

    /**
     * Redoes the last, previously undone command.
     */
    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo.");
            return;
        }
        Command cmd = redoStack.pop();
        if (cmd.execute()) {
            undoStack.push(cmd);
        }
        // if execute() returns false (resources spent since undo), redo is silently
        // abandoned.
    }

    /** ensures a turn boundary so undo cannot go into the previous turn. */
    public void closeTurn() {
        undoStack.push(new EndTurnCommand());
    }
}
