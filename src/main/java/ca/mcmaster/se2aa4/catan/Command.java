package ca.mcmaster.se2aa4.catan;

/**
 * Represents a player action that can be reversed (Command pattern, used to
 * comply with R3.1)
 * Any concrete implementation of this interface encapsulate the state needed
 * to perform and undo a single action not past the last turn made, during a
 * human players turn.
 * 
 * @author Vaishnav Yandrapalli 400572601
 */
public interface Command {
    /** true if the action was carried out, false if preconditions were not met. */
    boolean execute();

    void unexecute();

    /** Whether this command can be undone. Boundary markers return false. */
    default boolean isUndoable() {
        return true;
    }
}
