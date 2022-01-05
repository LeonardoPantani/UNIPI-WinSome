/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.server;

import it.pantani.winsome.server.entities.WinSomeSession;
import it.pantani.winsome.server.rmi.WinSomeCallback;
import it.pantani.winsome.shared.rmi.WinSomeCallbackInterface;
import it.pantani.winsome.server.rmi.WinSomeService;
import it.pantani.winsome.shared.rmi.WinSomeServiceInterface;
import it.pantani.winsome.shared.ConfigManager;
import it.pantani.winsome.server.utils.JsonManager;

import javax.naming.ConfigurationException;
import java.io.IOException;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;

/**
 * Classe del server del social WinSome. Da sola questa classe inizializza solo alcune strutture dati utilizzate dalla
 * classe ConnectionHandler. Oltre a quello, si occupa di leggere il file di configurazione, controllare alcune preferenze
 * (solo quelle che usa questa classe) e inizializzare tutte le altre classi del server tra cui: SocialManager, RewardsManager,
 * JSONManager, InputHandler (per l'input amministratore del server). Avvia anche le interfacce RMI e inizializza il ThreadPoolExecutor,
 * che contiene tutti i connectionhandler; ogni connectionhandler gestisce una singola connessione. Il server si arresta
 * completamente in due modi: CTRL+C (arresto forzato, no salvataggio dati di persistenza) oppure con il comando "stopserver"
 * e poi rispondendo "S" al prompt (in questo modo i dati persistenti sono salvati).
 */
public class ServerMain {
    public static ConfigManager config;
    public static JsonManager jsonmngr;
    public static SocialManager social;
    public static RewardsManager rewards;
    public static PeriodicSaveManager psmngr;

    // utilizzati dal connectionhandler
    // mantengono la lista di socket connessi e la lista delle sessioni di tutti gli utenti collegati
    public static final ConcurrentLinkedQueue<Socket> socketsList = new ConcurrentLinkedQueue<>();
    public static final ConcurrentHashMap<String, WinSomeSession> sessionsList = new ConcurrentHashMap<>();

    public static ServerSocket serverSocket;

    private static int server_port;
    private static int rmi_server_port;
    private static String rmi_server_registry_name;
    private static int rmi_callback_client_port;
    private static String rmi_callback_client_registry_name;
    private static int max_timeout_pool_shutdown;

