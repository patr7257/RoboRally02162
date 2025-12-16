package dk.dtu.domain.core;


/**
 * Observer interface for receiving notifications about game state changes.
 * <p>
 * Observers are notified of:
 * <ul>
 *   <li>Game state updates</li>
 *   <li>Winner declaration</li>
 * </ul>
 * </p>
 * @see Game#addObserver(GameObserver)
 * @see Game#removeObserver(GameObserver)
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Niklas Emil Lysdal
 * @author William Pii Jæger
 */
public interface GameObserver {
    void onWinnerDeclared(Game game, int winner);
    void onGameUpdate(Game game);
}
