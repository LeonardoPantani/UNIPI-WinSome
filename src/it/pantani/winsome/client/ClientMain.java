/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.client;

import it.pantani.winsome.shared.ConfigManager;
import it.pantani.winsome.shared.Utils;
import it.pantani.winsome.client.rmi.NotifyEvent;
import it.pantani.winsome.shared.rmi.NotifyEventInterface;
import it.pantani.winsome.shared.rmi.WinSomeCallbackInterface;
import it.pantani.winsome.shared.rmi.WinSomeServiceInterface;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Classe del Client del social WinSome. Gestisce tutte le operazioni che può svolgere il thin client, che come dice
 * il nome, si occupa di inviare la richiesta e ricevere una risposta. Quasi tutte le operazioni non fanno altro che
 * inviare la query "grezza" e aspettare la risposta del server, non sono svolte verifiche sugli argomenti lato client.
 * Le uniche query un po' più "complesse" sono la register, login e logout, che hanno dei controlli supplementari sulla
 * risposta del server. Per esempio, l'esito del login viene controllato dal client per verificare che quest'ultimo sia
 * finito con successo per poter registrare il client al callback. Register e logout fanno controlli simili.
 */
public class ClientMain {
    public static ConfigManager config;

    public static String server_address;
    public static int server_port;
    public static int server_rmi_port;
    public static String server_rmi_registry_name;
    public static int client_rmi_callback_port;
    public static String server_rmi_callback_registry_name;
    public static String multicast_server_address;
    public static int multicast_server_port;

    public static ArrayList<String> followersList = new ArrayList<>();

