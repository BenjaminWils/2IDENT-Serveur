package serveur;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renseigne un joueur sur l'état des salons
 * @author Benjamin
 */
public class FileAttente extends Thread {
    
    private Socket socJoueur;
    private PrintWriter out;
    
    public FileAttente(Socket soc, PrintWriter out) throws IOException {
        this.socJoueur = soc;
        this.out = out;
    }
    
    @Override
    public void run() {
        try {
            // Tant que non interrompu
            while (!Thread.currentThread().isInterrupted()) {
                // Construction de la chaîne
                String chaine = "::AGAIN::Avec combien de joueurs voulez-vous jouer ?::REPONSES";
                for (int i = 2; i < 7 ; i++) {
                    int tmp = this.existSalonTaille(i);
                    // Salon vide ou non existant pour la taille i
                    if (tmp == 0) {
                        chaine += "::" + String.valueOf(i - 1) + " (aucun salon ouvert)";
                    }
                    else {
                        // Nombre de joueurs manquants pour remplir le salon
                        chaine += "::" + String.valueOf(i - 1) + " (" + String.valueOf(tmp) + " joueur(s) manquant(s))";
                    }
                }
                chaine += "::END";
                // Envoie du message au joueur
                this.ecrireMessage(chaine);
                // Attend 1 seconde avant d'envoyer à nouveau
                sleep(1000);
            }
        }
        catch (InterruptedException ex) {
            System.out.println("File interrompue");
        }
        catch (IOException e) {
            System.out.println("Client fermé");
        }
    }
    
    public void ecrireMessage(String msg) {
        out.println(msg);
    }
    
    /**
     * Fonction qui recherche les salons de taille spécifiée
     * @param n
     * @return 0 si le salon de taille n n'existe pas ou est plein, le nb de joueurs manquants minimal sinon
     * @throws IOException 
     */
    public int existSalonTaille(int n) throws IOException {
        // Retourne 0 si le salon de taille n n'existe pas ou est plein
        // Retourne le nb de joueurs manquants minimal sinon
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
}
