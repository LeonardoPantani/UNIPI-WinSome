/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.entities.WinSomeSession;
import it.pantani.winsome.entities.WinSomeUser;
import it.pantani.winsome.rmi.WinSomeCallback;
import it.pantani.winsome.utils.ConfigManager;

import java.io.IOException;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;

import static it.pantani.winsome.utils.Utils.getFormattedDate;

public class InputHandler implements Runnable {
    private boolean close = false;
    private Scanner in;

    @Override
    public void run() {
        in = new Scanner(System.in);

        while(!close) {
            String raw_request = in.nextLine();
            if(raw_request.equals("")) continue;
            String[] temp = raw_request.split(" ");

            String request = temp[0];
            String[] arguments = new String[temp.length-1];
            System.arraycopy(temp, 1, arguments, 0, temp.length - 1);

            switch (request) {
                case "kickclient": {
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
                case "listfollowers": {
                    if (arguments.length != 1) {
                        System.err.println("[!] Utilizzo comando errato: " + request + " <username>");
                        break;
                    }
                    listFollowers(arguments[0]);
                    break;
                }
                case "listfollowing": {
                    if (arguments.length != 1) {
                        System.err.println("[!] Utilizzo comando errato: " + request + " <username>");
                        break;
                    }
                    listFollowing(arguments[0]);
                    break;
                }
                case "stopserver": {
                    stopServer();
                    break;
                }
                case "test": {
                    test(arguments);
                    break;
                }
                case "help": {
                    help();
                    break;
                }
                default: {
                    unknownCommand();
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
        if(numClients == 0) {
            System.out.println("> Nessun client connesso");
            return;
        }

        System.out.println("> LISTA CLIENT CONNESSI (" + numClients + "):");
        for(Socket x : ServerMain.listaSocket) {
            System.out.print("> Client " + x.getInetAddress() + ":" + x.getPort());
            WinSomeSession wss = ConnectionHandler.getSessionBySocket(x);
            if(wss != null) {
                System.out.println(" (utente connesso: " + wss.getUsername() + " | data login: " + getFormattedDate(wss.getTimestamp()) + ")");
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
            if(user_tags_list.size() != 0) {
                System.out.println("  " + user_tags_list);
            } else {
                System.out.println("  (nessun tag specificato)");
            }
        }
    }

    private void listFollowers(String utente) {
        SocialManager s = ServerMain.social;
        if(!s.findUser(utente)) {
            System.out.println("[!] Utente '" + utente + "' non valido.");
            return;
        }
        ArrayList<String> list = s.getFollowers(utente);
        if(list == null) {
            System.out.println("> '" + utente + "' non e' seguito da alcun utente.");
            return;
        }

        System.out.println("> UTENTI CHE SEGUONO " + utente + " (" + list.size() + "):");
        for(String u : list) {
            System.out.println("* " + u);
        }
    }

    private void listFollowing(String utente) {
        SocialManager s = ServerMain.social;
        if(!s.findUser(utente)) {
            System.out.println("[!] Utente '" + utente + "' non valido.");
            return;
        }
        ArrayList<String> list = s.getFollowing(utente);
        if(list == null) {
            System.out.println("> '" + utente + "' non segue alcun utente.");
            return;
        }

        System.out.println("> UTENTI CHE " + utente + " SEGUE (" + list.size() + "):");
        for(String u : list) {
            System.out.println("* " + u);
        }
    }

    private void test(String[] arguments) {
        SocialManager s = ServerMain.social;
        ConfigManager c = ServerMain.config;

        if(arguments.length < 1) {
            System.err.println("[!] Uso comando errato. Vedi codice per dettagli.");
            return;
        }

        switch (arguments[0]) {
            case "addfollower": {
                if (arguments.length != 3) {
                    System.err.println("[!] Argomento non valido: test " + arguments[0] + " <username> <nuovo follower>");
                    break;
                }
                try {
                    WinSomeCallback.notifyFollowerUpdate(arguments[1], "+" + arguments[2]);
                    System.out.println("> Aggiunto a '" + arguments[1] + "' il follower '" + arguments[2]+ "'");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            case "removefollower": {
                if (arguments.length != 3) {
                    System.err.println("[!] Argomento non valido: test " + arguments[0] + " <username> <follower da rimuovere>");
                    break;
                }
                try {
                    WinSomeCallback.notifyFollowerUpdate(arguments[1], "-" + arguments[2]);
                    System.out.println("> Rimosso a '" + arguments[1] + "' il follower '" + arguments[2]+ "'");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            case "addfollowing": {
                if (arguments.length != 3) {
                    System.err.println("[!] Argomento non valido: test " + arguments[0] + " <username> <nuovo seguito>");
                    break;
                }
                s.addFollower(arguments[2], arguments[1]);
                s.addFollowing(arguments[1], arguments[2]);
                System.out.println("> Ora l'utente '" + arguments[1] + "' segue '" + arguments[2]+ "'");
            }

            case "removefollowing": {
                if (arguments.length != 3) {
                    System.err.println("[!] Argomento non valido: test " + arguments[0] + " <username> <seguito da rimuovere>");
                    break;
                }
                s.removeFollower(arguments[2], arguments[1]);
                s.removeFollowing(arguments[1], arguments[2]);
                System.out.println("> L'utente '" + arguments[1] + "' non segue piu' '" + arguments[2]+ "'");
            }

            case "changebal": {
                if (arguments.length != 3) {
                    System.err.println("[!] Argomento non valido: test " + arguments[0] + " <username> <cambiamento>");
                    break;
                }
                double change;
                try {
                    change = Float.parseFloat(arguments[2]);
                } catch(NumberFormatException e) {
                    System.err.println("[!] Argomento non valido: test " + arguments[0] + " <username> <cambiamento>");
                    break;
                }
                if(s.getUser(arguments[1]) == null) {
                    System.err.println("[!] L'utente '" + arguments[1] + "' non esiste");
                    break;
                }
                String reason = c.getPreference("default_reason_transaction");
                if(reason == null) reason = "SYSTEM";
                double newBalance = s.getWalletByUsername(arguments[1]).changeBalance(change, reason);
                System.out.println("> L'utente '" + arguments[1] + "' ha ora un bilancio di " + s.getFormattedCurrency(newBalance));
            }
            default: {
                System.err.println("[!] Uso comando errato. Vedi codice per dettagli.");
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
        System.out.println("kickclient <porta>     - Chiude forzatamente la connessione con un client");
        System.out.println("kickallclients         - Chiude forzatamente la connessione a tutti i client");
        System.out.println("listclients            - Mostra la lista di clients connessi");
        System.out.println("listusers              - Mostra la lista degli utenti registrati");
        System.out.println("listfollowers <utente> - Mostra gli utenti che seguono <utente>");
        System.out.println("listfollowing <utente> - Mostra gli utenti seguiti da <utente>");
        System.out.println("stopserver             - Termina il server");
        System.out.println("test                   - Testa delle funzionalita' (debug)");
        System.out.println("help                   - Mostra questa schermata");
    }

    private void unknownCommand() {
        System.out.println("> Comando sconosciuto, scrivi 'help' per una lista di comandi.");
    }
}