    public static void main(String[] args) {
        // leggo il file di configurazione (o lo creo se non esiste)
        System.out.println("> Lettura dati dal file di configurazione...");
        try {
            config = new ConfigManager(true);
        } catch(IOException e) {
            System.err.println("[!] Lettura fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }

        // tutte le preferenze relative al main vengono controllate direttamente qui
        try {
            validateAndSavePreferences();
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }

        // avvio il social (effettuerà i controlli sulle preferenze che usa)
        System.out.println("> Inizializzazione social...");
        try {
            social = new SocialManager(config);
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }

        // avvio il rewardsmanager (effettuerà i controlli sulle preferenze che usa)
        System.out.println("> Inizializzazione rewards...");
        try {
            rewards = new RewardsManager(config, social);
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }
        Thread r_manager = new Thread(rewards);
        r_manager.start();

        // carico i dati di persistenza
        System.out.println("> Caricamento dati da json...");
        jsonmngr = new JsonManager();
        jsonmngr.loadAll(social);
        System.out.println("> Caricamento json completato.");

        // preparo il threadpoolexecutor (di tipo cache che genera i thread quando serve e li riusa quando possibile)
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        // gestisce tutta la parte di comandi inviati dall'amministratore del server per vederne lo stato
        InputHandler ih;
        try {
            ih = new InputHandler(config, social);
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }
        Thread in_handler = new Thread(ih);

        // gestisce tutta la parte di salvataggio dati persistente periodica
        System.out.println("> Inizializzazione periodic save manager...");
        try {
            psmngr = new PeriodicSaveManager(config, jsonmngr, social, rewards);
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }
        Thread p_save_manager = new Thread(psmngr);

        // rmi register
        WinSomeService obj = new WinSomeService();
        try {
            WinSomeServiceInterface stub = (WinSomeServiceInterface) UnicastRemoteObject.exportObject(obj, 0);

            LocateRegistry.createRegistry(rmi_server_port);
            Registry registry = LocateRegistry.getRegistry(rmi_server_port);
            registry.bind(rmi_server_registry_name, stub);

            System.out.println("> RMI pronto sulla porta " + rmi_server_port + ". Nome registry: " + rmi_server_registry_name);
        } catch(RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
            return;
        }

        // rmi callback
        WinSomeCallback callbackobj = new WinSomeCallback();
        try {
            WinSomeCallbackInterface stub = (WinSomeCallbackInterface) UnicastRemoteObject.exportObject(callbackobj, 0);

            LocateRegistry.createRegistry(rmi_callback_client_port);
            Registry registry = LocateRegistry.getRegistry(rmi_callback_client_port);
            registry.bind(rmi_callback_client_registry_name, stub);

            System.out.println("> RMI callback pronto sulla porta " + rmi_callback_client_port + ". Nome registry: " + rmi_callback_client_registry_name);
        } catch(RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
            return;
        }

        // apro il socket del server
        try {
            serverSocket = new ServerSocket(server_port);
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }

        // avvio il salvataggio periodico
        p_save_manager.start();

        System.out.println("> Server in ascolto sulla porta " + server_port + ". Scrivi 'help' per una lista di comandi.");
        in_handler.start(); // avvio ora il thread che attende l'input dell'amministratore del server

        // contatore dei connection handler, solo a scopo informativo e di debug
        int i = 1;
        // il main non fa altro che accettare le connessioni dei client e assegnarle ad un connection handler del pool
        while(true) {
            try {
                Socket a = serverSocket.accept();
                socketsList.add(a);
                pool.submit(new ConnectionHandler(a, i, config, social));
                i++;
            } catch(SocketException e) { // si verifica solo se il socket viene chiuso forzatamente da un altro thread o entità esterna (nel nostro caso il comando "stopserver" nell'InputHandler)
                break;
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        // ---------------- CHIUSURA SERVER
        // se esco dal while mi preparo ad arrestare tutti i thread, collegamenti e chiudere le risorse ancora aperte

        // chiusura socket dei client
        for (Socket c : socketsList) {
            try {
                c.close();
            } catch(IOException ignored) { }
        }
        System.out.println("> Handler connessioni chiusi.");

        // chiusura socket del server
        try {
            serverSocket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

        // chiusura input handler
        try {
            in_handler.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        // chiusura del rewards manager
        rewards.stopExecution();
        r_manager.interrupt();

        // chiusura pool
        pool.shutdown();
        try {
            if (!pool.awaitTermination(max_timeout_pool_shutdown, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch(InterruptedException e) {
            pool.shutdownNow();
            System.out.println("> Chiudo forzatamento il pool a causa di un'interruzione.");
        }
        System.out.println("> Pool chiuso.");

        // chiusura RMI
        try {
            UnicastRemoteObject.unexportObject(obj, false);
            UnicastRemoteObject.unexportObject(callbackobj, false);
        } catch(NoSuchObjectException e) {
            e.printStackTrace();
        }
        System.out.println("> RMI chiuso.");

        // salvataggio dati finale
        psmngr.stopExecution();
        p_save_manager.interrupt();

        System.out.println("> Server terminato.");
    }

    /**
     * Verifica che le preferenze specificate nel file di configurazione siano corrette facendo vari controlli. Se
     * anche una sola opzione è errata allora lancia un'eccezione.
     *
     * @throws ConfigurationException se una opzione della configurazione è errata
     */
    private static void validateAndSavePreferences() throws ConfigurationException {
        // controllo la porta del server
        try {
            server_port = Integer.parseInt(config.getPreference("server_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'server_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if (server_port <= 0 || server_port >= 65535) {
            throw new ConfigurationException("valore 'server_port' non valido (" + server_port + " non e' una porta valida)");
        }

        // controllo la porta dell'interfaccia RMI del server
        try {
            rmi_server_port = Integer.parseInt(config.getPreference("rmi_server_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'rmi_server_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if (rmi_server_port <= 0 || rmi_server_port >= 65535) {
            throw new ConfigurationException("valore 'rmi_server_port' non valido (" + rmi_server_port + " non e' una porta valida)");
        }

        // controllo che il nome del registro dell'interfaccia RMI del server sia presente
        rmi_server_registry_name = config.getPreference("rmi_server_registry_name");
        if (rmi_server_registry_name == null) {
            throw new ConfigurationException("valore 'rmi_server_registry_name' non valido (non e' presente nel file di configurazione)");
        }

        // controllo la porta dell'interfaccia callback RMI del server
        try {
            rmi_callback_client_port = Integer.parseInt(config.getPreference("rmi_callback_client_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'rmi_callback_client_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if (rmi_callback_client_port <= 0 || rmi_callback_client_port >= 65535) {
            throw new ConfigurationException("valore 'rmi_callback_client_port' non valido (" + rmi_callback_client_port + " non e' una porta valida)");
        }

        // controllo che il nome del registro dell'interfaccia RMI del server sia presente
        rmi_callback_client_registry_name = config.getPreference("rmi_callback_client_registry_name");
        if (rmi_callback_client_registry_name == null) {
            throw new ConfigurationException("valore 'rmi_callback_client_registry_name' non valido (non e' presente nel file di configurazione)");
        }

        // controllo che il numero di millisecondi da attendere prima della chiusura della pool non sia invalido
        try {
            max_timeout_pool_shutdown = Integer.parseInt(config.getPreference("max_timeout_pool_shutdown"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'max_timeout_pool_shutdown' non valido (" + e.getLocalizedMessage() + ")");
        }
        if (max_timeout_pool_shutdown <= 0) {
            throw new ConfigurationException("valore 'max_timeout_pool_shutdown' non valido (" + max_timeout_pool_shutdown + " dovrebbe essere maggiore di 0)");
        }
    }
}