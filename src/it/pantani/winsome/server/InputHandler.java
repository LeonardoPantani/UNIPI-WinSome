/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server;

import it.pantani.winsome.server.entities.WinSomeSession;
import it.pantani.winsome.server.entities.WinSomeUser;
import it.pantani.winsome.rmi.WinSomeCallback;
import it.pantani.winsome.other.ConfigManager;
import it.pantani.winsome.other.Utils;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

import static it.pantani.winsome.other.Utils.getFormattedDate;

/**
 * Classe che gestisce l'input di richieste del server. Permette di eseguire alcuni comandi per vedere l'andamento del server.
 * E' ad utilizzo amministrativo e di gestione, sarà poco commentata perché non molto rilevante.
 */
public class InputHandler implements Runnable {
    private final ConfigManager config;

    private volatile boolean stop;

    String default_reason_transaction;

    public InputHandler(ConfigManager config) throws ConfigurationException {
        this.config = config;
        stop = false;

        validateAndSavePreferences();
    }

    /**
     * Processo che viene eseguito all'avvio del server che aspetta l'input dell'utente.
     */
    public void run() {
        Scanner in = new Scanner(System.in);

        while(!stop) {
            String raw_request = in.nextLine();
            if(raw_request.equals("")) continue; // se l'utente ha premuto invio continuo
            String[] temp = raw_request.split(" ");

            String request = temp[0];
            String[] arguments = new String[temp.length-1];
            System.arraycopy(temp, 1, arguments, 0, temp.length - 1);

            switch (request) {
                case "kickclient": { // espelle un client
                    if (arguments.length != 1) {
                        System.err.println("[!] Utilizzo comando errato: " + request + " <porta_client>");
                        break;
                    }

                    int port;
                    try {
                        port = Integer.parseInt(arguments[0]);
                    } catch (NumberFormatException e) {
                        System.err.println("[!] Utilizzo comando errato: " + request + " <porta client>");
                        break;
                    }

                    kickClient(port);
                    break;
                }
                case "kickallclients": { // espelle tutti i client
                    kickAllClients();
                    break;
                }
                case "listclients": { // mostra una lista di client connessi (con username e data di login)
                    listClients();
                    break;
                }
                case "listusers": { // mostra una lista di utenti
                    listUsers();
                    break;
                }
                case "listfollowers": { // mostra i followers di un certo utente
                    if (arguments.length != 1) {
                        System.err.println("[!] Utilizzo comando errato: " + request + " <username>");
                        break;
                    }
                    listFollowers(arguments[0]);
                    break;
                }
                case "listfollowing": { // mostra gli utenti che un certo utente segue
                    if (arguments.length != 1) {
                        System.err.println("[!] Utilizzo comando errato: " + request + " <username>");
                        break;
                    }
                    listFollowing(arguments[0]);
                    break;
                }
                case "stopserver": { // ferma il server (è l'unico modo che c'è perché i dati vengano salvati!)
                    stopServer(in);
                    break;
                }
                case "addfollower": { // aggiunge un follower (può anche non esistere)
                    if (arguments.length != 2) {
                        System.err.println("[!] Argomento non valido: addfollower <username> <nuovo follower>");
                        break;
                    }
                    addFollower(arguments[0], arguments[1]);
                    break;
                }
                case "removefollower": { // rimuove un follower
                    if (arguments.length != 2) {
                        System.err.println("[!] Argomento non valido: removefollower <username> <follower da rimuovere>");
                        break;
                    }
                    removeFollower(arguments[0], arguments[1]);
                    break;
                }
                case "addfollowing": { // fa seguire all'utente un altro utente (può anche non esistere)
                    if (arguments.length != 2) {
                        System.err.println("[!] Argomento non valido: addfollowing <username> <nuovo seguito>");
                        break;
                    }
                    addFollowing(arguments[0], arguments[1]);
                    break;
                }
                case "removefollowing": { // non fa seguire più all'utente un altro utente
                    if (arguments.length != 2) {
                        System.err.println("[!] Argomento non valido: removefollowing <username> <following da rimuovere>");
                        break;
                    }
                    removeFollowing(arguments[0], arguments[1]);
                    break;
                }
                case "changebal": { // cambia il bilancio di un utente
                    if (arguments.length != 2) {
                        System.err.println("[!] Argomento non valido: changebal <username> <cambiamento>");
                        break;
                    }

                    try {
                        changeBalance(arguments[0], Double.parseDouble(arguments[1]));
                    } catch(NumberFormatException e) {
                        System.err.println("[!] Argomento non valido: changebal <username> <cambiamento>");
                    }
                    break;
                }
                case "help": { // mostra una lista di comandi
                    help();
                    break;
                }
                default: { // se viene digitato un comando non valido
                    unknownCommand();
                }
            }
        }
    }

