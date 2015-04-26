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
public class Salon extends Thread {

    public ArrayList<Connexion> coJoueurs;
    public Moderateur modo;
    public CollectionCartes cartes;
    public jeu.Main mains;
    public Defausse fosse;

    public Semaphore semaphore;

    public boolean aSyncRep = false;

    private ArrayList<Connexion> reponses;
    private int nbReponsesNeg;
    private int nbJoueurs;

    public String nom;

    /**
     * Constructeur : Initialise la liste des connexions des joueurs
     *
     * @param nbJoueurs
     */
    public Salon(String nom, int nbJoueurs) {
        this.nom = nom;
        this.nbJoueurs = nbJoueurs;
        this.coJoueurs = new ArrayList<>();
        this.reponses = new ArrayList<Connexion>();
        this.semaphore = new Semaphore(0);
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
            boolean premierePartie = true;
            int tentatives = 0;
            // Tant qu'il n'est pas interrompu
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (repriseSalon) {
                        // Si la déconnexion d'un joueur a interrompu une partie précédente
                        this.nbJoueurs = this.getNbJoueurs();
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

                    System.out.println("File d'attente");

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
                    /*
                     if (!this.areReadyConnections(5)) {
                     throw new SocketException("Absence de réponse d'un/de joueur(s)");
                     }
                     */
                    boolean finParties = false;
                    while (!finParties) {
                        if (premierePartie) {
                            // Informe du démarrage du jeu
                            this.ecrireMessageAll("jeu::demarrage");
                            // Démarre le modérateur de jeu
                            this.modo = new Moderateur(this);
                            // Initialise le jeu de cartes
                            this.cartes = new CollectionCartes();
                            // Distribue les cartes pour chaque joueur (une main pour chaque joueur)
                            this.mains = new jeu.Main(this.modo.distribution());
                            this.fosse = new Defausse();
                        } else {
                            this.ecrireMessageAll("jeu::demarrage");
                            // Initialise le jeu de cartes
                            this.cartes.initialisationDuJeu();
                            // Distribue les cartes pour chaque joueur (une main pour chaque joueur)
                            this.mains = new jeu.Main(this.modo.distribution());

                            this.fosse.vider();
                        }

                        // Informe les joueurs de leurs adversaires
                        this.ecrireMessageAll("jeu::infosJoueurs::" + this.listerJoueurs().toJSONString());

                        /*
                         if (!this.areReadyConnections(5)) {
                         throw new SocketException("Absence de réponse d'un/de joueur(s)");
                         }
                         */
                        // Informe chaque joueur des cartes en sa possession
                        synchronized (this.coJoueurs) {
                            for (Connexion co : this.coJoueurs) {
                                this.ecrireMessage(co, "jeu::infosCartes::" + this.mains.listerCartes(co.nomJoueur).toJSONString());
                            }
                        }

                        /*
                         if (!this.areReadyConnections(5)) {
                         throw new SocketException("Absence de réponse d'un/de joueur(s)");
                         }
                         */
                        this.ecrireMessageAll("jeu::infosCartesRestantes::" + this.mains.listerCartes(this.cartes.cartesRestantes).toJSONString());

                        /*
                         if (!this.areReadyConnections(5)) {
                         throw new SocketException("Absence de réponse d'un/de joueur(s)");
                         }
                         */
                        if (!premierePartie) {
                            // fonctions d'échange des cartes en fonction des rôles
                            if (this.nbJoueurs > 3) {
                                // échange entre Président et TrouDuCul, entre VicePrésident et Secrétaire
                                Connexion president = null;
                                ArrayList<Carte> presidentC = null;
                                Connexion vicePresident = null;
                                ArrayList<Carte> vicePresidentC = null;
                                Connexion secretaire = null;
                                ArrayList<Carte> secretaireC = null;
                                Connexion trouDuc = null;
                                ArrayList<Carte> trouDucC = null;

                                boolean PcartesDeLaMain = true;
                                boolean VPcartesDeLaMain = true;

                                do {
                                    synchronized (this.coJoueurs) {
                                        for (Connexion co : this.coJoueurs) {
                                            if (co.role == TypeRole.President && president == null) {
                                                president = co;
                                                this.ecrireMessage(co, "jeu::echange::president");
                                            } else if (president != null) {
                                                this.ecrireMessage(co, "jeu::echange::president");
                                            }
                                            if (co.role == TypeRole.VicePresident && vicePresident == null) {
                                                vicePresident = co;
                                                this.ecrireMessage(co, "jeu::echange::vicepresident");
                                            } else if (vicePresident != null) {
                                                this.ecrireMessage(co, "jeu::echange::president");
                                            }
                                            if (co.role == TypeRole.Secretaire && secretaire == null) {
                                                secretaire = co;
                                            }
                                            if (co.role == TypeRole.TrouDuCul && trouDuc == null) {
                                                trouDuc = co;
                                            }
                                        }
                                    }
                                    this.semaphore.acquire(2);
                                    if (president != null && president.currentMsg.matches("jeu::echange::.*")) {
                                        String presidentE = president.currentMsg.split("::")[2];
                                        presidentC = this.mains.parserJSON(presidentE);
                                        if (presidentC.size() == 2 && !this.mains.carteDupliquee(presidentC)) {
                                            for (Carte ca : presidentC) {
                                                if (!this.mains.getMainJoueur(president.nomJoueur).contains(ca)) {
                                                    PcartesDeLaMain = false;
                                                }
                                            }
                                        } else {
                                            PcartesDeLaMain = false;
                                        }
                                    }
                                    if (vicePresident != null && vicePresident.currentMsg.matches("jeu::echange::.*")) {
                                        String vicePresidentE = vicePresident.currentMsg.split("::")[2];
                                        vicePresidentC = this.mains.parserJSON(vicePresidentE);
                                        if (vicePresidentC.size() == 2 && !this.mains.carteDupliquee(vicePresidentC)) {
                                            if (!this.mains.getMainJoueur(vicePresident.nomJoueur).contains(vicePresidentC.get(0))) {
                                                VPcartesDeLaMain = false;
                                            }
                                        } else {
                                            VPcartesDeLaMain = false;
                                        }
                                    }
                                } while (presidentC == null || vicePresidentC == null || !PcartesDeLaMain || !VPcartesDeLaMain);

                                secretaireC = this.modo.getCartesAEchanger(TypeRole.Secretaire);
                                trouDucC = this.modo.getCartesAEchanger(TypeRole.TrouDuCul);

                                this.ecrireMessage(trouDuc, "jeu::echange::donner::" + this.mains.listerCartes(trouDucC).toJSONString());
                                this.ecrireMessage(secretaire, "jeu::echange::donner::" + this.mains.listerCartes(secretaireC).toJSONString());
                                this.ecrireMessage(vicePresident, "jeu::echange::donner::" + this.mains.listerCartes(vicePresidentC).toJSONString());
                                this.ecrireMessage(president, "jeu::echange::donner::" + this.mains.listerCartes(presidentC).toJSONString());

                                this.mains.prendreCartes(trouDuc.nomJoueur, trouDucC);
                                this.mains.prendreCartes(secretaire.nomJoueur, secretaireC);
                                this.mains.prendreCartes(vicePresident.nomJoueur, vicePresidentC);
                                this.mains.prendreCartes(president.nomJoueur, presidentC);

                                /*
                                 if (!this.areReadyConnections(5)) {
                                 throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                 }
                                 */
                                this.ecrireMessage(trouDuc, "jeu::echange::recevoir::" + this.mains.listerCartes(presidentC).toJSONString());
                                this.ecrireMessage(secretaire, "jeu::echange::recevoir::" + this.mains.listerCartes(vicePresidentC).toJSONString());
                                this.ecrireMessage(vicePresident, "jeu::echange::recevoir::" + this.mains.listerCartes(secretaireC).toJSONString());
                                this.ecrireMessage(president, "jeu::echange::recevoir::" + this.mains.listerCartes(trouDucC).toJSONString());

                                this.mains.donnerCartes(trouDuc.nomJoueur, presidentC);
                                this.mains.donnerCartes(secretaire.nomJoueur, vicePresidentC);
                                this.mains.donnerCartes(vicePresident.nomJoueur, secretaireC);
                                this.mains.donnerCartes(president.nomJoueur, trouDucC);

                                /*
                                 if (!this.areReadyConnections(5)) {
                                 throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                 }
                                 */
                                this.ecrireMessage(president, "jeu::infosCartes::" + this.mains.listerCartes(president.nomJoueur).toJSONString());
                                this.ecrireMessage(trouDuc, "jeu::infosCartes::" + this.mains.listerCartes(trouDuc.nomJoueur).toJSONString());
                                this.ecrireMessage(secretaire, "jeu::infosCartes::" + this.mains.listerCartes(secretaire.nomJoueur).toJSONString());
                                this.ecrireMessage(vicePresident, "jeu::infosCartes::" + this.mains.listerCartes(vicePresident.nomJoueur).toJSONString());

                            } else {
                                // échange entre Président et TrouDuCul
                                Connexion president = null;
                                ArrayList<Carte> presidentC = null;
                                Connexion trouDuc = null;
                                ArrayList<Carte> trouDucC = null;

                                boolean PcartesDeLaMain = true;

                                do {
                                    synchronized (this.coJoueurs) {
                                        for (Connexion co : this.coJoueurs) {
                                            if (co.role == TypeRole.President && president == null) {
                                                president = co;
                                                this.ecrireMessage(co, "jeu::echange::president");
                                            } else if (president != null) {
                                                this.ecrireMessage(co, "jeu::echange::president");
                                            }
                                            if (co.role == TypeRole.TrouDuCul && trouDuc == null) {
                                                trouDuc = co;
                                            }
                                        }
                                    }
                                    this.semaphore.acquire();
                                    if (president != null && president.currentMsg.matches("jeu::echange::.*")) {
                                        String presidentE = president.currentMsg.split("::")[2];
                                        presidentC = this.mains.parserJSON(presidentE);
                                        if (presidentC.size() == 2 && !this.mains.carteDupliquee(presidentC)) {
                                            for (Carte ca : presidentC) {
                                                if (!this.mains.getMainJoueur(president.nomJoueur).contains(ca)) {
                                                    PcartesDeLaMain = false;
                                                }
                                            }
                                        } else {
                                            PcartesDeLaMain = false;
                                        }
                                    }

                                } while (presidentC == null || !PcartesDeLaMain);

                                trouDucC = this.modo.getCartesAEchanger(TypeRole.TrouDuCul);

                                this.ecrireMessage(trouDuc, "jeu::echange::donner::" + this.mains.listerCartes(trouDucC).toJSONString());
                                this.ecrireMessage(president, "jeu::echange::donner::" + this.mains.listerCartes(presidentC).toJSONString());

                                this.mains.prendreCartes(trouDuc.nomJoueur, trouDucC);
                                this.mains.prendreCartes(president.nomJoueur, presidentC);

                                /*
                                 if (!this.areReadyConnections(5)) {
                                 throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                 }
                                 */
                                this.ecrireMessage(trouDuc, "jeu::echange::recevoir::" + this.mains.listerCartes(presidentC).toJSONString());
                                this.ecrireMessage(president, "jeu::echange::recevoir::" + this.mains.listerCartes(trouDucC).toJSONString());

                                this.mains.donnerCartes(trouDuc.nomJoueur, presidentC);
                                this.mains.donnerCartes(president.nomJoueur, trouDucC);

                                /*
                                 if (!this.areReadyConnections(5)) {
                                 throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                 }
                                 */
                                this.ecrireMessage(president, "jeu::infosCartes::" + this.mains.listerCartes(president.nomJoueur).toJSONString());
                                this.ecrireMessage(trouDuc, "jeu::infosCartes::" + this.mains.listerCartes(trouDuc.nomJoueur).toJSONString());
                            }
                        }
                        /*
                         if (!this.areReadyConnections(5)) {
                         throw new SocketException("Absence de réponse d'un/de joueur(s)");
                         }
                         */

                        boolean sessionSuivante = true;
                        while (sessionSuivante) {
                            boolean sessionPoursuivie = true;
                            while (sessionPoursuivie) {
                                premierePartie = false;
                                // Annonce du premier joueur qui joue
                                Connexion tourJoueur = this.modo.getNextJoueurSession();
                                while (this.mains.getMainJoueur(tourJoueur.nomJoueur).isEmpty()) {
                                    tourJoueur = this.modo.getNextJoueurSession();
                                }
                                this.ecrireMessageAll("jeu::tour::" + tourJoueur.nomJoueur);

                                /*
                                 if (!this.areReadyConnections(5)) {
                                 throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                 }
                                 */
                                // Informe le joueur dont c'est le tour des cartes qu'il peut jouer
                                // /!\ Réflexion en terme de combinaisons de carte
                                ArrayList<Carte> main = this.mains.getMainJoueur(tourJoueur.nomJoueur);
                                ArrayList<ArrayList<Carte>> combinaisons = this.modo.combinaisonsAutorisees(main);

                                this.ecrireMessage(tourJoueur, "jeu::cartesJouables::" + this.modo.listerCombinaisons(combinaisons).toJSONString());

                                // Attente d'infos du joueur dont c'est le tour
                                boolean msgAttendu = false;
                                while (!msgAttendu) {
                                    this.semaphore.acquire();
                                    String msgJoueurTour = tourJoueur.currentMsg;
                                    if (!msgJoueurTour.matches("jeu::cartesJouees::.*")) {
                                        this.ecrireMessage(tourJoueur, "jeu::cartesJouees::erreur::En attente des cartes jouées !");
                                    } else {
                                        String chaineCartes = msgJoueurTour.split("::")[2];
                                        if (chaineCartes.equals("")) {
                                            // Passe son tour
                                            msgAttendu = true;
                                        } else {
                                            ArrayList<Carte> cartesJouees = this.mains.parserJSON(chaineCartes);
                                            boolean cartesDeLaMain = true;
                                            for (Carte ca : cartesJouees) {
                                                if (!main.contains(ca)) {
                                                    cartesDeLaMain = false;
                                                }
                                            }
                                            if (!cartesDeLaMain) {
                                                this.ecrireMessage(tourJoueur, "jeu::cartesJouees::erreur::Ces cartes ne font pas partie de votre jeu !");
                                            } else if (!this.mains.carteDupliquee(cartesJouees) && this.modo.carteAutorisee(cartesJouees)) {
                                                msgAttendu = true;
                                                for (Carte ca : cartesJouees) {
                                                    this.mains.jouerCarte(tourJoueur.nomJoueur, ca);
                                                }
                                                this.fosse.poserCartes(tourJoueur.nomJoueur, cartesJouees);
                                                this.ecrireMessageAll("jeu::infosJoueurs::" + this.listerJoueurs().toJSONString());
                                                /*
                                                 if (!this.areReadyConnections(5)) {
                                                 throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                                 }
                                                 */
                                                this.ecrireMessage(tourJoueur, "jeu::infosCartes::" + this.mains.listerCartes(tourJoueur.nomJoueur).toJSONString());
                                                /*
                                                 if (!this.areReadyConnections(5)) {
                                                 throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                                 }
                                                 */
                                                this.ecrireMessageAll("jeu::cartesPosees::" + this.mains.listerCartes(cartesJouees).toJSONString());
                                                if (cartesJouees.get(0).getHauteur().equals("2") || (!this.fosse.getDerniersCartesPosees().isEmpty() && (cartesJouees.size() + this.fosse.getDerniersCartesPosees().size()) == 4 && this.fosse.getDerniersCartesPosees().get(0).getHauteur().equals(cartesJouees.get(0).getHauteur()))) {
                                                    sessionPoursuivie = false;
                                                    /*
                                                     if (!this.areReadyConnections(5)) {
                                                     throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                                     }
                                                     */
                                                    this.ecrireMessageAll("jeu::sessionSuivante");
                                                }
                                                if (this.mains.getMainJoueur(tourJoueur.nomJoueur).isEmpty()) {
                                                    this.modo.mainVide(tourJoueur);
                                                    if (cartesJouees.get(0).getHauteur().equals("2")) {
                                                        this.modo.ajouterTrouDuc(tourJoueur);
                                                        /*
                                                         if (!this.areReadyConnections(5)) {
                                                         throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                                         }
                                                         */
                                                        this.ecrireMessageAll("chat::" + tourJoueur.nomJoueur + ", vous avez joué un 2 en dernier. Vous serez Trou du Cul à la prochaine partie.");
                                                    }
                                                    if (this.modo.ordreFinJoueurs.size() == this.nbJoueurs - 1) {
                                                        // Fin de la partie - 1 seul joueur a encore des cartes
                                                        sessionSuivante = false;
                                                        synchronized (this.coJoueurs) {
                                                            for (Connexion co : this.coJoueurs) {
                                                                if (!this.modo.ordreFinJoueurs.contains(co)) {
                                                                    this.modo.mainVide(co);
                                                                }
                                                            }
                                                        }
                                                        /*
                                                         if (!this.areReadyConnections(5)) {
                                                         throw new SocketException("Absence de réponse d'un/de joueur(s)");
                                                         }
                                                         */
                                                        this.ecrireMessageAll("jeu::partieSuivante");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            }
                            this.modo.finSession();
                        }
                        /*
                         if (!this.areReadyConnections(5)) {
                         throw new SocketException("Absence de réponse d'un/de joueur(s)");
                         }
                         */

                        this.modo.finPartie();
                    }
                } catch (SocketException e) {
                    System.out.println("Passage socket");
                    // Atteint dès lors qu'un client a été déconnecté
                    synchronized (this.coJoueurs) {
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
                    synchronized (this.coJoueurs) {
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
        } catch (Exception e) {
            System.out.println("Salon : " + e.getMessage());
        }
    }

    /**
     * Ajoute un joueur à la liste des connexions du salon
     *
     * @param co La connexion du joueur à ajouter
     * @return True si l'ajout s'est bien passé, False sinon
     * @throws IOException
     * @throws InterruptedException
     */
    public boolean ajoutJoueur(Connexion co) throws IOException, InterruptedException {
        boolean ajout = false;
        // Retire les connexions fermées de la liste des connexions
        nettoyage();
        synchronized (this.coJoueurs) {
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
     * Nettoie la liste des connexions puis vérifie leur nombre, avant de le
     * retourner
     *
     * @return Le nombre de joueurs actuellement dans le salon
     */
    public int getNbJoueurs() {
        int taille = this.nbJoueurs;
        nettoyage();
        synchronized (this.coJoueurs) {
            taille = this.coJoueurs.size();
        }
        return taille;
    }

    /**
     * Vérifie l'état de chaque connexion de la liste, et la supprime si elle
     * est fermée
     */
    public void nettoyage() {
        synchronized (this.coJoueurs) {
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
     * Vérifie que toutes les connexions sont ouvertes, et lève une exception
     * dans le cas contraire
     *
     * @return 
     * @throws SocketException
     */
    public ArrayList<Connexion> checkConnexions() throws SocketException {
        ArrayList<Connexion> cos = new ArrayList<>();
        synchronized (this.coJoueurs) {
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
     *
     * @param nbTentatives
     * @return True si les connexions sont prêtes, False sinon
     * @throws IOException
     * @throws java.lang.InterruptedException
     */
    public boolean areReadyConnections(int nbTentatives) throws IOException, InterruptedException {
        boolean ready = true;
        int tentatives = 0;
        this.reponses.clear();
        int taille;
        synchronized (this.reponses) {
            taille = this.reponses.size();
        }
        while (taille != this.nbJoueurs && tentatives < nbTentatives) {
            synchronized (this.coJoueurs) {
                for (Connexion co : this.coJoueurs) {
                    if (!co.isReady()) {
                        ready = false;
                    }
                }
            }
            sleep(500);
            tentatives++;
            synchronized (this.reponses) {
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
        synchronized (this.reponses) {
            taille = this.reponses.size();
        }
        if (taille != this.nbJoueurs) {
            synchronized (this.coJoueurs) {
                for (Connexion co : this.coJoueurs) {
                    co.isReady();
                }
            }
        } else {
            this.aSyncRep = true;
        }
    }

    /**
     * Envoie un message au joueur par la connexion spécifiée
     *
     * @param co Connexion utilisée
     * @param msg Message envoyé
     * @throws IOException
     */
    public void ecrireMessage(Connexion co, String msg) throws IOException {
        co.ecrireMessage(msg);
    }

    /**
     * Envoie un message à tous les joueurs du salon
     *
     * @param msg Message envoyé
     * @throws IOException
     */
    public void ecrireMessageAll(String msg) throws IOException {
        synchronized (this.coJoueurs) {
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
