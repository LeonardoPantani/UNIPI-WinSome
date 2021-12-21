/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.entities.WinSomeSession;
import it.pantani.winsome.rmi.WinSomeCallback;
import it.pantani.winsome.rmi.WinSomeCallbackInterface;
import it.pantani.winsome.rmi.WinSomeService;
import it.pantani.winsome.rmi.WinSomeServiceInterface;
import it.pantani.winsome.utils.ConfigManager;
import it.pantani.winsome.utils.JsonManager;
import it.pantani.winsome.utils.Utils;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;

public class ServerMain {
    public static ConfigManager config;
    public static SocialManager social;
    public static final ConcurrentLinkedQueue<Socket> listaSocket = new ConcurrentLinkedQueue<>();
    public static final ConcurrentHashMap<String, WinSomeSession> listaSessioni = new ConcurrentHashMap<>();

    public static ServerSocket serverSocket;
    public static int server_port;

    public static void main(String[] args) {
        System.out.println("> Server in fase di avvio...");
        System.out.println("> Lettura dati dal file di configurazione...");
        try {
            config = new ConfigManager();
        } catch(IOException e) {
            System.err.println("[!] Lettura fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }

        System.out.println("> Inizializzazione social...");
        try {
            social = new SocialManager(config);
        } catch(IOException e) {
            System.err.println("[!] Inizializzazione fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }
        System.out.println("> Social pronto.");

        System.out.println("> Caricamento dati da json...");
        JsonManager jsonmng = new JsonManager();
        jsonmng.loadAll(social);
        System.out.println("> Caricamento json completato.");

        server_port = Integer.parseInt(config.getPreference("server_port"));
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        // rmi register
        WinSomeService obj = new WinSomeService();
        try {
            WinSomeServiceInterface stub = (WinSomeServiceInterface) UnicastRemoteObject.exportObject(obj, 0);

            int rmi_port;
            try {
                rmi_port = Integer.parseInt(config.getPreference("rmi_server_port"));
            } catch(NumberFormatException e) {
                System.err.println("[!] Porta server RMI non valida.");
                return;
            }

            LocateRegistry.createRegistry(rmi_port);
            Registry registry = LocateRegistry.getRegistry(rmi_port);
            registry.bind("winsome-server", stub);

            System.out.println("> RMI pronto sulla porta " + rmi_port + ".");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }

        // rmi callback
        WinSomeCallback callbackobj = new WinSomeCallback();
        try {
            WinSomeCallbackInterface stub = (WinSomeCallbackInterface) UnicastRemoteObject.exportObject(callbackobj, 0);

            int rmi_callback_port;
            try {
                rmi_callback_port = Integer.parseInt(config.getPreference("rmi_callback_client_port"));
            } catch(NumberFormatException e) {
                System.err.println("[!] Porta RMI callback client non valida.");
                return;
            }

            LocateRegistry.createRegistry(rmi_callback_port);
            Registry registry = LocateRegistry.getRegistry(rmi_callback_port);
            registry.bind("winsome-server-callback", stub);

            System.out.println("> RMI Callback pronto.");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }

        RewardsManager rm = new RewardsManager(config);
        Thread walletUpdaterThread = new Thread(rm);
        walletUpdaterThread.start();


        System.out.println("> Server in ascolto sulla porta " + server_port + ". Scrivi 'help' per una lista di comandi.");

        Thread in_handler = new Thread(new InputHandler());
        in_handler.start();

        int i = 1;
        try {
            serverSocket = new ServerSocket(server_port);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true) {
            try {
                Socket a = serverSocket.accept();
                listaSocket.add(a);
                pool.submit(new ConnectionHandler(a, i));
                i++;
            } catch(SocketException e) {
                break;
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        }

        // chiusura server
        try {
            in_handler.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("> Handler input chiuso.");

        // chiusura socket dei client
        for(Socket c : listaSocket) {
            try {
                c.close();
            } catch (IOException ignored) { }
        }
        System.out.println("> Handler connessioni chiusi.");

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // chiusura pool
        pool.shutdown();
        try {
            if (!pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            System.out.println("> Chiudo forzatamento il pool a causa di un'interruzione.");
        }
        System.out.println("> Pool chiuso.");

        // chiusura RMI
        try {
            UnicastRemoteObject.unexportObject(obj, false);
            UnicastRemoteObject.unexportObject(callbackobj, false);
        } catch (NoSuchObjectException e) {
            e.printStackTrace();
        }
        System.out.println("> RMI chiuso.");

        // salvataggio dati persistente
        try {
            jsonmng.saveAll(social);
            social.close(config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // rewards manager stop
        rm.stopExecution();

        System.out.println("> Server terminato.");
    }
}
