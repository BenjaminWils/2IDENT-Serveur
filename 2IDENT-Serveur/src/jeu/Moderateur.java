package jeu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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
    private int nbParties;
    private int indexTour;
    
    public ArrayList<Connexion> ordreFinJoueurs;
    
    private ArrayList<Connexion> ordreTourJoueurs;
    
    private ArrayList<Connexion> trouDucs;
    
    public Moderateur(Salon salle) {
        this.salle = salle;
        this.rand = new Random();
        this.nbParties = 1;
        this.ordreFinJoueurs = new ArrayList<>();
        this.ordreTourJoueurs = new ArrayList<>();
        this.trouDucs = new ArrayList<>();
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
    
    public void finSession() {
        this.salle.fosse.signalerFinSession();
    }
    public void finPartie() {
        this.attributionRoles();
        nbParties++;
        this.ordreFinJoueurs.clear();
        this.trouDucs.clear();
        synchronized (this.salle.coJoueurs) {
            for(Connexion c : this.salle.coJoueurs) {
                if (c.role == TypeRole.TrouDuCul) {
                    for (int i = 0; i < this.ordreTourJoueurs.size(); i++) {
                        if (this.ordreTourJoueurs.get(i).nomJoueur.equals(c.nomJoueur)) {
                            this.indexTour = i;
                        }
                    }
                }
            }
        }
    }
    
    public void ajouterTrouDuc(Connexion co) {
        this.trouDucs.add(co);
    }
    
    public int generateRandomNumber(int min, int max, Random rand) {
        long range = (long)max - (long)min + 1;
        long fraction = (long)(range * rand.nextDouble());
        int randomNumber =  (int)(fraction + min);    
        return randomNumber;
    }
    
    public void attributionRoles() {
        if (!this.trouDucs.isEmpty()) {
            for (int i = 0; i < this.trouDucs.size(); i++) {
                Connexion co = this.trouDucs.get(i);
                int index = 0;
                for (int j = 0; j < this.ordreFinJoueurs.size(); j++) {
                    if (co.equals(this.ordreFinJoueurs.get(j))) {
                        index = j;
                    }
                }
                this.ordreFinJoueurs.add(co);
                this.ordreFinJoueurs.remove(index);
            }
        }
        
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
        ArrayList<Carte> defossees = this.salle.fosse.getDerniersCartesPosees();
        ArrayList<Carte> defosseesA = this.salle.fosse.getADerniersCartesPosees();
        
        if (cartes.size() < 5) {
            if (!this.salle.aPasse && defossees.size() == 1 && defosseesA.size() == 1 && defossees.get(0).getHauteur().equals(defosseesA.get(0).getHauteur())) {
                if (cartes.size() == 1 && cartes.get(0).getHauteur().equals(defossees.get(0).getHauteur())) {
                    flag = true;
                }
            }
            else if (defossees.isEmpty() || defossees.size() == 4 || (defossees.size() > 0 && defossees.get(0).getHauteur().equals("2"))) {
                // -> nouvelle session
                // tout nb de cartes entre 1 et 4 autorisé
                // toute hauteur autorisée 
                if (cartes.size() == 1) {
                    flag = true;
                } else if (cartes.size() == 2) {
                    String hauteur = cartes.get(0).getHauteur();
                    if (hauteur.equals(cartes.get(1).getHauteur())) {
                        flag = true;
                    }
                } else if (cartes.size() == 3) {
                    String hauteur = cartes.get(0).getHauteur();
                    if (hauteur.equals(cartes.get(1).getHauteur()) && hauteur.equals(cartes.get(2).getHauteur())) {
                        flag = true;
                    }
                } else if (cartes.size() == 4) {
                    String hauteur = cartes.get(0).getHauteur();
                    if (hauteur.equals(cartes.get(1).getHauteur()) && hauteur.equals(cartes.get(2).getHauteur()) && hauteur.equals(cartes.get(3).getHauteur())) {
                        flag = true;
                    }
                }
            } else if (defossees.size() == 1) {
                // tout nb de cartes entre 1 et 4 autorisé
                // si hauteur identique, entre 1 et 3 cartes
                // si hauteur supérieure, entre 1 et 4 cartes
                if (cartes.size() == 1 && (Integer.valueOf(cartes.get(0).getHauteur()) >= Integer.valueOf(defossees.get(0).getHauteur()) || cartes.get(0).getHauteur().equals("2"))) {
                    flag = true;
                }
            } else if (defossees.size() == 2) {
                // nb de cartes entre 2 et 4 autorisé
                // si hauteur identique, entre 1 et 2 cartes
                // si hauteur supérieure, entre 1 et 4 cartes
                if (cartes.size() == 2) {
                    String hauteur = cartes.get(0).getHauteur();
                    if (hauteur.equals(cartes.get(1).getHauteur())) {
                        if (Integer.valueOf(hauteur) >= Integer.valueOf(defossees.get(0).getHauteur()) || hauteur.equals("2")) {
                            flag = true;
                        }
                    }
                }
            } else if (defossees.size() == 3) {
                // nb de cartes entre 3 et 4 autorisé
                // hauteur supérieure uniquement, entre 1 et 4 cartes
                if (cartes.size() == 3) {
                    String hauteur = cartes.get(0).getHauteur();
                    if (hauteur.equals(cartes.get(1).getHauteur()) && hauteur.equals(cartes.get(2).getHauteur())) {
                        if (Integer.valueOf(hauteur) > Integer.valueOf(defossees.get(0).getHauteur()) || hauteur.equals("2")) {
                            flag = true;
                        }
                    }
                }
            }
        }

        return flag;
    }
    
    public Connexion getNextJoueurSession() {
        Connexion co = null;
        synchronized(this.salle.coJoueurs) {
            if (this.ordreTourJoueurs.size() == this.salle.coJoueurs.size()) {
                co = this.ordreTourJoueurs.get(this.indexTour % this.salle.coJoueurs.size());
                this.indexTour++;
            }
            else {
                co = this.salle.coJoueurs.get(this.generateRandomNumber(0, this.salle.coJoueurs.size() - 1, rand));
                while (this.ordreTourJoueurs.contains(co)) {
                    co = this.salle.coJoueurs.get(this.generateRandomNumber(0, this.salle.coJoueurs.size() - 1, rand));
                }
                this.ordreTourJoueurs.add(co);
            }
        }
        return co;
    }
    
    public ArrayList<Carte> getCartesAEchanger(TypeRole role) {
        ArrayList<Carte> cartes = null;
        ArrayList<Carte> main = null;
        if (role != null && role != TypeRole.Neutre) {
            synchronized(this.salle.coJoueurs) {
                for (Connexion co : this.salle.coJoueurs) {
                    if (co.role == role) {
                        main = this.salle.mains.getMainJoueur(co.nomJoueur);
                    }
                }
            }
            
            if (role == TypeRole.Secretaire) {
                Carte max = main.get(0);
                for (Carte ca : main) {
                    if (cartes.size() != 1) {
                        if (ca.getHauteur().equals("2")) {
                            cartes.add(ca);
                        }
                        if (Integer.valueOf(ca.getHauteur()) > Integer.valueOf(max.getHauteur())) {
                            max = ca;
                        }
                    }
                }
                if (cartes.size() != 1) {
                    cartes.add(max);
                }
            }
            
            if (role == TypeRole.TrouDuCul) {
                Carte max = main.get(0);
                Carte max2 = main.get(1);
                for (Carte ca : main) {
                    if (cartes.size() != 2) {
                        if (ca.getHauteur().equals("2")) {
                            cartes.add(ca);
                        }
                        if (Integer.valueOf(ca.getHauteur()) > Integer.valueOf(max.getHauteur()) && !cartes.contains(ca)) {
                            max = ca;
                        }
                    }
                }
                if (cartes.size() != 2) {
                    cartes.add(max);
                }
                if (cartes.size() != 2) {
                    for (Carte ca : main) {
                        if (Integer.valueOf(ca.getHauteur()) > Integer.valueOf(max2.getHauteur()) && !cartes.contains(ca)) {
                            max2 = ca;
                        }
                    }
                    cartes.add(max2);
                }
            }
        }
        
        return cartes;
    }
    
    public ArrayList<ArrayList<Carte>> combinaisonsAutorisees(ArrayList<Carte> main) {
        ArrayList<ArrayList<Carte>> combinaisons = new ArrayList();
        ArrayList<Carte> cartes = null;
        if (main.size() > 3) {
            for (int i = 0; i < main.size(); i++) {
                for (int j = 0; j < main.size(); j++) {
                    for (int k = 0; k < main.size(); k++) {
                        for (int l = 0; l < main.size(); l++) {
                            if (i != j && i != k && i != l && j != k && j != l && k != l) {
                                cartes = new ArrayList();
                                cartes.add(main.get(i));
                                cartes.add(main.get(j));
                                cartes.add(main.get(k));
                                cartes.add(main.get(l));
                                if (this.carteAutorisee(cartes)) {
                                    combinaisons.add(cartes);
                                }
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < main.size(); i++) {
                for (int j = 0; j < main.size(); j++) {
                    for (int k = 0; k < main.size(); k++) {
                        if (i != j && i != k && j != k) {
                            cartes = new ArrayList();
                            cartes.add(main.get(i));
                            cartes.add(main.get(j));
                            cartes.add(main.get(k));
                            if (this.carteAutorisee(cartes)) {
                                combinaisons.add(cartes);
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < main.size(); i++) {
                for (int j = 0; j < main.size(); j++) {
                    if (i != j) {
                        cartes = new ArrayList();
                        cartes.add(main.get(i));
                        cartes.add(main.get(j));
                        if (this.carteAutorisee(cartes)) {
                            combinaisons.add(cartes);
                        }
                    }
                }
            }
            for (int i = 0; i < main.size(); i++) {
                cartes = new ArrayList();
                cartes.add(main.get(i));
                if (this.carteAutorisee(cartes)) {
                    combinaisons.add(cartes);
                }
            }
        } else if (main.size() == 3) {
            for (int i = 0; i < main.size(); i++) {
                for (int j = 0; j < main.size(); j++) {
                    for (int k = 0; k < main.size(); k++) {
                        if (i != j && i != k && j != k) {
                            cartes = new ArrayList();
                            cartes.add(main.get(i));
                            cartes.add(main.get(j));
                            cartes.add(main.get(k));
                            if (this.carteAutorisee(cartes)) {
                                combinaisons.add(cartes);
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < main.size(); i++) {
                for (int j = 0; j < main.size(); j++) {
                    if (i != j) {
                        cartes = new ArrayList();
                        cartes.add(main.get(i));
                        cartes.add(main.get(j));
                        if (this.carteAutorisee(cartes)) {
                            combinaisons.add(cartes);
                        }
                    }
                }
            }
            for (int i = 0; i < main.size(); i++) {
                cartes = new ArrayList();
                cartes.add(main.get(i));
                if (this.carteAutorisee(cartes)) {
                    combinaisons.add(cartes);
                }
            }
        } else if (main.size() == 2) {
            for (int i = 0; i < main.size(); i++) {
                for (int j = 0; j < main.size(); j++) {
                    if (i != j) {
                        cartes = new ArrayList();
                        cartes.add(main.get(i));
                        cartes.add(main.get(j));
                        if (this.carteAutorisee(cartes)) {
                            combinaisons.add(cartes);
                        }
                    }
                }
            }
            for (int i = 0; i < main.size(); i++) {
                cartes = new ArrayList();
                cartes.add(main.get(i));
                if (this.carteAutorisee(cartes)) {
                    combinaisons.add(cartes);
                }
            }
        } else if (main.size() == 1) {
            cartes = new ArrayList();
            cartes.add(main.get(0));
            if (this.carteAutorisee(cartes)) {
                combinaisons.add(cartes);
            }
        }
        return combinaisons;
    }
    
    public JSONArray listerCombinaisons(ArrayList<ArrayList<Carte>> combinaisons) {
        JSONArray listeCombinaisons = new JSONArray();
        for (ArrayList<Carte> cartes : combinaisons) {
            JSONArray listeCartes = new JSONArray();
            for (Carte ca : cartes) {
                JSONObject obj = new JSONObject();
                obj.put("couleur", ca.getCouleur());
                obj.put("hauteur", ca.getHauteur());
                listeCartes.add(obj);
            }
            listeCombinaisons.add(listeCartes);
        }
        return listeCombinaisons;
    }
}
