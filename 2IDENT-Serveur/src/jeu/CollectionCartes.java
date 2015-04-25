package jeu;

import java.util.ArrayList;
import java.util.Iterator;
import org.json.simple.JSONArray;

/**
 *
 * @author Benjamin
 */
public class CollectionCartes {
    public ArrayList<Carte> cartes;
    public ArrayList<Carte> cartesRestantes;
    public ArrayList<Carte> cartesDistribuees;
    
    /*
    Correspondance hauteur : numéro
    AS : 14
    ROI : 13
    DAME : 12
    VALET : 11
    2 : 2
    */
    
    public CollectionCartes() {
        this.cartes = new ArrayList<Carte>();
        this.cartesRestantes = new ArrayList<Carte>();
        this.cartesDistribuees = new ArrayList<Carte>();
        
        this.initialisationDuJeu();
    }
    
    public void initialisationDuJeu() {
        this.cartes.clear();
        this.cartesDistribuees.clear();
        this.cartesRestantes.clear();
        
        Carte c;
        c = new Carte("14","CO");
        this.cartes.add(c);
        c = new Carte("14","PI");
        this.cartes.add(c);
        c = new Carte("14","TR");
        this.cartes.add(c);
        c = new Carte("14","CA");
        this.cartes.add(c);
        c = new Carte("13","CO");
        this.cartes.add(c);
        c = new Carte("13","PI");
        this.cartes.add(c);
        c = new Carte("13","TR");
        this.cartes.add(c);
        c = new Carte("13","CA");
        this.cartes.add(c);
        c = new Carte("12","CO");
        this.cartes.add(c);
        c = new Carte("12","PI");
        this.cartes.add(c);
        c = new Carte("12","TR");
        this.cartes.add(c);
        c = new Carte("12","CA");
        this.cartes.add(c);
        c = new Carte("11","CO");
        this.cartes.add(c);
        c = new Carte("11","PI");
        this.cartes.add(c);
        c = new Carte("11","TR");
        this.cartes.add(c);
        c = new Carte("11","CA");
        this.cartes.add(c);
        c = new Carte("10","CO");
        this.cartes.add(c);
        c = new Carte("10","PI");
        this.cartes.add(c);
        c = new Carte("10","TR");
        this.cartes.add(c);
        c = new Carte("10","CA");
        this.cartes.add(c);
        c = new Carte("9","CO");
        this.cartes.add(c);
        c = new Carte("9","PI");
        this.cartes.add(c);
        c = new Carte("9","TR");
        this.cartes.add(c);
        c = new Carte("9","CA");
        this.cartes.add(c);
        c = new Carte("8","CO");
        this.cartes.add(c);
        c = new Carte("8","PI");
        this.cartes.add(c);
        c = new Carte("8","TR");
        this.cartes.add(c);
        c = new Carte("8","CA");
        this.cartes.add(c);
        c = new Carte("7","CO");
        this.cartes.add(c);
        c = new Carte("7","PI");
        this.cartes.add(c);
        c = new Carte("7","TR");
        this.cartes.add(c);
        c = new Carte("7","CA");
        this.cartes.add(c);
        c = new Carte("6","CO");
        this.cartes.add(c);
        c = new Carte("6","PI");
        this.cartes.add(c);
        c = new Carte("6","TR");
        this.cartes.add(c);
        c = new Carte("6","CA");
        this.cartes.add(c);
        c = new Carte("5","CO");
        this.cartes.add(c);
        c = new Carte("5","PI");
        this.cartes.add(c);
        c = new Carte("5","TR");
        this.cartes.add(c);
        c = new Carte("5","CA");
        this.cartes.add(c);
        c = new Carte("4","CO");
        this.cartes.add(c);
        c = new Carte("4","PI");
        this.cartes.add(c);
        c = new Carte("4","TR");
        this.cartes.add(c);
        c = new Carte("4","CA");
        this.cartes.add(c);
        c = new Carte("3","CO");
        this.cartes.add(c);
        c = new Carte("3","PI");
        this.cartes.add(c);
        c = new Carte("3","TR");
        this.cartes.add(c);
        c = new Carte("3","CA");
        this.cartes.add(c);
        c = new Carte("2","CO");
        this.cartes.add(c);
        c = new Carte("2","PI");
        this.cartes.add(c);
        c = new Carte("2","TR");
        this.cartes.add(c);
        c = new Carte("2","CA");
        this.cartes.add(c);
        
        this.cartesRestantes.addAll(this.cartes);
    }
    
    public boolean distribuerCarte(Carte ca) {
        boolean result = false;
        if (this.cartesRestantes.contains(ca)) {
            Iterator it = this.cartesRestantes.iterator();
            boolean find = false;
            while (it.hasNext() && find == false) {
                Carte c = (Carte) it.next();
                if (c.equals(ca)) {
                    it.remove();
                    find = true;
                    this.cartesDistribuees.add(ca);
                    result = true;
                }
            }
        }
        return result;
    } 
}
