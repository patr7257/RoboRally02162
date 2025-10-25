package dk.dtu.observer;

import dk.dtu.model.Lobby;
import dk.dtu.model.LobbyUpdateReason;

public interface LobbyObserver {

    public void handleUpdate(LobbyUpdateReason reason, Lobby lobby);
}
