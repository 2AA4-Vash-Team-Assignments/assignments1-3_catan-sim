package ca.mcmaster.se2aa4.catan;

/**
 * Class implements command interface as a no-op boundary marker.
 * Pushed onto the undo stack at the end of each turn so that
 * undo cannot reach back into a previous turn.
 * 
 * @author Vaishnav Yandrapalli 400572601
 */
public class EndTurnCommand implements Command {

    @Override
    public boolean execute() {
        return true; // no-op boundary marker; always succeeds
    }

    @Override
    public void unexecute() {
    }

    @Override
    public boolean isUndoable() {
        return false;
    }
}
