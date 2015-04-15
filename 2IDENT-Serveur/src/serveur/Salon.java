package serveur;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Benjamin
 */
public class Salon extends Thread{
    
    private ArrayList<Connexion> coJoueurs;
    private int nbJoueurs;
    
    public String nom;
   
    /**
     * Constructeur : Initialise la liste des connexions des joueurs
     * @param nbJoueurs 
     */
    public Salon(String nom, int nbJoueurs) {
        this.nom = nom;
        this.nbJoueurs = nbJoueurs;
        this.coJoueurs = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "::nbJoueursMax=" + nbJoueurs + ", nbJoueurs=" + String.valueOf(this.getNbJoueurs()) + ", nom=" + nom;
    }
    
    @Override
    public void run() {
        System.out.println("Salon démarré");
        try {
            // Tant qu'il n'est pas interrompu
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Démarre l'attente des joueurs
                    int joueursAttendus = this.getNbJoueursMax() - this.getNbJoueurs();
                    // Sors de la boucle lorsqu'il est rempli
                    while (joueursAttendus != 0) {
                        this.ecrireMessageAll("En attente de " + joueursAttendus + " joueurs.");
                        sleep(500);
                        joueursAttendus = this.getNbJoueursMax() - this.getNbJoueurs();
                    }
                    
                    // En attente de statut READY des clients
                    // On s'assure que tous les clients vont bien recevoir le message
                    // Permet de synchroniser les clients
                    while (this.areReadyConnexions() == false) {
                        sleep(500);
                    }
                    // Variables du jeu
                    int compteurs[] = new int[this.nbJoueurs];
                    int compteurTotal = 0;
                    synchronized(this.coJoueurs) {
                        // Renseigne le nom des joueurs
                        String chaineAdversaires = "::SALON";
                        for (Connexion co : this.coJoueurs) {
                            chaineAdversaires += "::" + co.getNomJoueur();
                        }
                        // Envoie l'entrée en mode salon
                        this.ecrireMessageAll(chaineAdversaires);
                        
                    }
                    this.ecrireMessageAll("::2S::Lancement de la partie !");
                    // En attente de statut READY des clients
                    while (this.areReadyConnexions() == false) {
                        sleep(500);
                    }
                    sleep(200);
                    
                    // On signale la fin du mode salon
                    this.ecrireMessageAll("::!SALON::");
                    // On indique l'arrêt
                    this.ecrireMessageAll("::DESTROY::");
                } catch (SocketException e) {
                    // Atteint dès lors qu'un client a été déconnecté
                    this.ecrireMessageAll("::3S::Un client s'est déconnecté. Reprise de la file d'attente...");
                    boolean flag2 = false;
                    while (flag2 == false) {
                        // Tant que les connexions ne sont pas supprimées de la liste
                        // On vérifie l'état des connexions
                        boolean flag = false;
                        while (flag == false) {
                            this.nettoyage();
                            try {
                                this.checkConnexions();
                                flag = true;
                            } catch (SocketException ex) {
                                flag = false;
                            }
                            sleep(500);
                        }
                        try {
                            // En attente de statut READY des clients
                            while (this.areReadyConnexions() == false) {
                                sleep(500);
                            }
                            flag2 = true;
                        }
                        catch (SocketException er) {
                            flag2 = false;
                        }
                        catch (IOException er) {
                            flag2 = false;
                        }
                    }
                    // On indique la sortie du mode salon (permet de reprendre l'attente)
                    this.ecrireMessageAll("::!SALON::");
                } catch (IOException e) {
                    this.ecrireMessageAll("::3S::Un client s'est déconnecté. Reprise de la file d'attente...");
                    boolean flag2 = false;
                    while (flag2 == false) {
                        boolean flag = false;
                        while (flag == false) {
                            this.nettoyage();
                            try {
                                this.checkConnexions();
                                flag = true;
                            } catch (SocketException ex) {
                                flag = false;
                            }
                            sleep(500);
                        }
                        try {
                            // En attente de statut READY des clients
                            while (this.areReadyConnexions() == false) {
                                sleep(500);
                            }
                            flag2 = true;
                        }
                        catch (SocketException er) {
                            flag2 = false;
                        }
                        catch (IOException er) {
                            flag2 = false;
                        }
                    }
                    this.ecrireMessageAll("::!SALON::");
                }
            }
        }
        catch (Exception e) {
            System.out.println("Salon : " + e.getMessage());
        }
    }
    
    /**
     * Ajoute un joueur à la liste des connexions du salon
     * @param co La connexion du joueur à ajouter
     * @return True si l'ajout s'est bien passé, False sinon
     * @throws IOException
     * @throws InterruptedException 
     */
    public boolean ajoutJoueur(Connexion co) throws IOException, InterruptedException {
        boolean ajout = false;
        // Retire les connexions fermées de la liste des connexions
        nettoyage();
        synchronized(this.coJoueurs) {
            // S'il y a encore de la place dans le salon
            // On ajoute le joueur
            if (this.coJoueurs.size() < nbJoueurs) {
                this.coJoueurs.add(co);
                ajout = true;
            }
        }
        return ajout;
    }
    
    public int getNbJoueursMax() {
        return this.nbJoueurs;
    }
    
    /**
     * Nettoie la liste des connexions puis vérifie leur nombre, avant de le retourner
     * @return Le nombre de joueurs actuellement dans le salon
     */
    public int getNbJoueurs() {
        int taille = this.nbJoueurs;
        nettoyage();
        synchronized(this.coJoueurs) {
            taille = this.coJoueurs.size();
        }
        return taille;
    }
    
    /**
     * Vérifie l'état de chaque connexion de la liste, et la supprime si elle est fermée
     */
    public void nettoyage() {
        synchronized(this.coJoueurs) {
            Iterator it = this.coJoueurs.iterator();
            while (it.hasNext()) {
                Connexion co = (Connexion) it.next();
                // Si connexion fermée
                if (co.isSocketClosed()) {
                    it.remove();
                    System.out.println("Connexion supprimée du salon");
                }
            }
        }
    }
    
    /**
     * Vérifie que toutes les connexions sont ouvertes, et lève une exception dans le cas contraire
     * @throws SocketException 
     */
    public void checkConnexions() throws SocketException {
        synchronized(this.coJoueurs) {
            for (Connexion co : this.coJoueurs) {
                if (co.isSocketClosed()) {
                    throw new SocketException("Socket Client fermé");
                }
            }
        }
    }
    
    /**
     * Envoie un message aux joueurs et attend leur réponse
     * @return True si les connexions sont prêtes, False sinon
     * @throws IOException 
     */
    public boolean areReadyConnexions() throws IOException {
        boolean ready = true;
        synchronized(this.coJoueurs) {
            for (Connexion co : this.coJoueurs) {
                if (!co.isReady()) {
                    ready = false;
                }
            }
        }
        return ready;
    }
    
    /**
     * Envoie un message au joueur par la connexion spécifiée
     * @param co Connexion utilisée
     * @param msg Message envoyé
     * @throws IOException 
     */
    public void ecrireMessage(Connexion co, String msg) throws IOException {
        co.ecrireMessage(msg);
    }
    
    /**
     * Envoie un message à tous les joueurs du salon
     * @param msg Message envoyé
     * @throws IOException 
     */
    public void ecrireMessageAll(String msg) throws IOException {
        synchronized(this.coJoueurs) {
            for (Connexion co : this.coJoueurs) {
                co.ecrireMessage(msg);
            }
        }
    }
}
