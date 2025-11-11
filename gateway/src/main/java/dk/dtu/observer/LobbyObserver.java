package dk.dtu.observer;

import dk.dtu.model.Lobby;
import dk.dtu.model.LobbyUpdateReason;
/**
 * @author Niklas Emil Lysdal
 */
public interface LobbyObserver {

    public void handleUpdate(LobbyUpdateReason reason, Lobby lobby);
}
