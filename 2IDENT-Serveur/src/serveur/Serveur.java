package serveur;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Classe Serveur : Fait office de serveur de jeu. Accepte et délègue les connexions entrantes.
 * Initialise le nettoyeur
 * @author Benjamin
 */
public class Serveur extends Thread {
    protected static ArrayList<Connexion> listeConnexions = new ArrayList<>();
    protected static ArrayList<Salon> salons = new ArrayList<>();
    protected static Nettoyeur net;
    
    private int portSoc;
    
    private ServerSocket servSoc;
    
    /**
     * Constructeur : Initialise le nettoyeur et démarre le thread
     * @param port 
     */
    public Serveur(int port) {
        this.portSoc = port;
        net = new Nettoyeur();
        net.start();
    }
    
    @Override
    public void run() {
        try {
            // Création du socket serveur
            servSoc = new ServerSocket(this.portSoc);
            
            // Tant que le thread n'est pas interrompu
            while (!Thread.currentThread().isInterrupted()) { 
                System.out.println("Serveur en attente d'un client");
                // Accept est bloquant, et retourne le socket du joueur
                // lorque le socket server accepte une connexion
                Socket soc = servSoc.accept();
                Connexion client = new Connexion(soc);
                client.start();
                // On nettoie les connexions fermées
                // Puis on ajoute la connexion du nouveau joueur à la liste
                synchronized(listeConnexions) {
                    net.nettoyerConnexions();
                    listeConnexions.add(client);
                }
            }
        } catch (IOException ex) {
            System.out.println("Serveur erreur : " + ex);
        }
        finally {
            try {
                // On stoppe le nettoyeur
                net.interrupt();
                // On ferme le socket serveur
                servSoc.close();
            }
            catch (IOException ex) {
                System.out.println("Fermeture du serveur : " + ex);
            }
        }
    }
    
    public static boolean isPseudoPresent(String pseudo) {
        boolean result = false;
        synchronized(listeConnexions) {
            for (Connexion co : listeConnexions) {
                if (co.nomJoueur != null && co.nomJoueur.equals(pseudo)) {
                    result = true;
                }
            }
        }
        return result;
    }
    
    public static boolean isSalonPresent(String nom) {
        boolean result = false;
        synchronized(salons) {
            for (Salon sal : salons) {
                if (sal.nom.equals(nom)) {
                    result = true;
                }
            }
        }
        return result;
    }
    
    public static String listerSalons() {
        String listeSalons = "";
        synchronized (Serveur.salons) {
            for (Salon sal : Serveur.salons) {
                listeSalons += sal.toString();
            }
        }
        return listeSalons;
    }
}
