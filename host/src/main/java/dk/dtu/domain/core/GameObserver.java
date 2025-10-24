package dk.dtu.domain.core;

import java.util.UUID;

public interface GameObserver {
    void onWinnerDeclared(PlayerID winner);
    void onGameUpdate(Game game);
}
