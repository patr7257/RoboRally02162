package dk.dtu.domain.model;

import dk.dtu.domain.program.ProgramCard;
import dk.dtu.domain.program.ProgramOP;


/**
 * @author Weihao Mo
 * @author Bjarke Søderhamn Petersen
 * @author Asger Allin Jensen
 */
public class DamageDecks {
    private int spamDrawPile;
    private int trojanHorseDrawPile;
    private int wormDrawPile;

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
     * @author Weihao Mo
     * @author Bjarke Søderhamn Petersen
     * @author Asger Allin Jensen
     */
    public void putBack(ProgramCard card) {
        switch (card.toOp()) {
            case ProgramOP.Spam():
                spamDrawPile++;
                break;
            case ProgramOP.TrojanHorse():
                trojanHorseDrawPile++;
                break;
            case ProgramOP.Worm():
                wormDrawPile++;
                break;

            default:
                break;
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