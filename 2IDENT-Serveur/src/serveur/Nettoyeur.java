package serveur;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread chargé de nettoyer les connexions fermées et les salons inactifs
 * @author Benjamin
 */
public class Nettoyeur extends Thread {
    
    public Nettoyeur() {
        
    }
    
    @Override
    public void run() {
        // Tant que non interrompu
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Patiente 5 secondes
                Thread.currentThread().sleep(5000);
                System.out.println("Nettoyage Connexions");
                // Nettoie les connexions du serveur
                nettoyerConnexions();
                // Patiente 5 secondes
                Thread.currentThread().sleep(5000);
                System.out.println("Nettoyage Salons");
                // Nettoie les salons inactifs du serveur
                nettoyerSalons();
            } catch (InterruptedException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
    
    /**
     * Supprime les salons inactifs de la liste
     */
    public void nettoyerSalons() {
        synchronized(Serveur.salons) {
            Iterator<Salon> it = Serveur.salons.iterator();
            while(it.hasNext())
            {
                Salon sa = it.next();
                // Si le salon est vide
                if (sa.getNbJoueurs() == 0) {
                    System.out.println("Salon nettoyé");
                    it.remove();
                    sa.interrupt();
                }
            }
        }
    }
    
    /**
     * Supprime les connexions fermées de la liste
     */
    public void nettoyerConnexions() {
        synchronized(Serveur.listeConnexions) {
            Iterator<Connexion> it = Serveur.listeConnexions.iterator();
            while(it.hasNext())
            {
                Connexion co = it.next();
                // Si la connexion est fermée
                if (!co.isAlive()) {
                    System.out.println("Connexion nettoyée");
                    it.remove();
                }
            }
        }
    }
}
