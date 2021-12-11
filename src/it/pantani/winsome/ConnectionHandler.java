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

public class ConnectionHandler implements Runnable {
    private final Socket clientSocket;
    private final int chCode;

    private PrintWriter out = null;
    private BufferedReader in = null;

    public ConnectionHandler(Socket clientSocket, int chCode) {
        this.clientSocket = clientSocket;
        this.chCode = chCode;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        } catch(IOException e) {
            e.printStackTrace();
        }

        if(out == null || in == null) {
            System.err.println("[!] Errore durante instaurazione connessione.");
            return;
        } else {
            System.out.println("[CH #" + chCode + "]> Sto gestendo il socket: " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
        }

        String raw_request;
        while(true) {
            try {
                raw_request = in.readLine();
                if(raw_request != null) {
                    System.out.println("[" + clientSocket.getInetAddress() + ":" + clientSocket.getPort() + "]> " + raw_request);

                    String[] temp = raw_request.split(" ");
                    String request = temp[0];
                    String[] arguments = new String[temp.length-1];
                    for(int i = 1; i < temp.length; i++) {
                        arguments[i-1] = temp[i];
                    }

                    switch(request) {
                        case "help": {
                            help(arguments);
                            break;
                        }

                        default: {
                            help(arguments);
                        }
                    }
                } else {
                    break;
                }
            } catch (IOException e) {
                break;
            }
        }
        ServerMain.listaSocket.remove(clientSocket);
        System.out.println("> Collegamento col client terminato.");
    }

    private void help(String[] feature) {
        if(feature.length == 0) { // se non ci sono argomenti
            out.println("Non e' disponibile alcuna funzione di aiuto al momento (no args)");
        } else {
            out.println("Non e' disponibile alcuna funzione di aiuto al momento (con args)");
        }
    }
}
