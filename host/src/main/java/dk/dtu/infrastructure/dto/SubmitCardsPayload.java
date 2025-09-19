package dk.dtu.infrastructure.dto;

import dk.dtu.domain.program.ProgramCard;

import java.util.ArrayList;
import java.util.List;

// Author(s) Weihao Mo

public record SubmitCardsPayload(String type, List<String> cards) {
    public List<ProgramCard> revertStringToCard(List<String> cards) {
        List<ProgramCard> revertCards = new ArrayList<>();

        for(String c: cards) {
            switch (c) {
                case "MOVE1" -> revertCards.add(ProgramCard.move1());
                case "MOVE2" -> revertCards.add(ProgramCard.move2());
                case "MOVE3" -> revertCards.add(ProgramCard.move3());
                case "MOVEBACK" -> revertCards.add(ProgramCard.back1());
                case "ROTATERIGHT" -> revertCards.add(ProgramCard.right());
                case "ROTATELEFT" -> revertCards.add(ProgramCard.left());
                case "UTURN" -> revertCards.add(ProgramCard.uturn());
            };
        }
        return revertCards;
    }
}
