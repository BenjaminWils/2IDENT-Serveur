package jeu;

import java.util.ArrayList;

/**
 *
 * @author Benjamin
 */
public class Defausse {
    private ArrayList<ArrayList<Carte>> pile;
    private int lastIndex;
    
    public Defausse() {
        this.pile = new ArrayList();
        this.lastIndex = -1;
    }
    
    public void vider() {
        this.lastIndex = -1;
        this.pile.clear();
    }
    
    public void poserCartes(ArrayList<Carte> cartes) {
        if (cartes.size() > 0) {
            this.lastIndex++;
            this.pile.add(cartes);
        }
    }
    
    public ArrayList<Carte> getDerniersCartesPosees() {
        ArrayList<Carte> cartes = new ArrayList();
        if (this.pile.size() != 0) {
            cartes = this.pile.get(lastIndex);
        }
        return cartes;
    }
}
