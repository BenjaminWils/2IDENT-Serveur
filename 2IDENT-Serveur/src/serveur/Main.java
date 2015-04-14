package serveur;

/**
 * Classe exécutable du serveur
 * Lance le thread du serveur de jeu
 * @author Benjamin
 */
public class Main {
    public static void main(String[] args) {
        Serveur serv = new Serveur(2000);
        serv.setPriority(Thread.MAX_PRIORITY);
        serv.start();
        System.out.println("Le serveur est lancé");
    }
}
