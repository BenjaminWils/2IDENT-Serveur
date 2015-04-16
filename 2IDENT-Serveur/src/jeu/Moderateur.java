package jeu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import serveur.*;

/**
 *
 * @author Benjamin
 * 
 * Classe chargée d'effectuer les actions du jeu :
 * Distribution, Passage des tours, Avantages/pénalités rôles, Gestion défausse
 * 
 */
public class Moderateur {
    
    private Salon salle;
    private Random rand;
    
    public Moderateur(Salon salle) {
        this.salle = salle;
        this.rand = new Random();
    }
    
    public HashMap<String, ArrayList<Carte>> distribution() {
        HashMap<String, ArrayList<Carte>> mains = new HashMap();
        
        synchronized(this.salle.coJoueurs) {
            int cartesADistrib = this.salle.cartes.cartes.size() / this.salle.coJoueurs.size();
            
            for (Connexion co : this.salle.coJoueurs) {
                ArrayList<Carte> cartes = new ArrayList<>();
                for (int i = 0; i < cartesADistrib; i++) {
                    Carte ca = this.salle.cartes.cartesRestantes.get(this.generateRandomNumber(0, this.salle.cartes.cartesRestantes.size() - 1, rand));
                    this.salle.cartes.distribuerCarte(ca);
                    cartes.add(ca);
                }
                mains.put(co.nomJoueur, cartes);
            }
        }
        return mains;
    }
    
    private int generateRandomNumber(int min, int max, Random rand) {
        long range = (long)max - (long)min + 1;
        long fraction = (long)(range * rand.nextDouble());
        int randomNumber =  (int)(fraction + min);    
        return randomNumber;
    }
}
