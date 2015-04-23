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
import java.util.concurrent.Semaphore;
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
    
    public Semaphore semaphore;
    
    public boolean aSyncRep = false;
    
    private ArrayList<Connexion> reponses;
    private int nbReponsesNeg;
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
        this.reponses = new ArrayList<Connexion>();
    }

    @Override
    public String toString() {
        return "::nbJoueursMax=" + nbJoueurs + ", nbJoueurs=" + String.valueOf(this.getNbJoueurs()) + ", nom=" + nom;
    }
    
    @Override
    public void run() {
        System.out.println("Salon démarré");
        try {
            boolean repriseSalon = false;
            int tentatives = 0;
            // Tant qu'il n'est pas interrompu
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    this.nbJoueurs = this.getNbJoueurs();
                    if (repriseSalon) {
                        // Si la déconnexion d'un joueur a interrompu une partie précédente
                        this.ecrireMessageAll("salon::reprise");
                        
                        tentatives = 0;
                        this.nbReponsesNeg = 0;
                        this.reponses.clear();
                        this.aSyncAreReadyConnections();
                        while (!this.aSyncRep && this.nbReponsesNeg == 0 && tentatives < 20) {
                            tentatives++;
                            this.aSyncAreReadyConnections();
                        }
                        
                        if (this.nbReponsesNeg > 0) {
                            this.ecrireMessageAll("salon::fin");
                            synchronized (this.coJoueurs) {
                                for (Connexion co : this.coJoueurs) {
                                    co.salle = null;
                                }
                            }
                            throw new Exception("Salon terminé");
                        }
                    }
                    
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
                    if (!this.areReadyConnections(5)) {
                        throw new SocketException("Absence de réponse d'un/de joueur(s)");
                    }
                    
                    // Informe du démarrage du jeu
                    this.ecrireMessageAll("jeu::demarrage");
                    
                    // Démarre le modérateur de jeu
                    this.modo = new Moderateur(this);
                    // Initialise le jeu de cartes
                    this.cartes = new CollectionCartes();
                    // Distribue les cartes pour chaque joueur (une main pour chaque joueur)
                    this.mains = new jeu.Main(this.modo.premiereDistribution());
                    // Signale le début de la session de jeu
                    this.modo.debutSession();
                    
                    // Informe les joueurs de leurs adversaires
                    this.ecrireMessageAll("jeu::infosJoueurs::" + this.listerJoueurs().toJSONString());
                    
                    if (!this.areReadyConnections(5)) {
                        throw new SocketException("Absence de réponse d'un/de joueur(s)");
                    }
                    
                    // Informe chaque joueur des cartes en sa possession
                    synchronized(this.coJoueurs) {
                        for (Connexion co : this.coJoueurs) {
                            this.ecrireMessage(co, "jeu::infosCartes::" + this.mains.listerCartes(co.nomJoueur).toJSONString());
                        }
                    }
                    
                    if (!this.areReadyConnections(5)) {
                        throw new SocketException("Absence de réponse d'un/de joueur(s)");
                    }
                    
                    // Annonce du premier joueur qui joue
                    Connexion tourJoueur = this.modo.getPremierJoueurSession();
                    this.ecrireMessageAll("jeu::tour::" + tourJoueur.nomJoueur);
                    
                    if (!this.areReadyConnections(5)) {
                        throw new SocketException("Absence de réponse d'un/de joueur(s)");
                    }
                    
                    // Informe le joueur dont c'est le tour des cartes qu'il peut jouer
                    // /!\ Réflexion en terme de combinaisons de carte
                    ArrayList<Carte> main = this.mains.getMainJoueur(tourJoueur.nomJoueur);
                    if (main.size() > 3) {
                        for (int i = 0; i < main.size(); i++) {
                            for (int j = 0; j < main.size(); j++) {
                                for (int k = 0; k < main.size(); k++) {
                                    for (int l = 0; l < main.size(); l++) {
                                        
                                    }
                                }
                            }
                        }
                    }
                    else if (main.size() == 3) {
                        for (int i = 0; i < main.size(); i++) {
                            for (int j = 0; j < main.size(); j++) {
                                for (int k = 0; k < main.size(); k++) {
                                    
                                }
                            }
                        }
                    }
                    else if (main.size() == 2) {
                        for (int i = 0; i < main.size(); i++) {
                            for (int j = 0; j < main.size(); j++) {
                                
                            }
                        }
                    }
                    else if (main.size() == 1) {
                        
                    }
                    
                    this.ecrireMessage(tourJoueur, "jeu: :cartesJouables::" + this.mains.listerCartes(main).toJSONString());
                    
                    // Attente d'infos du joueur dont c'est le tour
                    this.semaphore.acquire();
                    if (!tourJoueur.currentMsg.matches("jeu::carte::.*")) {
                        this.ecrireMessage(tourJoueur, "jeu::carte");
                    }
                    else {
                        
                    }
                    
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
                    repriseSalon = true;
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
                    repriseSalon = true;
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
     * @param nbTentatives
     * @return True si les connexions sont prêtes, False sinon
     * @throws IOException 
     */
    public boolean areReadyConnections(int nbTentatives) throws IOException, InterruptedException {
        boolean ready = true;
        int tentatives = 0;
        this.reponses.clear();
        int taille;
        synchronized(this.reponses) {
            taille = this.reponses.size();
        }
        while (taille != this.nbJoueurs && tentatives < nbTentatives) {
            synchronized(this.coJoueurs) {
                for (Connexion co : this.coJoueurs) {
                    if (!co.isReady()) {
                        ready = false;
                    }
                }
            }
            sleep(500);
            tentatives++;
            synchronized(this.reponses) {
            taille = this.reponses.size();
        }
        }

        if (tentatives >= nbTentatives) {
            ready = false;
        }
        return ready;
    }
    
    public void aSyncAreReadyConnections() throws IOException, InterruptedException {
        this.aSyncRep = false;
        int taille;
        synchronized(this.reponses) {
            taille = this.reponses.size();
        }
        if (taille != this.nbJoueurs) {
            synchronized(this.coJoueurs) {
                for (Connexion co : this.coJoueurs) {
                    co.isReady();
                }
            }
        }
        else {
            this.aSyncRep = true;
        }
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
    
    public void repondre(Connexion co) {
        if (!this.reponses.contains(co)) {
            this.reponses.add(co);
        }
    }
    
    public void voterContre() {
        this.nbReponsesNeg++;
    }
}
