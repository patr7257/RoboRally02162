package dk.dtu.domain.model;

import dk.dtu.domain.program.ProgramCard;

import java.util.*;
import java.util.function.Supplier;

public class Deck {
    private Deque<ProgramCard> drawPile = new ArrayDeque<>();
    private List<ProgramCard> discardPile = new ArrayList<>();
    private List<ProgramCard> hand = new ArrayList<>(9);

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    public Deck() {
        // Use the standard 20-card RoboRally deck
        List<ProgramCard> startingCards = buildStandardDeck();
        drawPile.addAll(startingCards);
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    public void draw() {
        if (drawPile.isEmpty()) reshuffle();
        ProgramCard drawCard = drawPile.pop();
        hand.add(drawCard);
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    public void dealHand(int count) {
        discardHand();
        for (int i = 0; i < count; i++) {
            draw();
        }
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    public void discardHand() {
        for (ProgramCard programCard : hand) {
            discard(programCard);
        }
        hand.clear();
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    public void discard(ProgramCard card) {
        discardPile.add(card);
    }

    // Author(s): William Pii Jæger, Bjarke, Niklas
    public List<ProgramCard> validateAndCompleteOrThrow(List<ProgramCard> picked) {
        if (picked == null) throw new IllegalArgumentException("cards null");
        if (picked.size() > 5) throw new IllegalArgumentException("Play at most 5 cards");

        Map<ProgramCard, Integer> have = new HashMap<>();
        for (ProgramCard c : hand) have.merge(c, 1, Integer::sum);

        Map<ProgramCard, Integer> need = new HashMap<>();
        for (ProgramCard c : picked) need.merge(c, 1, Integer::sum);

        for (var e : need.entrySet()) {
            int haveCnt = have.getOrDefault(e.getKey(), 0);
            if (e.getValue() > haveCnt) {
                throw new IllegalArgumentException("Not enough " + e.getKey() + " in hand");
            }
        }

        Map<ProgramCard, Integer> remain = new HashMap<>(have);
        for (var e : need.entrySet()) {
            remain.put(e.getKey(), remain.get(e.getKey()) - e.getValue());
        }

        List<ProgramCard> result = new ArrayList<>(picked);
        if (result.size() < 5) {
            for (ProgramCard c : hand) {
                if (result.size() == 5) break;
                int r = remain.getOrDefault(c, 0);
                if (r > 0) {
                    result.add(c);
                    remain.put(c, r - 1);
                }
            }
        }

        if (result.size() != 5) {
            throw new IllegalStateException("Unable to complete to 5 with current hand");
        }
        return List.copyOf(result);
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    private void reshuffle() {
        Collections.shuffle(discardPile);
        drawPile.addAll(discardPile);
        discardPile.clear();
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    private static void add(List<ProgramCard> list, Supplier<ProgramCard> supplier, int n) {
        for (int i = 0; i < n; i++) {
            list.add(supplier.get());
        }
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    public static List<ProgramCard> buildStandardDeck() {
        List<ProgramCard> cards = new ArrayList<>(20);
        add(cards, ProgramCard::move1, 4);
        add(cards, ProgramCard::move2, 3);
        add(cards, ProgramCard::move3, 1);
        add(cards, ProgramCard::back1, 1);
        add(cards, ProgramCard::left, 4);
        add(cards, ProgramCard::right, 4);
        add(cards, ProgramCard::uturn, 1);

        Collections.shuffle(cards);
        return cards;
    }

    // Author(s) Kajsa Berlstedt, Lizette Nikolajsen
    public ArrayList<ProgramCard> getHand() {
        return new ArrayList<>(hand);
    }
}
