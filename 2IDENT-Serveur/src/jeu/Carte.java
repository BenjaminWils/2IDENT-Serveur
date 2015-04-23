package jeu;

/**
 *
 * @author Benjamin
 */
public class Carte {
    private String hauteur;
    private String couleur;
    
    public Carte(String hauteur, String couleur) {
        this.couleur = couleur;
        this.hauteur = hauteur;
    }
    
    @Override
    public String toString() {
        return this.hauteur + "-" + this.couleur;
    }
    
    public String getCouleur() {
        return this.couleur;
    }
    
    public String getHauteur() {
        return this.hauteur;
    }
    
    public static Carte parserCarte(String ca) {
        Carte c = new Carte(ca.split("-")[0], ca.split("-")[1]);
        return c;
    }
}
