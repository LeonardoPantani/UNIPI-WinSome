/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome.client;

import it.pantani.winsome.other.Utils;
import it.pantani.winsome.rmi.NotifyEvent;
import it.pantani.winsome.rmi.NotifyEventInterface;
import it.pantani.winsome.rmi.WinSomeCallbackInterface;
import it.pantani.winsome.rmi.WinSomeServiceInterface;
import it.pantani.winsome.other.ConfigManager;

import javax.naming.ConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

// TODO commentare e ottimizzare
public class ClientMain {
    public static ConfigManager config;

    public static String server_address;
    public static int server_port;
    public static int server_rmi_port;
    public static int client_rmi_callback_port;
    public static String multicast_server_address;
    public static int multicast_server_port;

    public static ArrayList<String> listaFollower = new ArrayList<>();

    @SuppressWarnings("DuplicateBranchesInSwitch")
    public static void main(String[] args) {
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

        // validazione ok, le stampo
        System.out.println("> Indirizzo server: " + server_address);
        System.out.println("> Porta server: " + server_port);
        System.out.println("> Porta RMI server: " + server_rmi_port);
        System.out.println("> Porta RMI callback client: " + client_rmi_callback_port);
        System.out.println("> Indirizzo multicast notifiche: " + multicast_server_address);
        System.out.println("> Porta multicast notifiche: " + multicast_server_port);

        Socket socket;
        Scanner lettore = new Scanner(System.in);

        WinSomeCallbackInterface server = null;
        NotifyEventInterface callbackstub;
        NotifyEventInterface callbackobj = null;
        String username = null;
        String raw_request = "";
        boolean reqFailed = false;

        // Ascolto aggiornamenti wallet
        WalletUpdateManager wum;
        try {
            wum = new WalletUpdateManager(multicast_server_address, multicast_server_port);
        } catch(ConfigurationException e) {
            System.err.println("[!] Inizializzazione WalletUpdateManager fallita. Motivo: " + e.getLocalizedMessage());
            return;
        }
        Thread walletCheckerThread = new Thread(wum);
        walletCheckerThread.start();

        // Mi collego al server
        loopesterno:
        while(true) {
            try {
                socket = new Socket(server_address, server_port);
            } catch(IOException e) {
                System.err.print("[!] Impossibile connettersi al server. Riprovare a collegarsi? (S/N): ");
                if(lettore.nextLine().equalsIgnoreCase("S")) {
                    continue;
                } else {
                    break;
                }
            }

            System.out.println("> Connessione col server stabilita. Sto comunicando sulla porta " + socket.getLocalPort());

            // RMI (register)
            Registry registry;
            WinSomeServiceInterface stub;
            try {
                registry = LocateRegistry.getRegistry(server_address, server_rmi_port);
                stub = (WinSomeServiceInterface) registry.lookup("winsome-server");
            } catch (RemoteException | NotBoundException e) {
                System.err.println("[!] Errore RMI. Motivo: " + e.getLocalizedMessage());
                return;
            }

            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while(true) {
                    System.out.print("> ");
                    if(reqFailed) {
                        System.out.print(raw_request);
                        lettore.nextLine();
                        reqFailed = false;
                    } else {
                        raw_request = lettore.nextLine();
                        if(raw_request.equals("")) continue;
                    }
                    String[] temp = raw_request.split(" ");
                    String request = temp[0];
                    String[] arguments = new String[temp.length-1];
                    System.arraycopy(temp, 1, arguments, 0, temp.length - 1);

                    switch (request) {
                        case "stopclient": {
                            socket.close();
                            out.close();
                            in.close();
                            break loopesterno;
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
                            } else {
                                System.out.println("> Non è stato possibile registrare l'utente '" + arguments[0] + "'.");
                            }
                            break;
                        }
                        case "login": {
                            if (arguments.length != 2) {
                                System.err.println("[!] Comando errato, usa: login <username> <password>");
                                break;
                            }
                            out.println(raw_request);
                            String response = in.readLine();
                            if (response.equalsIgnoreCase(Utils.SOCIAL_LOGIN_SUCCESS)) {
                                username = arguments[0];
                                // registrazione callback
                                try {
                                    Registry callbackregistry = LocateRegistry.getRegistry(server_address, client_rmi_callback_port);
                                    server = (WinSomeCallbackInterface) callbackregistry.lookup("winsome-server-callback");
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

                                listaFollower = stub.initializeFollowerList(username, arguments[1]);
                                System.out.println("> Login dell'utente '" + username + "' effettuato.");
                            } else {
                                System.out.println("[Server]> " + response);
                            }
                            break;
                        }
                        case "logout": {
                            out.println(raw_request);
                            String a = in.readLine();
                            if (server != null && a.equals(Utils.SOCIAL_LOGOUT_SUCCESS)) {
                                System.out.println("> Logout dell'utente '" + username + "' effettuato");
                                server.unregisterForCallback(username);
                                UnicastRemoteObject.unexportObject(callbackobj, false);
                                username = null;
                            } else {
                                System.out.println("[Server]> " + a);
                            }
                            break;
                        }
                        case "listusers": {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        case "listfollowers": {
                            if(username == null) {
                                System.out.println("[!] Non hai effettuato il login");
                                return;
                            }

                            if (listaFollower.size() == 0) {
                                System.out.println("> Nessun utente ti segue.");
                                break;
                            }

                            System.out.println("> LISTA FOLLOWERS (" + listaFollower.size() + "):");
                            for (String u : listaFollower) {
                                System.out.println("* " + u);
                            }
                            System.out.print("\n");
                            break;
                        }
                        case "listfollowing": {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        case "follow": {
                            if (arguments.length != 1) {
                                System.err.println("[!] Comando errato, usa: follow <username>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                            break;
                        }
                        case "unfollow": {
                            if (arguments.length != 1) {
                                System.err.println("[!] Comando errato, usa: unfollow <username>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                            break;
                        }
                        case "post": {
                            if(arguments.length < 1) {
                                System.err.println("[!] Comando errato, usa: post <titolo>|<contenuto>");
                                break;
                            }
                            String req_body = raw_request.substring(5);
                            String[] text = req_body.split("\\|");
                            if(text.length != 2) {
                                System.err.println("[!] Comando errato, usa: post <titolo>|<contenuto>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                            break;
                        }
                        case "blog": {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        case "rewin": {
                            if(arguments.length != 1) {
                                System.err.println("[!] Comando errato, usa: rewin <id post>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                            break;
                        }
                        case "rate": {
                            if(arguments.length != 2) {
                                System.err.println("[!] Comando errato, usa: rate <id post> <+1/-1>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                            break;
                        }
                        case "showfeed": {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        case "showpost": {
                            if(arguments.length != 1) {
                                System.err.println("[!] Comando errato, usa: showpost <id post>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        case "comment": {
                            if(arguments.length < 2) {
                                System.err.println("[!] Comando errato, usa: comment <id post> <commento>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                            break;
                        }
                        case "delete": {
                            if(arguments.length != 1) {
                                System.err.println("[!] Comando errato, usa: delete <id post>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                            break;
                        }
                        case "wallet": {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        case "walletbtc": {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                            break;
                        }
                        default: {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                        }
                    }
                }
            } catch (IOException e) {
                System.err.print("[!] Connessione al server perduta. Riprovare a collegarsi? (S/N): ");
                if(!lettore.nextLine().equalsIgnoreCase("S")) {
                    break;
                } else {
                    reqFailed = true;
                }
            }
        }

        // rimozione callback RMI
        try {
            if (server != null && callbackobj != null) {
                if(username != null)
                    server.unregisterForCallback(username);

                UnicastRemoteObject.unexportObject(callbackobj, false);
            }
        } catch (RemoteException ignored) {}
        // chiudo lettore
        lettore.close();
        // chiudo wallet update manager
        wum.stopExecution();

        System.out.println("> Terminazione client.");
    }

    private static void validateAndSavePreferences() throws ConfigurationException {
        try {
            String temp = config.getPreference("server_address");
            InetAddress ignored = InetAddress.getByName(temp);
            server_address = temp;
        } catch(UnknownHostException e) {
            throw new ConfigurationException("valore 'server_address' non valido (" + e.getLocalizedMessage() + ")");
        }

        try {
            server_port = Integer.parseInt(config.getPreference("server_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'server_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(server_port <= 0 || server_port >= 65535) {
            throw new ConfigurationException("valore 'server_port' non valido (" + server_port + " non e' una porta valida)");
        }

        try {
            server_rmi_port = Integer.parseInt(config.getPreference("server_rmi_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'server_rmi_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(server_rmi_port <= 0 || server_rmi_port >= 65535) {
            throw new ConfigurationException("valore 'server_rmi_port' non valido (" + server_rmi_port + " non e' una porta valida)");
        }

        try {
            client_rmi_callback_port = Integer.parseInt(config.getPreference("client_rmi_callback_port"));
        } catch(NumberFormatException e) {
            throw new ConfigurationException("valore 'client_rmi_callback_port' non valido (" + e.getLocalizedMessage() + ")");
        }
        if(client_rmi_callback_port <= 0 || client_rmi_callback_port >= 65535) {
            throw new ConfigurationException("valore 'client_rmi_callback_port' non valido (" + client_rmi_callback_port + " non e' una porta valida)");
        }

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