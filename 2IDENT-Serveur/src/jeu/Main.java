package jeu;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

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
    
    public JSONArray listerCartes(String pseudo) {
        JSONArray listeCartes = new JSONArray();
        ArrayList<Carte> cartes = this.getMainJoueur(pseudo);
        for (Carte ca : cartes) {
            JSONObject obj = new JSONObject();
            obj.put("couleur", ca.getCouleur());
            obj.put("hauteur", ca.getHauteur());
            listeCartes.add(obj);
        }
        return listeCartes;
    }
    
    public JSONArray listerCartes(ArrayList<Carte> cartes) {
        JSONArray listeCartes = new JSONArray();
        for (Carte ca : cartes) {
            JSONObject obj = new JSONObject();
            obj.put("couleur", ca.getCouleur());
            obj.put("hauteur", ca.getHauteur());
            listeCartes.add(obj);
        }
        return listeCartes;
    }
}
