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
    private int nbSessions;
    
    private ArrayList<Connexion> ordreFinJoueurs;
    
    public Moderateur(Salon salle) {
        this.salle = salle;
        this.rand = new Random();
        this.nbSessions = 0;
        this.ordreFinJoueurs = new ArrayList<>();
    }
    
    public HashMap<String, ArrayList<Carte>> premiereDistribution() {
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
    
    public void debutSession() {
        this.nbSessions++;
        this.ordreFinJoueurs.clear();
    }
    
    public void finSession() {
        this.attributionRoles();
    }
    
    public int generateRandomNumber(int min, int max, Random rand) {
        long range = (long)max - (long)min + 1;
        long fraction = (long)(range * rand.nextDouble());
        int randomNumber =  (int)(fraction + min);    
        return randomNumber;
    }
    
    public void attributionRoles() {
        for (Connexion co : this.ordreFinJoueurs) {
            co.role = null;
        }
        if (this.ordreFinJoueurs.size() < 4) {
            this.ordreFinJoueurs.get(0).role = TypeRole.President;
            this.ordreFinJoueurs.get(1).role = TypeRole.Neutre;
            this.ordreFinJoueurs.get(2).role = TypeRole.TrouDuCul;
        }
        else {
            this.ordreFinJoueurs.get(0).role = TypeRole.President;
            this.ordreFinJoueurs.get(1).role = TypeRole.VicePresident;
            this.ordreFinJoueurs.get(this.ordreFinJoueurs.size() - 2).role = TypeRole.Secretaire;
            this.ordreFinJoueurs.get(this.ordreFinJoueurs.size() - 1).role = TypeRole.TrouDuCul;
            
            for (Connexion co : this.ordreFinJoueurs) {
                if (co.role != null) {
                    co.role = TypeRole.Neutre;
                }
            }
        }
    }
    
    public void mainVide(Connexion co) {
        this.ordreFinJoueurs.add(co);
    }
    
    public boolean carteAutorisee(ArrayList<Carte> cartes) {
        boolean flag = false;
        
        return flag;
    }
    
    public Connexion getPremierJoueurSession() {
        Connexion co = null;
        synchronized(this.salle.coJoueurs) {
            for (Connexion c : this.salle.coJoueurs) {
                if (co.role == TypeRole.President) {
                    co = c;
                }
            }
            if (co == null) {
                co = this.salle.coJoueurs.get(this.generateRandomNumber(0, this.salle.coJoueurs.size() - 1, rand));
            }
        }
        return co;
    }
}
