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
}
