package jeu;

import java.util.ArrayList;

/**
 *
 * @author Benjamin
 */
public class Defausse {
    private ArrayList<ArrayList<Carte>> pile;
    private int lastIndex;
    private String pseudoDernierPoseur;
    
    public Defausse() {
        this.pile = new ArrayList();
        this.lastIndex = -1;
    }
    
    public void vider() {
        this.lastIndex = -1;
        this.pile.clear();
    }
    
    public void poserCartes(String pseudo, ArrayList<Carte> cartes) {
        if (cartes.size() > 0) {
            this.lastIndex++;
            this.pile.add(cartes);
            this.pseudoDernierPoseur = pseudo;
        }
    }
    
    public ArrayList<Carte> getDerniersCartesPosees() {
        ArrayList<Carte> cartes = new ArrayList();
        if (this.pile.size() != 0) {
            cartes = this.pile.get(lastIndex);
        }
        return cartes;
    }
    
    public void signalerFinSession() {
        this.lastIndex++;
        ArrayList<Carte> cartes = new ArrayList();
        this.pile.add(cartes);
    }
}
