package jeu;

/**
 *
 * @author Benjamin
 */
public enum TypeRole {
    President("Président"),
    VicePresident("Vice-Président"),
    Secretaire("Secrétaire"),
    TrouDuCul("Trou du cul"),
    Neutre("Neutre");

    private String name = "";

    TypeRole(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
