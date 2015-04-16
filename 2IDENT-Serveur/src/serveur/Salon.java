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
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import jeu.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author Benjamin
 */
public class Salon extends Thread{
    
    public ArrayList<Connexion> coJoueurs;
    public Moderateur modo;
    public CollectionCartes cartes;
    public jeu.Main mains;
    
    
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
                    int oldJoueursAttendus = 0;
                    // Sors de la boucle lorsqu'il est rempli
                    while (joueursAttendus != 0) {
                        if (joueursAttendus != oldJoueursAttendus) {
                            // Envoi le nb de joueurs attendus aux joueurs
                            // Seulement si ce nombre est différent de l'itération précédente
                            this.ecrireMessageAll("salon::attente::joueurs::" + joueursAttendus);
                            oldJoueursAttendus = joueursAttendus;
                        }
                        sleep(500);
                        joueursAttendus = this.getNbJoueursMax() - this.getNbJoueurs();
                    }
                    
                    // En attente de statut READY des clients
                    // On s'assure que tous les clients vont bien recevoir le message
                    // Permet de synchroniser les clients
                    /*while (this.areReadyConnections() == false) {
                        sleep(500);
                    }*/
                    
                    this.ecrireMessageAll("jeu::demarrage");
                    
                    this.modo = new Moderateur(this);
                    this.cartes = new CollectionCartes();
                    this.mains = new jeu.Main(this.modo.distribution());
                    
                    this.ecrireMessageAll("jeu::infosJoueurs::" + this.listerJoueurs().toJSONString());
                    
                    // Attente d'infos des joueurs
                    wait();
                    
                } catch (SocketException e) {
                    System.out.println("Passage socket");
                    // Atteint dès lors qu'un client a été déconnecté
                    synchronized(this.coJoueurs) {
                        ArrayList<Connexion> tmp = this.checkConnexions();
                        if (tmp.size() > 0) {
                            for (Connexion co : tmp) {
                                this.ecrireMessageAll("salon::deconnection::" + co.nomJoueur);
                            }
                        }
                    }
                    this.nettoyage();
                } catch (IOException e) {
                    System.out.println("Passage IO");
                    // Atteint dès lors qu'un client a été déconnecté
                    synchronized(this.coJoueurs) {
                        ArrayList<Connexion> tmp = this.checkConnexions();
                        if (tmp.size() > 0) {
                            for (Connexion co : tmp) {
                                this.ecrireMessageAll("salon::deconnection::" + co.nomJoueur);
                            }
                        }
                    }
                    this.nettoyage();
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
    public ArrayList<Connexion> checkConnexions() throws SocketException {
        ArrayList<Connexion> cos = new ArrayList<Connexion>();
        synchronized(this.coJoueurs) {
            for (Connexion co : this.coJoueurs) {
                if (co.isSocketClosed()) {
                    cos.add(co);
                }
            }
        }
        return cos;
    }
    
    /**
     * Envoie un message aux joueurs et attend leur réponse
     * @return True si les connexions sont prêtes, False sinon
     * @throws IOException 
     */
    public boolean areReadyConnections() throws IOException {
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
    
    public JSONArray listerJoueurs() {
        JSONArray listeJoueurs = new JSONArray();
        synchronized (this.coJoueurs) {
            for (Connexion co : this.coJoueurs) {
                JSONObject obj = new JSONObject();
                obj.put("pseudo", co.nomJoueur);
                obj.put("role", co.role.toString());
                obj.put("nbCartes", this.mains.getMainJoueur(co.nomJoueur).size());
                listeJoueurs.add(obj);
            }
        }
        return listeJoueurs;
    }
}
