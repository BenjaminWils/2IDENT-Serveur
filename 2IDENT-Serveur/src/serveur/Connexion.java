package serveur;

import java.io.*;
import static java.lang.Thread.sleep;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    
    /**
     * Constructeur : Initialise les flux d'entrée/sortie du socket, et le chargement des questions
     * @param cl Socket du client
     * @throws IOException 
     */
    public Connexion(Socket cl) throws IOException {
        this.cl = cl;
        this.in = new BufferedReader(new InputStreamReader(this.cl.getInputStream()));
        this.out = new PrintWriter(cl.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            // Récupère le pseudo du joueur
            String pseudo, msg;
            boolean tmp = true;
            while (tmp) {
                msg = this.in.readLine();
                System.out.println(msg);
                if (!msg.matches("pseudo::.*")) {
                    this.ecrireMessage("error::1::Pseudo attendu");
                } else {
                    if (msg.matches("pseudo::validation::.*")) {
                        pseudo = msg.split("pseudo::validation::")[1];
                        synchronized (Serveur.listeConnexions) {
                            if (Serveur.isPseudoPresent(pseudo) || pseudo.length() > 20) {
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
            
            this.ecrireMessage("salon::liste" + Serveur.listerSalons());
            
            tmp = true;
            
            while (tmp) {
                msg = this.in.readLine();
                if (!msg.matches("salon::.*")) {
                    this.ecrireMessage("error::1::Salon attendu");
                } else {
                    // Prise en compte choix client : Rafraichissement/Connexion/Création salon
                    if (msg.matches("salon::refresh")) {
                        this.ecrireMessage("salon::liste" + Serveur.listerSalons());
                    } else if (msg.matches("salon::connection::.*")) {
                        
                    } else if (msg.matches("salon::creation::.*")) {
                        String nomSalon = msg.split("::")[2];
                        int nbJoueurs = Integer.valueOf(msg.split("::")[3]);
                        synchronized(Serveur.salons) {
                            // On nettoie les salons vides
                            // Si il n'y a pas de salon adapté dispo, on en crée un
                            Salon sa = new Salon(nomSalon,nbJoueurs);
                            // On y ajoute le joueur de la connexion
                            sa.ajoutJoueur(this);
                            // Et on l'ajoute à la liste des salons
                            Serveur.salons.add(sa);
                            // Puis on le démarre
                            sa.start();
                        }
                    } else {
                        this.ecrireMessage("error::2::Rafraichissement/Connexion/Création salon attendu");
                    }
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
        System.out.println(msg);
        out.println(msg);
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
        this.ecrireMessage("::READY::");
        String line = this.in.readLine();
        if (line == null) {
            throw new SocketException("Socket client fermé");
        }
        if (line.contains("::READY::")) {
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