    public static void main(String[] args) {
        // leggo le preferenze dal file di configurazione
        System.out.println("> Lettura dati dal file di configurazione...");
        try {
            config = new ConfigManager(false);
        } catch(IOException e) {
            System.err.println("[!] Lettura fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }

        // validazione variabili
        try {
            validateAndSavePreferences();
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione fallita, " + e.getLocalizedMessage());
            return;
        }

        // validazione ok, stampo le variabili
        System.out.println("> Indirizzo server: " + server_address);
        System.out.println("> Porta server: " + server_port);
        System.out.println("> Porta RMI server: " + server_rmi_port);
        System.out.println("> Nome registry RMI server: " + server_rmi_registry_name);
        System.out.println("> Porta RMI callback client: " + client_rmi_callback_port);
        System.out.println("> Nome registry RMI callback client: " + server_rmi_callback_registry_name);
        System.out.println("> Indirizzo multicast notifiche: " + multicast_server_address);
        System.out.println("> Porta multicast notifiche: " + multicast_server_port);

        // preparo il collegamento col server
        Socket socket;
        Scanner reader = new Scanner(System.in);

        WinSomeCallbackInterface server = null;
        NotifyEventInterface callbackstub;
        NotifyEventInterface callbackobj = null;
        String username = null;
        String raw_request = "";
        String request = "";
        boolean reqFailed = false;
        boolean connLost;

        // ascolto aggiornamenti wallet
        WalletUpdateManager wum;
        try {
            wum = new WalletUpdateManager(multicast_server_address, multicast_server_port);
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione WalletUpdateManager fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }
        Thread walletCheckerThread = new Thread(wum);
        walletCheckerThread.start();

        // nel caso la connessione col server cadesse, apparirà un prompt dove è chiesto se provare a ricollegarsi
        loopesterno:
        while(true) {
            // mi collego al server
            try {
                socket = new Socket(server_address, server_port);
            } catch(IOException e) {
                System.err.print("[!] Impossibile connettersi al server. Riprovare a collegarsi? (S/N): ");
                if(Utils.readFromConsole(reader).equalsIgnoreCase("S")) {
                    continue;
                } else {
                    connLost = true;
                    break;
                }
            }

            System.out.println("> Connessione col server stabilita. Sto comunicando sulla porta " + socket.getLocalPort());

            // RMI (register)
            Registry registry;
            WinSomeServiceInterface stub;
            try {
                registry = LocateRegistry.getRegistry(server_address, server_rmi_port);
                stub = (WinSomeServiceInterface) registry.lookup(server_rmi_registry_name);
            } catch (RemoteException | NotBoundException e) {
                System.err.println("[!] Errore RMI. Motivo: " + e.getLocalizedMessage());
                return;
            }

            // preparo i reader e writer dopo il primo collegamento al server
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                connLost = false;
                // ciclo per ogni richiesta fatta dall'utente
                while(true) {
                    System.out.print("> ");
                    // questo parametro è impostato a vero quando la connessione col server cade e si risponde "S" al prompt per ricollegarsi
                    // in questo modo l'ultima richiesta è ricopiata per evitare che debba essere richiesta (solo per register e login)
                    if(reqFailed) {
                        System.out.print(raw_request);
                        Utils.readFromConsole(reader); // ignoro eventuale input dell'utente, esso dovrà premere INVIO
                        // se la richiesta è fallita in precedenza devo fare l'unexport dell'oggetto se era già istanziato
                        // se non ci fosse questo controllo, il client non terminerebbe correttamente
                        if(callbackobj != null) {
                            try {
                                UnicastRemoteObject.unexportObject(callbackobj, false);
                            } catch(NoSuchObjectException ignored) { }
                        }
                        reqFailed = false;
                    } else {
                        // se l'ultima richiesta non è fallita semplicemente ricevo il comando dall'utente
                        // se il comando è vuoto lo ignoro
                        raw_request = Utils.readFromConsole(reader);
                        if(raw_request.equals("")) continue;
                    }
                    String[] temp = raw_request.split(" ");
                    request = temp[0];
                    String[] arguments = new String[temp.length-1];
                    System.arraycopy(temp, 1, arguments, 0, temp.length - 1);

                    // una volta effettuata la divisione tra tipo operazione e argomenti, gestisco propriamente la richiesta
                    switch (request) {
                        case "stopclient": {
                            socket.close();
                            out.close();
                            in.close();
                            break loopesterno; // per terminare tutta l'esecuzione
                        }
                        case "register": {
                            if (arguments.length < 2 || arguments.length > 7) {
                                System.err.println("[!] Comando errato, usa: register <username> <password> [lista di tag, max 5]");
                                break;
                            }
                            ArrayList<String> tags_list = new ArrayList<>(Arrays.asList(arguments).subList(2, arguments.length));

                            String server_response = stub.register(arguments[0], arguments[1], tags_list);
                            if(server_response.equals(Utils.SOCIAL_REGISTRATION_SUCCESS)) {
                                System.out.println("> Registrazione dell'utente '" + arguments[0] + "' completata.");
                            } else if(server_response.equals(Utils.SOCIAL_REGISTRATION_FAILED)) {
                                System.out.println("> Non e' stato possibile registrare l'utente '" + arguments[0] + "' perché quell'username e' gia' in uso.");
                            } else {
                                System.out.println("> Non e' stato possibile registrare l'utente '" + arguments[0] + "'.");
                            }
                            break;
                        }
                        case "login": {
                            Utils.send(out, raw_request);
                            String response = Utils.receive(in);
                            if (response.equalsIgnoreCase(Utils.SOCIAL_LOGIN_SUCCESS)) { // se il login ha successo registro il client per il callback
                                username = arguments[0];
                                // registrazione callback per le notifiche riguardo l'aggiornamento della lista follower locale
                                try {
                                    Registry callbackregistry = LocateRegistry.getRegistry(server_address, client_rmi_callback_port);
                                    server = (WinSomeCallbackInterface) callbackregistry.lookup(server_rmi_callback_registry_name);
                                    callbackobj = new NotifyEvent();
                                    callbackstub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackobj, 0);
                                } catch (Exception e) {
                                    System.err.println("[!] Errore RMI callback. Motivo: " + e.getLocalizedMessage());
                                    break;
                                }

                                try {
                                    server.registerForCallback(username, callbackstub);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }

                                // al login ricevo la lista dei follower, per i successivi aggiornamenti si farà uso dell'RMI
                                followersList = stub.initializeFollowerList(username, arguments[1]);
                                System.out.println("> Login dell'utente '" + username + "' effettuato.");
                            } else {
                                System.out.println("[Server]> " + response);
                            }
                            break;
                        }
                        case "logout": {
                            Utils.send(out, raw_request);
                            String response = Utils.receive(in);
                            // SE sono connesso ad un server & SE il logout ha successo ALLORA rimuovo il client dalla lista del callback
                            if (server != null && response.equals(Utils.SOCIAL_LOGOUT_SUCCESS)) {
                                System.out.println("> Logout dell'utente '" + username + "' effettuato");
                                server.unregisterForCallback(username);
                                UnicastRemoteObject.unexportObject(callbackobj, false);
                                callbackobj = null;
                                username = null;
                            } else {
                                System.out.println("[Server]> " + response);
                            }
                            break;
                        }
                        case "listfollowers": { // questa operazione è gestita lato client, quindi i controlli sono implementati qui
                            if(username == null) {
                                System.out.println("[!] Non hai effettuato il login");
                                break;
                            }
                            if (followersList.size() == 0) {
                                System.out.println("> Nessun utente ti segue.");
                                break;
                            }

                            System.out.println("> LISTA FOLLOWERS (" + followersList.size() + "):");
                            for (String u : followersList) {
                                System.out.println("* " + u);
                            }
                            System.out.print("\n");
                            break;
                        }
                        case "help": { // mostra una lista dei comandi disponibili del client
                            System.out.println("> LISTA COMANDI:");
                            System.out.println("login <username> <password>        - Effettua il login al social WinSome");
                            System.out.println("logout                             - Effettua il logout dal social WinSome");
                            System.out.println("listfollowers                      - Mostra una lista dei propri followers");
                            System.out.println("listusers                          - Mostra utenti con almeno un tag in comune");
                            System.out.println("listfollowing                      - Mostra una lista degli utenti che si segue");
                            System.out.println("blog                               - Mostra tutti post nel proprio blog");
                            System.out.println("showfeed                           - Mostra tutti i post nel tuo feed");
                            System.out.println("wallet                             - Mostra il tuo bilancio e transazioni");
                            System.out.println("walletbtc                          - Mostra il tuo bilancio in Bitcoin");
                            System.out.println("follow <utente>                    - Fa seguire un utente");
                            System.out.println("unfollow <utente>                  - Fa smettere di seguire un utente");
                            System.out.println("rewin <id post>                    - Effettua il rewin di un post");
                            System.out.println("post \"<titolo>\" \"<contenuto>\"      - Crea un post");
                            System.out.println("rate <id post> <+1/-1>             - Valuta un post nel proprio feed");
                            System.out.println("showpost <id post>                 - Mostra un post");
                            System.out.println("comment <id post> <testo>          - Commenta un post nel proprio feed");
                            System.out.println("delete <id post>                   - Elimina un proprio post");
                            System.out.println("help                               - Mostra questa schermata");
                            break;
                        }
                        // insieme di operazioni senza controlli sugli argomenti
                        case "listusers":
                        case "listfollowing":
                        case "blog":
                        case "showfeed":
                        case "wallet":
                        case "walletbtc":
                        case "follow":
                        case "unfollow":
                        case "rewin":
                        case "rate":
                        case "showpost":
                        case "comment":
                        case "delete":
                        case "post": {
                            Utils.send(out, raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        default: {
                            Utils.send(out, raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                        }
                    }
                }
            } catch (IOException e) {
                // se si verifica un errore IO probabilmente il server è stato spento oppure ha avuto un errore
                System.err.print("[!] Connessione al server perduta. Riprovare a collegarsi? (S/N): ");
                if(!Utils.readFromConsole(reader).equalsIgnoreCase("S")) { // se non è uguale a S
                    connLost = true; // per entrare nell'if sotto (fuori dal while)
                    break;
                } else {
                    // se il collegamento fallisce e stavamo facendo register o login, al prossimo collegamento riapparirà la query dell'utente senza che quest'ultimo debba riscriverla
                    if(request.equals("register") || request.equals("login"))
                        reqFailed = true;
                }
            }
        }

        // PROCEDURE CHIUSURA CLIENT
        // rimozione callback RMI
        try {
            if (!connLost && server != null && username != null) {
                // mi devo rimuovere dal callback solo se sono loggato e la connessione al server non è stata persa
                server.unregisterForCallback(username);
            }

            if(callbackobj != null) {
                UnicastRemoteObject.unexportObject(callbackobj, false);
            }
        } catch (RemoteException ignored) { }
        // chiudo reader
        reader.close();
        // chiudo wallet update manager
        wum.stopExecution();
        System.out.println("> Client terminato.");
    }


    /**
     * Verifica che le preferenze specificate nel file di configurazione siano corrette facendo vari controlli. Se
     * anche una sola opzione è errata allora lancia un'eccezione.
     * @throws ConfigurationException se una opzione della configurazione è errata
     */
    private static void validateAndSavePreferences() throws ConfigurationException {
        // controllo che l'indirizzo del server sia valido
        try {
            String temp = config.getPreference("server_address");
            InetAddress ignored = InetAddress.getByName(temp);
            server_address = temp;
        } catch(UnknownHostException e) {
            throw new ConfigurationException("valore 'server_address' non valido (" + e.getLocalizedMessage() + ")");
        }

        // controllo la porta del server
        try {
            server_port = Integer.parseInt(config.getPreference("server_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'server_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(server_port <= 0 || server_port >= 65535) {
            throw new ConfigurationException("valore 'server_port' non valido (" + server_port + " non e' una porta valida)");
        }

        // controllo la porta dell'interfaccia RMI che permette al client di registrarsi
        try {
            server_rmi_port = Integer.parseInt(config.getPreference("server_rmi_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'server_rmi_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(server_rmi_port <= 0 || server_rmi_port >= 65535) {
            throw new ConfigurationException("valore 'server_rmi_port' non valido (" + server_rmi_port + " non e' una porta valida)");
        }

        // controllo che il nome del registry dell'interfaccia RMI sia presente nel config
        server_rmi_registry_name = config.getPreference("server_rmi_registry_name");
        if(server_rmi_registry_name == null) {
            throw new ConfigurationException("valore 'server_rmi_registry_name' non valido (non e' presente nel file di configurazione)");
        }

        // controllo la porta dell'interfaccia RMI che permette al server di essere contattato per un cambiamento nella lista follower
        try {
            client_rmi_callback_port = Integer.parseInt(config.getPreference("client_rmi_callback_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'client_rmi_callback_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(client_rmi_callback_port <= 0 || client_rmi_callback_port >= 65535) {
            throw new ConfigurationException("valore 'client_rmi_callback_port' non valido (" + client_rmi_callback_port + " non e' una porta valida)");
        }

        // controllo che il nome del registry dell'interfaccia RMI sia presente nel config
        server_rmi_callback_registry_name = config.getPreference("server_rmi_callback_registry_name");
        if(server_rmi_callback_registry_name == null) {
            throw new ConfigurationException("valore 'server_rmi_callback_registry_name' non valido (non e' presente nel file di configurazione)");
        }

        // controllo che l'indirizzo a cui il server invia le notifiche di aggiornamento dei wallet sia valido
        try {
            String temp = config.getPreference("multicast_server_address");
            InetAddress temp2 = InetAddress.getByName(temp);
            if(!temp2.isMulticastAddress()) {
                throw new ConfigurationException("valore 'multicast_server_address' non valido (" + multicast_server_address + " non e' un indirizzo multicast)");
            }
            multicast_server_address = temp;
        } catch(UnknownHostException e) {
            throw new ConfigurationException("valore 'multicast_server_address' non valido (" + e.getLocalizedMessage() + ")");
        }

        // controllo la porta multicast su cui il server invia le notifiche di aggiornamento dei wallet
        try {
            multicast_server_port = Integer.parseInt(config.getPreference("multicast_server_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'multicast_server_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(multicast_server_port <= 0 || multicast_server_port >= 65535) {
            throw new ConfigurationException("valore 'multicast_server_port' non valido (" + multicast_server_port + " non e' una porta valida)");
        }
    }
}