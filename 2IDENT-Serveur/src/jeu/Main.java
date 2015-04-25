package jeu;

import java.util.ArrayList;
import java.util.HashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
    
    public boolean carteDupliquee(ArrayList<Carte> cartes) {
        boolean result = false;
        
        return result;
    }
    
    public ArrayList<Carte> parserJSON(String cartesS) throws ParseException {
        ArrayList<Carte> cartes = new ArrayList();
        JSONParser parser = new JSONParser();
        JSONArray cartesJ = (JSONArray) parser.parse(cartesS);
        for (Object o : cartesJ) {
            JSONObject obj = (JSONObject) o;
            Carte c = new Carte((String)obj.get("hauteur"),(String)obj.get("couleur"));
            cartes.add(c);
        }
        return cartes;
    }
}
