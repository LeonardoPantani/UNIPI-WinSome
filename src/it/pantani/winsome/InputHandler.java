/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.entities.WinSomeUser;
import it.pantani.winsome.rmi.WinSomeCallback;
import it.pantani.winsome.rmi.WinSomeCallbackInterface;

import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

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

                case "listusers": {
                    listUsers();
                    break;
                }

                case "stopserver": {
                    stopServer();
                    break;
                }

                case "testfeature": {
                    if(arguments.length != 3) {
                        System.err.println("> Utilizzo comando errato: testfeature addfollower/removefollower <username> <change>");
                        break;
                    }
                    testfeature(arguments);
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

    private void listUsers() {
        int numUsers = ServerMain.listaUtenti.size();
        ConcurrentLinkedQueue<String> user_tags_list;

        System.out.println("> LISTA UTENTI REGISTRATI (" + numUsers + "):");
        if(numUsers != 0) {
            for(WinSomeUser u : ServerMain.listaUtenti) {
                System.out.print("[#" + u.getId() + "] " + u.getUsername());
                user_tags_list = u.getTags_list();
                if(user_tags_list.size() != 0) {
                    System.out.println("  " + user_tags_list);
                } else {
                    System.out.println("  (nessun tag specificato)");
                }
            }
        } else {
            System.out.println("(nessun utente registrato)");
        }
    }

    private void testfeature(String[] arguments) {
        switch(arguments[0]) {
            case "addfollower": {
                if(arguments.length != 3) {
                    System.err.println("[!] Argomento non valido: testfeature addfollower <username> <change>");
                    break;
                }
                try {
                    WinSomeCallback.notifyFollowerUpdate(arguments[1], "+" + arguments[2]);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }

            case "removefollower": {
                if(arguments.length != 3) {
                    System.err.println("[!] Argomento non valido: testfeature removefollower <username> <change>");
                    break;
                }
                try {
                    WinSomeCallback.notifyFollowerUpdate(arguments[1], "+" + arguments[2]);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            }

            default: {
                System.err.println("[!] Uso comando errato: testfeature addfollower/removefollower <username> <change>");
            }
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
