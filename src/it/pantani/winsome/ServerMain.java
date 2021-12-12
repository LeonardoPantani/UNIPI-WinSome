/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.entities.WinSomeUser;
import it.pantani.winsome.rmi.WinSomeCallback;
import it.pantani.winsome.rmi.WinSomeCallbackInterface;
import it.pantani.winsome.rmi.WinSomeService;
import it.pantani.winsome.rmi.WinSomeServiceInterface;
import it.pantani.winsome.utils.ConfigManager;
import it.pantani.winsome.utils.JsonManager;

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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    public static ConcurrentLinkedQueue<Socket> listaSocket = new ConcurrentLinkedQueue<>();
    public static ConcurrentLinkedQueue<WinSomeUser> listaUtenti = new ConcurrentLinkedQueue<>();

    public static ServerSocket serverSocket;
    public static int server_port;

    public static void main(String[] args) {
        System.out.println("> Server in fase di avvio...");
        System.out.println("> Lettura dati dal file di configurazione...");
        ConfigManager config = new ConfigManager();

        System.out.println("> Caricamento utenti da json...");
        JsonManager jsonmng = new JsonManager();
        jsonmng.load();
        System.out.println("> Caricamento json completato.");

        server_port = Integer.parseInt(config.getPreference("server_port"));
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        Thread in_handler = new Thread(new InputHandler());
        in_handler.start();

        // rmi register
        WinSomeService obj = new WinSomeService();
        try {
            WinSomeServiceInterface stub = (WinSomeServiceInterface) UnicastRemoteObject.exportObject(obj, 0);

            LocateRegistry.createRegistry(1099);
            Registry registry = LocateRegistry.getRegistry(1099);
            registry.bind("winsome-server", stub);

            System.out.println("> RMI pronto.");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }

        // rmi callback
        WinSomeCallback callbackobj = new WinSomeCallback();
        try {
            WinSomeCallbackInterface stub = (WinSomeCallbackInterface) UnicastRemoteObject.exportObject(callbackobj, 0);

            LocateRegistry.createRegistry(1100);
            Registry registry = LocateRegistry.getRegistry(1100);
            registry.bind("winsome-server-callback", stub);

            System.out.println("> RMI Callback pronto.");
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }


        System.out.println("> Server in ascolto sulla porta " + server_port + ". Scrivi 'help' per una lista di comandi.");

        int i = 1;
        while(true) {
            try {
                serverSocket = new ServerSocket(server_port);
                Socket a = serverSocket.accept();
                listaSocket.add(a);
                pool.submit(new ConnectionHandler(a, i));
                i++;
            } catch(SocketException e) {
                e.printStackTrace();
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
            jsonmng.save();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("> Server terminato.");
    }
}
