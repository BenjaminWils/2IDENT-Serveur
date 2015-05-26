package serveur;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;
import jeu.TypeRole;
import org.json.simple.*;
import org.json.simple.parser.*;


/**
 * Classe qui gère la communication entre le serveur et le joueur, pour un joueur donné
 * @author Benjamin
 */
public class Connexion extends Thread {
    private int compteurPoints;
    private int compteurTotal;
    
    private Socket cl;
    private BufferedReader in;
    private PrintWriter out;

    private int mode;
    private int nbJoueurs;
    public String nomJoueur;
    public TypeRole role;
    public Salon salle;
    public String currentMsg;
    
    /**
     * Constructeur : Initialise les flux d'entrée/sortie du socket, et le chargement des questions
     * @param cl Socket du client
     * @throws IOException 
     */
    public Connexion(Socket cl) throws IOException {
        this.cl = cl;
        this.in = new BufferedReader(new InputStreamReader(this.cl.getInputStream()));
        this.out = new PrintWriter(cl.getOutputStream(), true);
        this.role = TypeRole.Neutre;
    }

    @Override
    public void run() {
        try {
            // Récupère le pseudo du joueur
            String pseudo, msg;
            boolean tmp = true;
            while (tmp) {
                msg = this.in.readLine();
                testSocket(msg);
                System.out.println("Réception : " + msg);
                if (!msg.matches("pseudo::.*")) {
                    this.ecrireMessage("error::1::Pseudo attendu");
                } else {
                    if (msg.matches("pseudo::validation::.*")) {
                        pseudo = msg.split("::",3)[2];
                        synchronized (Serveur.listeConnexions) {
                            if (Serveur.isPseudoPresent(pseudo) || pseudo.length() > 20 || pseudo.equals("[@Moderation]")) {
                                this.ecrireMessage("pseudo::dispo::ko");
                            }
                            else {
                                this.nomJoueur = pseudo;
                                tmp = false;
                            }
                        }
                    } else {
                        this.ecrireMessage("error::2::Validation attendue");
                    }
                    
                }
            }
            this.ecrireMessage("pseudo::dispo::ok");
            
            // Envoi liste salons
            
            this.ecrireMessage("salon::liste::" + Serveur.listerSalons().toJSONString());
            
            /*JSONParser pars = new JSONParser();
            JSONArray listeSalons = (JSONArray) pars.parse("");
            Iterator it = listeSalons.iterator();
            while (it.hasNext()) {
                JSONObject salon = (JSONObject) it.next();
                JList li = new JList();
                DefaultListModel model = new DefaultListModel();
                model.addElement("Test");
                model.addElement("Test1");
                model.addElement("Test2");
                li.setModel(model);
            }*/
            boolean fin = false;
            while (!fin) {
                tmp = true;
                Salon salle = null;

                while (tmp) {
                    msg = this.in.readLine();
                    testSocket(msg);
                    System.out.println("Réception : " + msg);
                    if (!msg.matches("salon::.*")) {
                        this.ecrireMessage("salon::erreur::Type Salon attendu");
                    } else {
                        // Prise en compte choix client : Rafraichissement/Connexion/Création salon
                        if (msg.matches("salon::refresh")) {
                            this.ecrireMessage("salon::liste::" + Serveur.listerSalons().toJSONString());
                        } else if (msg.matches("salon::connection::.*")) {
                            synchronized (Serveur.salons) {
                                String nomSalon = msg.split("::",3)[2];
                                if (Serveur.isSalonPresent(nomSalon)) {
                                    for (Salon sal : Serveur.salons) {
                                        if (sal.nom != null && sal.nom.equals(nomSalon)) {
                                            salle = sal;
                                        }
                                    }
                                    if (salle.ajoutJoueur(this)) {
                                        this.ecrireMessage("salon::connection::ok");
                                        this.salle = salle;
                                        tmp = false;
                                    } else {
                                        this.ecrireMessage("salon::connection::erreur::Salon plein !");
                                    }
                                } else {
                                    this.ecrireMessage("salon::connection::erreur::Salon inexistant !");
                                }
                            }
                        } else if (msg.matches("salon::creation::.*")) {
                            String nomSalon = msg.split("::",4)[2];
                            int nbJoueurs = Integer.valueOf(msg.split("::",4)[3]);
                            synchronized (Serveur.salons) {
                                if (nomSalon.length() > 15) {
                                    this.ecrireMessage("salon::creation::erreur::Nom de salon trop long !");
                                } else if ("".equals(nomSalon)) {
                                    this.ecrireMessage("salon::creation::erreur::Nom de salon manquant !");
                                } else if (nomSalon.contains("  ")) {
                                    this.ecrireMessage("salon::creation::erreur::Nom de salon incorrect !");
                                } else if (nbJoueurs < 3 || nbJoueurs > 10) {
                                    this.ecrireMessage("salon::creation::erreur::Nombre de joueurs invalide !");
                                } else if (Serveur.isSalonPresent(nomSalon)) {
                                    // On signale au client que le nom du salon n'est pas disponible
                                    this.ecrireMessage("salon::creation::erreur::Nom de salon déjà pris !");
                                } else {
                                    // On nettoie les salons vides
                                    // Si il n'y a pas de salon adapté dispo, on en crée un
                                    salle = new Salon(nomSalon, nbJoueurs);
                                    // On y ajoute le joueur de la connexion
                                    salle.ajoutJoueur(this);
                                    // Et on l'ajoute à la liste des salons
                                    Serveur.salons.add(salle);
                                    this.salle = salle;

                                    tmp = false;

                                    // Envoi du message de dispo du salon
                                    this.ecrireMessage("salon::creation::ok");
                                    // Puis on le démarre
                                    salle.start();
                                }
                            }
                        } else {
                            this.ecrireMessage("salon::erreur::Rafraichissement/Connexion/Création salon attendu");
                        }
                    }
                }

                // A partir d'ici, le client est connecté à un salon
                // Ecoute des messages du client
                msg = this.in.readLine();
                testSocket(msg);
                this.currentMsg = msg;
                System.out.println("Réception depuis " + this.nomJoueur + " : " + msg);
                while ((msg != null) && (this.salle != null) && (!msg.matches("connection::fin::.*"))) {
                    if (msg.matches("chat::.*")) {
                        if (!msg.equals("chat::")) {
                            String contenu = msg.split("::",2)[1];
                            this.salle.ecrireMessageAll("chat::" + this.nomJoueur + "::[" + new SimpleDateFormat("HH:mm:ss").format(new Date()) + "] " + contenu);
                        }
                    } else if (msg.matches("READY")) {
                        this.salle.repondre(this);
                    } else if (msg.matches("jeu::.*")) {
                        this.salle.semaphore.release();
                    } else if (msg.equals("salon::fin")) {
                        synchronized(this.salle.coJoueurs) {
                            this.salle.coJoueurs.remove(this);
                            this.salle.semaphore.release();
                        }
                        this.salle = null;
                    } else {
                        this.ecrireMessage("erreur::Type inattendu");
                    }
                    if ((msg != null) && (this.salle != null)) {
                        msg = this.in.readLine();
                        testSocket(msg);
                        this.currentMsg = msg;
                        System.out.println("Réception depuis " + this.nomJoueur + " : " + msg);
                    }
                }
                if (msg.matches("connection::fin::.*")) {
                    fin = true;
                }
            }
        }
        catch (SocketException ex) {
            if (ex.getMessage().equals("Connection reset")) {
                System.out.println("Connexion du client fermée");
            }
            else {
                System.out.println(ex.getMessage());
            }
        }
        catch (Exception ex) {
            System.out.println("Connexion :" + ex.toString());
        }
        finally {
            try {
                if (this.salle != null) {
                    if (this.salle.semaphore.hasQueuedThreads()) {
                        this.salle.semaphore.release();
                    }
                }
                cl.close();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
    
    /**
     * Procédure permettant l'envoi d'un message au joueur
     * @param msg Message à envoyer
     */
    public void ecrireMessage(String msg) {
        System.out.println("Envoi : " + msg);
        out.println(new String(msg.getBytes(), Charset.forName("UTF-8")));
    }
    
    /**
     * Procédure qui déclenche une exception SocketException si l'objet passé en paramètre n'a pas d'adresse
     * @param m Objet quelconque
     * @throws SocketException 
     */
    public void testSocket(Object m) throws SocketException {
        if (m == null) {
            throw new SocketException("Connexion client fermée !");
        }
    }
    
    /**
     * Fonction testant si un socket est encore vivant
     * @return True si le socket est fermé, False sinon
     */
    public boolean isSocketClosed() {
        boolean result = false;
        try {
            // Pour tester si un socket est fermé
            // 1. Tester si le readLine du PrintWriter == null
            // -> Problème, bloquant si ce n'est pas le cas
            // 2. Tester si écrire dans le flux provoque une erreur
            OutputStream out = this.cl.getOutputStream();
            out.write("ALIVE\n".getBytes());          
        } catch (IOException e) {
            result = true;
        }
        return result;
    }
    
    /**
     * Fonction qui recherche les salons de taille spécifiée
     * @param n
     * @return 0 si le salon de taille n n'existe pas ou est plein, le nb de joueurs manquants minimal sinon
     * @throws IOException 
     */
    public int existSalonTaille(int n) throws IOException {
        int result = 0;
        synchronized(Serveur.salons) {
            int min = 7;
            for (Salon s : Serveur.salons) {
                int jManquants = s.getNbJoueursMax() - s.getNbJoueurs();
                if (s.getNbJoueursMax() == n && jManquants > 0) {
                    if (jManquants < min) {
                        min = jManquants;
                    }
                }
            }
            if (min != 7) {
                result = min;
            }
        }
        return result;
    }
    
    /**
     * Procédure qui détermine si le client est prêt à être sollicité
     * e.g. s'il n'est pas en cours de traitement
     * @return True si le client est prêt, False sinon
     * @throws IOException
     * @throws SocketException 
     */
    public boolean isReady() throws IOException, SocketException {
        boolean result = false;
        this.ecrireMessage("READY");
        String line = this.in.readLine();
        if (line == null) {
            throw new SocketException("Socket client fermé");
        }
        if (line.contains("READY")) {
            result = true;
        }
        return result;
    }
    
    public Socket getSocket() {
        return this.cl;
    }
    
    public BufferedReader getBufferedReader() {
        return this.in;
    }
    
    public String getNomJoueur() {
        return this.nomJoueur;
    }
}
