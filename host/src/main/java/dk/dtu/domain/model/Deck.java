package dk.dtu.domain.model;

import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;

import java.util.*;
import java.util.function.Supplier;

/**
 * @author Lizette Bloch Dahl Nikolajsen
 * @author Kajsa Alice Ulrika Berlstedt
 * @author William Pii Jæger
 * @author Benjamin Benyo Endahl Hansen
 * @author Niklas Emil Lysdal
 * @author Karl Johannes Agerbo
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 */
public class Deck {
    private Deque<ProgramCard> drawPile = new ArrayDeque<>();
    private List<ProgramCard> discardPile = new ArrayList<>();
    private List<ProgramCard> hand = new ArrayList<>(9);
    private DamageDecks dDecks;

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    public Deck(DamageDecks dDecks) {
        // Use the standard 20-card RoboRally deck
        this.dDecks = dDecks;
        List<ProgramCard> startingCards = buildStandardDeck();
        drawPile.addAll(startingCards);
    }

    /**
     * @author Karl Johannes Agerbo
     */
    public Deck(Deque<ProgramCard> drawPile, List<ProgramCard> discardPile, List<ProgramCard> hand,DamageDecks dDecks) {
        this.drawPile = drawPile;
        this.discardPile = discardPile;
        this.hand = hand;
        this.dDecks = dDecks;
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    public void draw() {
        if (drawPile.isEmpty()) reshuffle();
        ProgramCard drawCard = drawPile.pop();
        hand.add(drawCard);
    }

    /**
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public ProgramCard popTop(){
        return drawPile.pop();
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public void dealHand(int count) {
        int size = discardHand();
        for (int i = 0; i < count-size; i++) {
            draw();
        }
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public int discardHand() {
        List<ProgramCard> damageCards = new ArrayList<>();
        List<ProgramCard> regularCards = new ArrayList<>();

        for (ProgramCard card : hand) {
            if (isDamageCard(card)) {
                damageCards.add(card);
            } else {
                regularCards.add(card);
            }
        }

        for (ProgramCard card : regularCards) {
            discard(card);
        }

        hand.clear();
        hand.addAll(damageCards);

        return damageCards.size();
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public void discard(ProgramCard card) {
        if (isDamageCard(card)){
            dDecks.putBack(card);
        } else {
            discardPile.add(card);
        }
    }

    /**
     * @author Weihao Mo
     */
    public void addToDiscard(ProgramCard card) {
        discardPile.add(card);
    }

    /**
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public boolean isDamageCard(ProgramCard card) {
        return (card.toOp() instanceof  ProgramOP.Spam()
                || card.toOp() instanceof ProgramOP.TrojanHorse()
                || card.toOp() instanceof ProgramOP.Worm());
    }

    /**
     * @author William Pii Jæger
     * @author Bjarke Søderhamn Petersen
     * @author Niklas Emil Lysdal
     */
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


    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    private void reshuffle() {
        Collections.shuffle(discardPile);
        drawPile.addAll(discardPile);
        discardPile.clear();
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    private static void add(List<ProgramCard> list, Supplier<ProgramCard> supplier, int n) {
        for (int i = 0; i < n; i++) {
            list.add(supplier.get());
        }
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    public static List<ProgramCard> buildStandardDeck() {
        List<ProgramCard> cards = new ArrayList<>(24);
        add(cards, ProgramCard::move1, 4);
        add(cards, ProgramCard::move2, 3);
        add(cards, ProgramCard::move3, 1);
        add(cards, ProgramCard::back1, 1);
        add(cards, ProgramCard::again, 1);
        add(cards, ProgramCard::left, 4);
        add(cards, ProgramCard::right, 4);
        add(cards, ProgramCard::uturn, 1);
        add(cards, ProgramCard::sandbox, 1);
        add(cards, ProgramCard::weasel, 1);
        add(cards, ProgramCard::speed, 1);

        Collections.shuffle(cards);
        return cards;
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    public ArrayList<ProgramCard> getHand() {
        return new ArrayList<>(hand);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public Deque<ProgramCard> getDrawPile() {
        return new ArrayDeque<>(drawPile);
    }

    /**
     * @author Bjarke Søderhamn Petersen
     * @author Benjamin Benyo Endahl Hansen
     * @author Karl Johannes Agerbo
     */
    public ArrayList<ProgramCard> getDiscardPile() {
        return new ArrayList<>(discardPile);
    }

    /**
     * @author Weihao Mo
     */
    public void removeFromHand(ProgramCard card) {
        hand.remove(card);
    }
}
