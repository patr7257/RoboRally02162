package dk.dtu.domain.model;

import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;

import java.util.*;
import java.util.function.Supplier;

/**
 * A single robot's program deck.
 *
 * Compartments:
 *  <li> drawPile: cards to be drawn</li>
 *  <li> discardPile: normal cards discarded at end of round or by effects</li>
 *  <li> hand: cards currently visible to the player for programming.</li>
 *
 * Invariants for NORMAL cards:
 *  |drawPile(normal)| + |discardPile(normal)| + |hand(normal)| = buildStandardDeck().size()
 *
 * Damage cards (Spam / Trojan / Worm) live here while owned by the player,
 * but their global counts are tracked in {@link DamageDecks}.
 *
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
    private final DamageDecks dDecks;
    private final int originalNormalCount = buildStandardDeck().size();

    /**
     * Creates a standard RoboRally program deck (22 normal cards).
     *
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
     * Draws a single card from the deck into the hand.
     * If the draw pile is empty, the discard pile is shuffled back first.
     *
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    public void draw() {
        if (drawPile.isEmpty()) reshuffle();
        if (drawPile.isEmpty()) {
            return;
        }
        ProgramCard drawCard = drawPile.pop();
        hand.add(drawCard);
    }

    /**
     * Pops the top card from the draw pile (used by effects like Spam).
     * Does NOT add the card to hand.
     *
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public ProgramCard popTop(){
        if (drawPile.isEmpty()) {
            reshuffle();
        }
        if (drawPile.isEmpty()) {
            throw new IllegalStateException("No cards to pop from draw pile");
        }
        return drawPile.pop();
    }

    /**
     * Discards the top card from the draw pile.
     * @author Bjarke Søderhamn Petersen
     */
    public void discardTopCard() {
        if (drawPile.isEmpty()) {
            reshuffle();
        }
        if (drawPile.isEmpty()) {
            return;
        }
        ProgramCard card = popTop();
        discard(card);
    }

    /**
     * Returns the top card of the draw pile without removing it.
     *
     * @author Bjarke Søderhamn Petersen
     */
    public ProgramCard peekDrawPileTop() {
        if (drawPile.isEmpty()) {
            reshuffle();
        }
        if (drawPile.isEmpty()) {
            return null;
        }
        return drawPile.peek();
    }

    /**
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     * @author William Pii Jæger
     */
    public void dealHand(int count) {
        int damageInHand = discardHand();
        for (int i = 0; i < count - damageInHand; i++) {
            draw();
        }
    }

    /**
     * Discards all non-damage cards from hand into discardPile and keeps damage cards.
     *
     * @return number of damage cards that remain in the hand
     *
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
     * Discards a card from the player's perspective.
     * <li> Normal cards go to the discard pile.</li>
     * <li> Damage cards are returned to the global damage pools (they leave this deck).</li>
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
     * Adds a card to the discard pile WITHOUT touching DamageDecks.
     * This is used when a robot TAKES damage: cards move from global DamageDecks
     * into this deck's discard pile.
     *
     * @author Weihao Mo
     */
    public void addToDiscard(ProgramCard card) {
        discardPile.add(card);
    }

    /**
     * Returns true if the card is any damage card (Spam, Trojan Horse, Worm).
     *
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public boolean isDamageCard(ProgramCard card) {
        ProgramOP op = card.toOp();
        return (op instanceof ProgramOP.Spam
                || op instanceof ProgramOP.TrojanHorse
                || op instanceof ProgramOP.Worm);
    }

    /**
     * Validates a player's picked cards and auto-completes to 5 cards.
     *
     * Rules:
     *  <li> All picked cards must be present in the current hand (by multiset).</li>
     *  <li> At most 5 cards may be picked.</li>
     *  <li> If fewer than 5 are picked, the remaining slots are filled by drawing NEW cards
     *    from the DECK (drawPile + reshuffle from discardPile), NEVER from the unused
     *    part of the hand.</li>
     *
     * Autocompleted cards are added to the hand when drawn, so they will be discarded
     * or kept (if damage) at the end of the round like any other hand card.
     *
     * @author William Pii Jæger
     * @author Bjarke Søderhamn Petersen
     * @author Niklas Emil Lysdal
     */
    public List<ProgramCard> validateAndCompleteOrThrow(List<ProgramCard> picked) {
        if (picked == null) throw new IllegalArgumentException("cards null");
        if (picked.size() > 5) throw new IllegalArgumentException("Play at most 5 cards");

        List<ProgramCard> tempHand = new ArrayList<>(hand);
        for (ProgramCard c : picked) {
            if (!tempHand.remove(c)) {
                throw new IllegalArgumentException("Not enough " + c + " in hand");
            }
        }

        List<ProgramCard> result = new ArrayList<>(5);
        result.addAll(picked);

        while (result.size() < 5) {
            ProgramCard extra = drawForProgramAutocomplete();
            if (extra == null) {
                throw new IllegalStateException("Unable to complete to 5 with current deck");
            }
            result.add(extra);
        }

        return List.copyOf(result);
    }

    /**
     * Draws one card from drawPile (reshuffling from discardPile if needed),
     * adds it to the hand, and returns it.
     *
     * This is used exclusively by the program autocomplete logic, to guarantee
     * that autocompleted cards come from the DECK.
     *
     * @author William Pii Jæger
     */
    private ProgramCard drawForProgramAutocomplete() {
        if (drawPile.isEmpty()) {
            reshuffle();
        }
        if (drawPile.isEmpty()) {
            return null;
        }
        ProgramCard c = drawPile.pop();
        hand.add(c);
        return c;
    }

    /**
     * Shuffles the discard pile back into the draw pile.
     * Damage cards never reach discardPile (they are put back into DamageDecks),
     * so this only moves normal program cards.
     *
     * @author Lizette Bloch Dahl Nikolajsen
     * @author Kajsa Alice Ulrika Berlstedt
     */
    private void reshuffle() {
        if (discardPile.isEmpty()) return;
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
     * Removes one instance of {@code card} from the hand, if present.
     * Used when a damage card is played so that it no longer stays in the hand
     * across rounds.
     *
     * @author Weihao Mo
     */
    public void removeFromHand(ProgramCard card) {
        hand.remove(card);
    }

    /**
     * Accepts cards as-is for demo mode without validation.
     * Does not check if cards are in hand or validate counts.
     *
     * @param cards the cards to accept
     * @return the cards list, trimmed to at most 5 cards
     * @throws IllegalArgumentException if cards is null
     * @author William Pii Jæger
     */
    public List<ProgramCard> acceptCardsAsIs(List<ProgramCard> cards) {
        if (cards == null) throw new IllegalArgumentException("cards null");

        if (cards.size() > 5) {
            return List.copyOf(cards.subList(0, 5));
        }

        return List.copyOf(cards);
    }

    /**
     * Debug-only check.
     *
     * Makes sure normal program cards never "bleed":
     *   normal(drawPile) + normal(discardPile) + normal(hand) == originalNormalCount
     *
     * @author William Pii Jæger
     */
    public String debugAssertNormalConservation(String label) {
        int normalDraw = countNormal(drawPile);
        int normalDiscard = countNormal(discardPile);
        int normalHand = countNormal(hand);

        int totalNormal = normalDraw + normalDiscard + normalHand;

        int dmgDraw = countDamage(drawPile);
        int dmgDiscard = countDamage(discardPile);
        int dmgHand = countDamage(hand);
        int totalDamage = dmgDraw + dmgDiscard + dmgHand;

        String msg =
                "[DeckCheck] " + (label == null ? "" : label + " ") +
                        "normal(total=" + totalNormal + ", expected=" + originalNormalCount +
                        ", draw=" + normalDraw + ", discard=" + normalDiscard + ", hand=" + normalHand + ") " +
                        "damage(total=" + totalDamage +
                        ", draw=" + dmgDraw + ", discard=" + dmgDiscard + ", hand=" + dmgHand + ")";

        if (totalNormal != originalNormalCount) {
            throw new IllegalStateException(
                    msg + "\nNormal-card conservation violated! This means you're duplicating or deleting normal cards."
            );
        }

        if (normalHand + dmgHand != hand.size()) {
            throw new IllegalStateException(msg + "\nHand count mismatch (bug in counting?)");
        }

        return msg;
    }

    /**
     * @author William Pii Jæger
     */
    private int countNormal(Iterable<ProgramCard> cards) {
        int n = 0;
        for (ProgramCard c : cards) {
            if (!isDamageCard(c)) n++;
        }
        return n;
    }

    /**
     * @author William Pii Jæger
     */
    private int countDamage(Iterable<ProgramCard> cards) {
        int n = 0;
        for (ProgramCard c : cards) {
            if (isDamageCard(c)) n++;
        }
        return n;
    }

}
