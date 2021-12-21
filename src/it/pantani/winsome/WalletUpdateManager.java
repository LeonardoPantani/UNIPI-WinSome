/*
 * Copyright (c) 2021/2022
 * Leonardo Pantani - 598896
 * University of Pisa - Department of Computer Science
 */

package it.pantani.winsome;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

public class WalletUpdateManager implements Runnable {
    private InetAddress multicast_address;
    private int multicast_port;

    private volatile boolean stop = false;

    public WalletUpdateManager(String multicast_address, int multicast_port) {
        try {
            this.multicast_address = InetAddress.getByName(multicast_address);
        } catch(UnknownHostException e) {
            System.err.println("[!] Errore, parametro multicast_address non valido, motivo: " + e.getLocalizedMessage());
            return;
        }
        this.multicast_port = multicast_port;
    }

    @Override
    public void run() {
        try {
            MulticastSocket socketMulticast = new MulticastSocket(multicast_port);
            socketMulticast.joinGroup(multicast_address);

            byte[] buffer = new byte[1000];
            DatagramPacket pacchetto = new DatagramPacket(buffer, buffer.length);

            // in ascolto
            while(!stop) {
                socketMulticast.receive(pacchetto); // TODO invio periodico notifiche delle ricompense
                //System.out.println("[WUM]> Dato ricevuto dal server: " + new String(pacchetto.getData(), 0, pacchetto.getLength()));
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void stopExecution() {
        stop = true;
    }
}