    private void kickClient(int client_port) {
        boolean found_client = false;

        for(Socket x : ServerMain.socketsList) {
            if(x.getPort() == client_port) {
                found_client = true;
                try {
                    x.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ServerMain.socketsList.remove(x);
            }
        }

        if(found_client) {
            System.out.println("> Connessione chiusa!");
        } else {
            System.err.println("[!] Impossibile trovare un client con porta: " + client_port);
        }
    }

    private void kickAllClients() {
        for(Socket x : ServerMain.socketsList) {
            try {
                x.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ServerMain.socketsList.remove(x);
        }

        System.out.println("> Connessione a tutti client chiusa.");
    }

    private void listClients() {
        int numClients = ServerMain.socketsList.size();
        if(numClients == 0) {
            System.out.println("> Nessun client connesso");
            return;
        }

        System.out.println("> LISTA CLIENT CONNESSI (" + numClients + "):");
        for(Socket x : ServerMain.socketsList) {
            System.out.print("> Client " + x.getInetAddress() + ":" + x.getPort());
            // ottengo la sessione dal socket del client
            WinSomeSession user_session = null;
            for (WinSomeSession wss : ServerMain.sessionsList.values()) {
                if(wss.getSessionSocket() == x) {
                    user_session = wss;
                }
            }
            // se l'ho trovata allora l'utente è collegato e lo stampo, altrimenti mostro "nessun login"
            if(user_session != null) {
                System.out.println(" (utente connesso: " + user_session.getUsername() + " | data login: " + getFormattedDate(user_session.getTimestamp()) + ")");
            } else {
                System.out.println(" (nessun login)");
            }
        }
    }

    private void listUsers() {
        SocialManager s = ServerMain.social;
        int numUsers = s.getUserCount();
        if(numUsers == 0) {
            System.out.println("> Non ci sono utenti registrati.");
            return;
        }
        Set<String> user_tags_list;

        System.out.println("> LISTA UTENTI REGISTRATI (" + numUsers + "):");
        for(WinSomeUser u : s.getUserList().values()) {
            System.out.print("- " + u.getUsername());
            user_tags_list = u.getTags_list();

            System.out.print(" | Bilancio: " + s.getFormattedCurrency(s.getWalletByUsername(u.getUsername()).getBalance()));
            System.out.print(" | Data reg.: " + Utils.getFormattedDate(u.getCreationDate()));
            System.out.print(" | Tags: ");
            if(user_tags_list.size() != 0) {
                System.out.println(user_tags_list);
            } else {
                System.out.println("(nessun tag specificato)");
            }
        }
    }

    private void listFollowers(String user) {
        SocialManager s = ServerMain.social;
        if(!s.findUser(user)) {
            System.out.println("[!] Utente '" + user + "' non valido.");
            return;
        }
        ArrayList<String> user_followers_list = s.getFollowers(user);
        if(user_followers_list == null) {
            System.out.println("> '" + user + "' non e' seguito da alcun utente.");
            return;
        }

        System.out.println("> UTENTI CHE SEGUONO '" + user.toUpperCase(Locale.ROOT) + "' (" + user_followers_list.size() + "):");
        for(String u : user_followers_list) {
            System.out.println("* " + u);
        }
    }

    private void listFollowing(String user) {
        SocialManager s = ServerMain.social;
        if(!s.findUser(user)) {
            System.out.println("[!] Utente '" + user + "' non valido.");
            return;
        }
        ArrayList<String> user_following_list = s.getFollowing(user);
        if(user_following_list == null) {
            System.out.println("> '" + user + "' non segue alcun utente.");
            return;
        }

        System.out.println("> UTENTI CHE '" + user.toUpperCase(Locale.ROOT) + "' SEGUE (" + user_following_list.size() + "):");
        for(String u : user_following_list) {
            System.out.println("* " + u);
        }
    }

    private void addFollower(String user, String new_follower) {
        try {
            WinSomeCallback.notifyFollowerUpdate(user, "+" + new_follower);
            System.out.println("> Aggiunto a '" + user + "' il follower '" + new_follower + "'");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void removeFollower(String user, String to_remove_follower) {
        try {
            WinSomeCallback.notifyFollowerUpdate(user, "-" + to_remove_follower);
            System.out.println("> Rimosso a '" + user + "' il follower '" + to_remove_follower + "'");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void addFollowing(String user, String new_following) {
        SocialManager s = ServerMain.social;

        s.addFollower(new_following, user);
        s.addFollowing(user, new_following);
        System.out.println("> Ora l'utente '" + user + "' segue '" + new_following + "'");
    }

    private void removeFollowing(String user, String to_remove_following) {
        SocialManager s = ServerMain.social;

        s.removeFollower(to_remove_following, user);
        s.removeFollowing(user, to_remove_following);
        System.out.println("> Ora l'utente '" + user + "' non segue piu' '" + to_remove_following + "'");
    }

    private void changeBalance(String user, double edit) {
        SocialManager s = ServerMain.social;
        if(s.getUser(user) == null) {
            System.err.println("[!] L'utente '" + user + "' non esiste");
            return;
        }

        ConfigManager c = ServerMain.config;
        String reason = c.getPreference("default_reason_transaction");
        if(reason == null) reason = "SYSTEM";
        double newBalance = s.getWalletByUsername(user).changeBalance(edit, reason);
        System.out.println("> L'utente '" + user + "' ha ora un bilancio di " + s.getFormattedCurrency(newBalance));
    }

    private void stopServer(Scanner in) {
        String check;

        System.out.print("> Sei sicuro di voler terminare il server? (S/N): ");
        check = in.nextLine();
        if(check.equalsIgnoreCase("S")) {
            System.out.println("> Server in arresto...");
            stop = true;
            try {
                // chiudendolo da qui, nel main sarà lanciata una IOException che mi permetterà di uscire dal ciclo infinito
                ServerMain.serverSocket.close();
            } catch (IOException ignored) { }
        }
    }

    private void help() {
        ConfigManager c = ServerMain.config;
        System.out.println("> LISTA COMANDI:");
        System.out.println("kickclient <porta>                   - Chiude forzatamente la connessione con un client");
        System.out.println("kickallclients                       - Chiude forzatamente la connessione a tutti i client");
        System.out.println("listclients                          - Mostra la lista di clients connessi");
        System.out.println("listusers                            - Mostra la lista degli utenti registrati");
        System.out.println("listfollowers <utente>               - Mostra gli utenti che seguono <utente>");
        System.out.println("listfollowing <utente>               - Mostra gli utenti seguiti da <utente>");
        System.out.println("addfollower <utente> <nuovo>         - Aggiunge a <utente> il follower <nuovo>");
        System.out.println("removefollower <utente> <follower>   - Rimuove a <utente> il follower <follower>");
        System.out.println("addfollowing <utente> <nuovo>        - Fa seguire a <utente> l'utente <nuovo>");
        System.out.println("removefollowing <utente> <following> - Fa smettere a <utente> di seguire l'utente <following>");
        System.out.println("changebal <utente> <cambiamento>     - Aggiunge una transazione di <cambiamento> " + c.getPreference("currency_name_plural") + " a <utente>");
        System.out.println("stopserver                           - Termina il server");
        System.out.println("help                                 - Mostra questa schermata");
    }

    private void unknownCommand() {
        System.out.println("> Comando sconosciuto, scrivi 'help' per una lista di comandi.");
    }

    /**
     * Verifica che le preferenze specificate nel file di configurazione siano corrette facendo vari controlli. Se
     * anche una sola opzione è errata allora lancia un'eccezione.
     * @throws ConfigurationException se una opzione della configurazione è errata
     */
    private void validateAndSavePreferences() throws ConfigurationException {
        // controllo che il motivo di default di una transazione non sia nullo
        default_reason_transaction = config.getPreference("default_reason_transaction");
        if (default_reason_transaction == null) {
            throw new ConfigurationException("valore 'default_reason_transaction' non valido (non e' presente nel file di configurazione)");
        }
    }
}
