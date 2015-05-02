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
    
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        try {
            Carte c = (Carte) obj;
            if (this.couleur.equals(c.couleur) && this.hauteur.equals(c.hauteur)) {
                result = true;
            }
        }
        catch (Exception e) {
            System.out.println("Carte : erreur equals");
        }
        return result;
    }
    
    public String chaineLongue() {
        String c = "";
        if (this.hauteur.equals("11")) {
            c += "Valet ";
        }
        else if (this.hauteur.equals("12")) {
            c += "Dame ";
        }
        else if (this.hauteur.equals("13")) {
            c += "Roi ";
        }
        else if (this.hauteur.equals("14")) {
            c += "As ";
        }
        else {
            c += this.hauteur + " ";
        }
        
        if (this.couleur.equals("CA")) {
            c += "de carreau";
        }
        else if (this.couleur.equals("CO")) {
            c += "de coeur";
        }
        else if (this.couleur.equals("PI")) {
            c += "de pique";
        }
        else if (this.couleur.equals("TR")) {
            c += "de trêfle";
        }
        return c;
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
