/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class ClientMain {
    public static String server_address = "localhost";
    public static int server_port = 6789;

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

            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while(true) {
                    System.out.print("> In attesa dell'input dell'utente: ");
                    String richiesta = lettore.nextLine();
                    if(Objects.equals(richiesta, "stopclient")) {
                        socket.close();
                        out.close();
                        in.close();
                        break loopesterno;
                    }
                    out.println(richiesta);
                    System.out.println("[Server]> " + in.readLine());
                }
            } catch (IOException e) {
                System.err.print("[!] Connessione al server perduta. Riprovare il collegamento? (S/N): ");
                if(!lettore.nextLine().equalsIgnoreCase("S")) {
                    break;
                }
            }
        }
        System.out.println("> Terminazione client.");
    }
}
