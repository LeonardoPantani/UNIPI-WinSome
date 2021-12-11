/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class InputHandler implements Runnable {
    private boolean close = false;
    private Scanner in;

    @Override
    public void run() {
        in = new Scanner(System.in);

        while(!close) {
            String raw_request = in.nextLine();
            String[] temp = raw_request.split(" ");

            String request = temp[0];
            String[] arguments = new String[temp.length-1];
            for(int i = 1; i < temp.length; i++) {
                arguments[i-1] = temp[i];
            }

            switch(request) {
                case "kickclient": {
                    if(arguments.length != 1) {
                        System.err.println("> Utilizzo comando errato: kickclient <porta_client>");
                        break;
                    }

                    int port;
                    try {
                        port = Integer.parseInt(arguments[0]);
                    } catch(NumberFormatException e) {
                        System.err.println("> Utilizzo comando errato: kickclient <porta_client>");
                        break;
                    }

                    kickClient(port);
                    break;
                }

                case "kickallclients": {
                    kickAllClients();
                    break;
                }

                case "listclients": {
                    listClients();
                    break;
                }

                case "stopserver": {
                    stopServer();
                    break;
                }

                case "help": {
                    help();
                    break;
                }
            }
        }
    }

    private void kickClient(int client_port) {
        boolean trovato = false;

        for(Socket x : ServerMain.listaSocket) {
            if(x.getPort() == client_port) {
                trovato = true;
                try {
                    x.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ServerMain.listaSocket.remove(x);
            }
        }

        if(trovato) {
            System.out.println("> Connessione chiusa!");
        } else {
            System.err.println("[!] Impossibile trovare un client con porta: " + client_port);
        }
    }


    private void kickAllClients() {
        for(Socket x : ServerMain.listaSocket) {
            try {
                x.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ServerMain.listaSocket.remove(x);
        }

        System.out.println("> Connessione a tutti client chiusa.");
    }

    private void listClients() {
        int numClients = ServerMain.listaSocket.size();

        System.out.println("> LISTA CLIENT CONNESSI (" + numClients + "):");
        if(numClients != 0) {
            for(Socket x : ServerMain.listaSocket) {
                System.out.println("> Client " + x.getInetAddress() + ":" + x.getPort());
            }
        } else {
            System.out.println("(nessun client connesso)");
        }
    }

    private void stopServer() {
        String check;

        System.out.print("> Sei sicuro di voler terminare il server? (S/N): ");
        check = in.nextLine();
        if(check.equalsIgnoreCase("S")) {
            System.out.println("> Server in arresto...");
            close = true;
            try {
                ServerMain.serverSocket.close();
            } catch (IOException ignored) { }
        }
    }

    private void help() {
        System.out.println("> LISTA COMANDI:");
        System.out.println("kickclient <porta> - Chiude forzatamente la connessione con un client");
        System.out.println("kickallclients     - Chiude forzatamente la connessione a tutti i client");
        System.out.println("listclients        - Mostra la lista di clients connessi");
        System.out.println("stopserver         - Termina il server");
        System.out.println("help               - Mostra questa schermata");
    }
}
