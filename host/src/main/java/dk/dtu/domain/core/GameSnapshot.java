package dk.dtu.domain.core;

import dk.dtu.domain.model.Board;
import dk.dtu.domain.model.Robot;

import java.util.List;

// Author(s) Weihao Mo

public record GameSnapshot(GameID gameID, int round, Phase phase, int registerIndex, Board board, List<Robot> robots) {
}
