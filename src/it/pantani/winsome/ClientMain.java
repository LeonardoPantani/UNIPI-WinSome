/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import it.pantani.winsome.rmi.NotifyEvent;
import it.pantani.winsome.rmi.NotifyEventInterface;
import it.pantani.winsome.rmi.WinSomeCallbackInterface;
import it.pantani.winsome.rmi.WinSomeServiceInterface;
import it.pantani.winsome.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ClientMain {
    public static String server_address = "localhost";
    public static int server_port = 6789;

    public static ArrayList<String> listaFollower = null;

    public static void main(String[] args) {
        if(args.length == 2) {
            try {
                server_address = args[0];
                server_port = Integer.parseInt(args[1]);

                if(server_port <= 0 || server_port >= 65535) {
                    System.err.println("[!] Numero porta non valido!");
                }
            } catch(NumberFormatException e) {
                System.err.println("[!] Numero porta non fornito.");
            }
        }
        System.out.println("> Indirizzo server: " + server_address);
        System.out.println("> Porta server: " + server_port);

        Socket socket;
        Scanner lettore = new Scanner(System.in);

        WinSomeCallbackInterface server = null;
        NotifyEventInterface callbackstub = null;
        NotifyEventInterface callbackobj = null;
        String username = null;
        String raw_request = "";
        boolean reqFailed = false;

        loopesterno:
        while(true) {
            try {
                socket = new Socket(server_address, server_port);
            } catch(IOException e) {
                System.err.print("[!] Impossibile connettersi al server. Riprovare il collegamento? (S/N): ");
                if(lettore.nextLine().equalsIgnoreCase("S")) {
                    continue;
                } else {
                    break;
                }
            }

            System.out.println("> Connessione col server stabilita. Sto comunicando sulla porta " + socket.getLocalPort());

            // RMI (register)
            Registry registry;
            WinSomeServiceInterface stub = null;
            try {
                registry = LocateRegistry.getRegistry(server_address, 1099);
                stub = (WinSomeServiceInterface) registry.lookup("winsome-server");
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
            if(stub == null) {
                System.err.println("> Errore stub!");
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
                    }
                    String[] temp = raw_request.split(" ");
                    String request = temp[0];
                    String[] arguments = new String[temp.length-1];
                    System.arraycopy(temp, 1, arguments, 0, temp.length - 1);

                    switch (request) {
                        case "stopclient" -> {
                            socket.close();
                            out.close();
                            in.close();
                            break loopesterno;
                        }
                        case "register" -> {
                            if (arguments.length < 2 || arguments.length > 7) {
                                System.err.println("[!] Comando errato, usa: register <username> <password> [lista di tag, max 5]");
                                break;
                            }
                            ArrayList<String> tags_list = new ArrayList<>(Arrays.asList(arguments).subList(2, arguments.length));

                            System.out.println("> Risposta server: " + stub.register(arguments[0], arguments[1], tags_list));
                        }
                        case "login" -> {
                            if (arguments.length != 2) {
                                System.err.println("[!] Comando errato, usa: login <username> <password>");
                                break;
                            }
                            out.println(raw_request);
                            String response = in.readLine();
                            if (response.equalsIgnoreCase("login ok")) {
                                username = arguments[0];
                                // registrazione callback
                                try {
                                    Registry callbackregistry = LocateRegistry.getRegistry(server_address, 1100);
                                    server = (WinSomeCallbackInterface) callbackregistry.lookup("winsome-server-callback");
                                    callbackobj = new NotifyEvent();
                                    callbackstub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackobj, 0);
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                                if (server == null) {
                                    System.err.println("> Errore stub callback!");
                                    break;
                                }

                                try {
                                    server.registerForCallback(username, callbackstub);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }

                                listaFollower = stub.initializeFollowerList(username, arguments[1]);
                            }
                            System.out.println("[Server]> " + response);
                        }
                        case "logout" -> {
                            out.println(raw_request);
                            String a = in.readLine();
                            System.out.println("[Server]> " + a);
                            if (server != null && a.equalsIgnoreCase("logout effettuato")) {
                                server.unregisterForCallback(username);
                                username = null;
                            }
                        }
                        case "listfollowers" -> {
                            if (listaFollower == null) {
                                System.out.println("> Nessun utente ti segue.");
                                break;
                            }

                            System.out.println("> LISTA FOLLOWERS (" + listaFollower.size() + "):");
                            for (String u : listaFollower) {
                                System.out.println(u);
                            }
                        }
                        case "listfollowing" -> {
                            out.println(raw_request);
                            System.out.println("[Server]> " + Utils.receive(in));
                        }
                        case "follow" -> {
                            if (arguments.length != 1) {
                                System.err.println("[!] Comando errato, usa: follow <username>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                        }
                        case "unfollow" -> {
                            if (arguments.length != 1) {
                                System.err.println("[!] Comando errato, usa: unfollow <username>");
                                break;
                            }
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                        }
                        default -> {
                            out.println(raw_request);
                            System.out.println("[Server]> " + in.readLine());
                        }
                    }
                }
            } catch (IOException e) {
                System.err.print("[!] Connessione al server perduta. Riprovare il collegamento? (S/N): ");
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
        } catch (ConnectException ignored) {} catch (RemoteException e) {
            e.printStackTrace();
        }

        System.out.println("> Terminazione client.");
    }
}
