package jeu;

/**
 *
 * @author Benjamin
 */
public enum TypeCarte {
    JOKER("J"),
    AS("A"),
    ROI("R"),
    DAME("D"),
    VALET("V"),
    DIX("10"),
    NEUF("9"),
    HUIT("8"),
    SEPT("7"),
    SIX("6"),
    CINQ("5"),
    QUATRE("4"),
    TROIS("3"),
    DEUX("2");
    
    private String name = "";

    TypeCarte(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
