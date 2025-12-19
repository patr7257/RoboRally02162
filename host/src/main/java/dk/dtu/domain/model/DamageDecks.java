package dk.dtu.domain.model;

import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Global pools for damage cards (Spam, Trojan Horse, Worm).
 * These are NOT in any player's program deck by default; instead:
 *  <li> When robots take damage, cards are drawn from here into that robot's discard pile.</li>
 *  <li> When a damage card is eventually played, it is returned here via {@link #putBack(ProgramCard)}.</li>
 *
 * Card conservation invariant:
 *   spamDrawPile + trojanHorseDrawPile + wormDrawPile
 *   + (all damage cards currently in any Deck)
 *   is constant for the whole Game.
 *
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 * @author William Pii Jæger
 */
public class DamageDecks {
    private int spamDrawPile;
    private int trojanHorseDrawPile;
    private int wormDrawPile;

    private final Random random = new Random();

    /**
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public DamageDecks(int spamCount, int trojanHorseCount, int wormCount) {
        this.spamDrawPile = spamCount;
        this.trojanHorseDrawPile = trojanHorseCount;
        this.wormDrawPile = wormCount;
    }

    /**
     * Draw up to {@code count} damage cards from the global pools.
     * Spam is drawn first; once Spam is empty, Trojan Horse and Worm
     * are chosen at random from the remaining pools.
     *
     * Returned cards are NOT associated with any particular player yet –
     * the caller decides which Deck to add them to.
     *
     * @param count number of damage cards requested
     * @return list of drawn damage cards (size <= count)
     *
     * @author William Pii Jæger
     */
    public List<ProgramCard> drawDamageCards(int count) {
        List<ProgramCard> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ProgramCard c = drawOne();
            if (c == null) break;
            result.add(c);
        }
        return result;
    }


    /**
     * Reduce the number of damage cards in the corresponding draw pile depending on the selected operation
     * The spam card draw pile has higher priority than others if there are still spam cards left.
     *
     * @author William Pii Jæger
     * @author Bjarke Søderhamn Petersen
     * @author Weihao Mo
     * @author Asger Allin Jensen
     */
    private ProgramCard drawOne() {
        if (spamDrawPile > 0) {
            spamDrawPile--;
            return ProgramCard.spam();
        }

        List<ProgramCard> options = new ArrayList<>(2);
        if (trojanHorseDrawPile > 0) options.add(ProgramCard.trojanHorse());
        if (wormDrawPile > 0) options.add(ProgramCard.worm());

        if (options.isEmpty()) {
            return null;
        }

        ProgramCard selected = options.get(random.nextInt(options.size()));
        ProgramOP op = selected.toOp();
        if (op instanceof ProgramOP.TrojanHorse) {
            trojanHorseDrawPile--;
        } else if (op instanceof ProgramOP.Worm) {
            wormDrawPile--;
        }
        return selected;
    }

    /**
     * Puts a played damage card back into the global pool.
     * This is the ONLY way a damage card should re-enter these piles.
     *
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public void putBack(ProgramCard card) {
        ProgramOP op = card.toOp();
        if (op instanceof ProgramOP.Spam) {
            spamDrawPile++;
        } else if (op instanceof ProgramOP.TrojanHorse) {
            trojanHorseDrawPile++;
        } else if (op instanceof ProgramOP.Worm) {
            wormDrawPile++;
        }
    }

    public int getSpamDrawPile() {
        return spamDrawPile;
    }

    public int getTrojanHorseDrawPile() {
        return trojanHorseDrawPile;
    }

    public int getWormDrawPile() {
        return wormDrawPile;
    }

    public void setSpamDrawPile(int spamDrawPile) {
        this.spamDrawPile = spamDrawPile;
    }

    public void setTrojanHorseDrawPile(int trojanHorseDrawPile) {
        this.trojanHorseDrawPile = trojanHorseDrawPile;
    }

    public void setWormDrawPile(int wormDrawPile) {
        this.wormDrawPile = wormDrawPile;
    }
}