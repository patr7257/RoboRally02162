package dk.dtu.domain.core;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Robot;
import dk.dtu.domain.rules.api.BoardAPI;

import java.util.*;

// Author(s) Weihao Mo

public class GameManager {
    private final Map<UUID, Game> activeGames = new HashMap<>();

    public UUID startGame(Board board, BoardAPI api, List<Robot> players) {
        UUID id = UUID.randomUUID();

        Game game = new Game(board, api, players);
        activeGames.put(id,game);
        return id;
    }

    public void endGame(UUID id) {
        activeGames.remove(id);
    }

    public Optional<Game> findByID(UUID gameID) {
        return Optional.ofNullable(activeGames.get(gameID));
    }

    public synchronized CommandResult apply(UUID id, GameCommand cmd) {
        Game game = activeGames.get(id);
        if (game == null) {
            return CommandResult.fail("Game not found");
        }

        return switch (cmd) {
            case GameCommand.SubmitPrograms submit -> {
                try {
                    Robot robot = game.getRobot(submit.player());
                    if(robot == null) {
                        yield CommandResult.fail("No robot for player" + submit.player());
                    }
                    robot.loadProgram(submit.cards());
                    yield CommandResult.ok("");
                } catch (Exception e) {
                    yield CommandResult.fail("SubmitPrograms failed: " + e.getMessage());
                }
            }
            case GameCommand.StartRound start -> {
                try {
                    game.startRound();
                    yield CommandResult.ok("");
                } catch (Exception e) {
                    yield CommandResult.fail("StartRound failed: " + e.getMessage());
                }
            }
            case GameCommand.EndGame end -> {
                activeGames.remove(id);
                yield CommandResult.ok("");
            }
        };
    }


}
