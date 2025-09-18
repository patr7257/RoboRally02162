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
            };
        }
        return revertCards;
    }
}
