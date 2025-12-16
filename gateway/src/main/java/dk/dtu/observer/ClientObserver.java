package dk.dtu.observer;

import dk.dtu.dto.ClientUpdateReason;
import dk.dtu.model.Client;

/**
 * @author Niklas Emil Lysdal
 */
public interface ClientObserver {
    void handleClientUpdate(ClientUpdateReason reason, Client client);
    void handleClientNameUpdate(Client client);
}
