/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.utils.ConfigManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    public static ConcurrentLinkedQueue<Socket> listaSocket = new ConcurrentLinkedQueue<>();
    public static ServerSocket serverSocket;
    public static int server_port;

    public static void main(String[] args) {
        System.out.println("> Server in fase di avvio...");
        System.out.println("> Lettura dati dal file di configurazione...");
        ConfigManager config = new ConfigManager();

        server_port = Integer.parseInt(config.getPreference("server_port"));
        ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        Thread in_handler = new Thread(new InputHandler());
        in_handler.start();

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

        System.out.println("> Server terminato.");
    }
}
