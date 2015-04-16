package jeu;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author Benjamin
 */
public class Main {
    private HashMap<String, ArrayList<Carte>> mains;
    
    public Main(HashMap<String, ArrayList<Carte>> mains) {
        this.mains = mains;
    }
    
    public ArrayList<Carte> getMainJoueur(String pseudo) {
        ArrayList<Carte> cartes = new ArrayList();
        if (this.mains.containsKey(pseudo)) {
            cartes = this.mains.get(pseudo);
        }
        return cartes;
    }
    
    public ArrayList<Carte> jouerCarte(String pseudo, Carte ca) {
        ArrayList<Carte> cartes = null;
        if (this.mains.containsKey(pseudo)) {
            cartes = this.mains.get(pseudo);
            cartes.remove(ca);
        }
        return cartes;
    }
}
