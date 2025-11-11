package dk.dtu.domain.core;

import java.util.UUID;

public interface GameObserver {
    void onWinnerDeclared(Game game,PlayerID winner);
    void onGameUpdate(Game game);
}
